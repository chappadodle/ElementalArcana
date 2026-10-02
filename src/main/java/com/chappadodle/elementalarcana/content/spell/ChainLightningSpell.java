package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Lightning's first spell: a bolt leaps from the caster's hand to the creature under the crosshair
 * (up to 20 blocks), then on to up to 3 more creatures within 6 blocks of the last, each jump a
 * fifth weaker. A wet creature conducts it: it takes half again as much, and the bolt can jump
 * twice more from it (never more than 6 jumps in all). Drawn as a jagged line of electric-yellow
 * pixels with sparks at its kinks. No target: the cast fails and costs nothing. Lightning wisps
 * cast it too.
 */
public class ChainLightningSpell extends Spell {
    private static final double RANGE = 20;
    private static final double AIM_LEEWAY = 0.6;
    private static final double JUMP_RANGE = 6;
    private static final int JUMPS = 3;
    private static final int MAX_JUMPS = 6;
    private static final float DAMAGE = 5f;
    private static final float FALLOFF = 0.8f;
    private static final float CONDUCTED = 1.5f;
    /** The caster's own bolt is drawn from this far past the hand, so nothing balloons in front of their eyes. */
    private static final double FIRST_PERSON_SKIP = 1.0;
    private static final DustParticleOptions ARC = new DustParticleOptions(new Vector3f(1f, 0.92f, 0.45f), 0.7f);
    private static final DustParticleOptions ARC_CORE = new DustParticleOptions(new Vector3f(1f, 1f, 0.9f), 0.45f);

    public ChainLightningSpell() {
        super(ModSchools.LIGHTNING, 28, 70);
    }

    @Override
    public CastResult cast(CastContext context) {
        LivingEntity first = SpellTargets.underCrosshair(context.caster(), RANGE, AIM_LEEWAY,
                // The first strike may be aimed at anyone (like a projectile); the jumps spare players.
                target -> SpellTargets.canAffect(context.caster(), target) || target instanceof Player);
        if (first == null) {
            return CastResult.fail(Component.translatable("message.elementalarcana.chain_lightning.no_target"));
        }
        chain(context.level(), context.caster(), hand(context.caster()), FIRST_PERSON_SKIP, first, context.power(),
                target -> SpellTargets.canAffect(context.caster(), target));
        return CastResult.SUCCESS;
    }

    /** Where the bolt leaves a caster: a little in front of the right hand. */
    public static Vec3 hand(LivingEntity caster) {
        float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return caster.getEyePosition().add(caster.getLookAngle().scale(0.6)).add(right.scale(0.3)).add(0, -0.3, 0);
    }

    /**
     * The chain itself: from {@code from} to {@code first} (drawn from {@code skip} blocks along),
     * then from creature to creature that {@code jumpsTo} allows (never back to one it already
     * struck, never the caster).
     */
    public static void chain(ServerLevel level, LivingEntity caster, Vec3 from, double skip, LivingEntity first, float power,
                             Predicate<LivingEntity> jumpsTo) {
        Set<Integer> struck = new HashSet<>();
        struck.add(caster.getId());
        LivingEntity target = first;
        float damage = DAMAGE * power;
        int jumpsLeft = JUMPS;
        int jumps = 0;
        Vec3 start = from;
        while (target != null) {
            Vec3 to = target.getBoundingBox().getCenter();
            bolt(level, start, to, jumps == 0 ? skip : 0);
            boolean wet = ElementalReactions.auraOf(target) == ElementalReactions.Aura.HYDRO;
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.LIGHTNING, caster, caster), damage * (wet ? CONDUCTED : 1f));
            struck.add(target.getId());
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 12, 0.3, 0.4, 0.3, 0.2);
            if (wet) {
                jumpsLeft += 2;
                level.sendParticles(ParticleTypes.SPLASH, to.x, to.y, to.z, 10, 0.3, 0.3, 0.3, 0.1);
            }
            level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.6f + level.random.nextFloat() * 0.3f);
            if (jumpsLeft-- <= 0 || ++jumps > MAX_JUMPS) {
                break;
            }
            damage *= FALLOFF;
            start = to;
            target = nextTarget(level, to, struck, jumpsTo);
        }
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.35f, 1.8f);
    }

    /** The nearest creature within reach of the last strike that hasn't been struck yet. */
    @Nullable
    private static LivingEntity nextTarget(ServerLevel level, Vec3 at, Set<Integer> struck, Predicate<LivingEntity> jumpsTo) {
        LivingEntity best = null;
        double bestDistance = JUMP_RANGE * JUMP_RANGE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(JUMP_RANGE),
                e -> e.isAlive() && !e.isSpectator() && !struck.contains(e.getId()) && jumpsTo.test(e))) {
            double distance = candidate.getBoundingBox().getCenter().distanceToSqr(at);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    /**
     * The bolt: a jagged line of electric-yellow pixels from {@code a} to {@code b} (its kinks a
     * little off the straight line, each throwing sparks), drawn from {@code skip} blocks past a.
     */
    public static void bolt(ServerLevel level, Vec3 a, Vec3 b, double skip) {
        Vec3 line = b.subtract(a);
        int kinks = Math.max(3, (int) (line.length() * 1.2));
        RandomSource random = level.random;
        Vec3 previous = a;
        for (int i = 1; i <= kinks; i++) {
            Vec3 point = a.add(line.scale(i / (double) kinks));
            if (i < kinks) {
                point = point.add((random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.6, (random.nextDouble() - 0.5) * 0.6);
            }
            int steps = Math.max(2, (int) Math.ceil(previous.distanceTo(point) / 0.22));
            for (int s = 0; s < steps; s++) {
                Vec3 p = previous.lerp(point, s / (double) steps);
                if (p.distanceTo(a) >= skip) {
                    level.sendParticles(s % 2 == 0 ? ARC : ARC_CORE, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            if (point.distanceTo(a) >= skip) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 2, 0.05, 0.05, 0.05, 0.08);
            }
            previous = point;
        }
    }
}
