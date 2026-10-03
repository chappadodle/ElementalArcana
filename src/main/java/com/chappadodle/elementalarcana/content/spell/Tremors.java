package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.TremorRules;
import com.chappadodle.elementalarcana.content.TremorOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tremor's shockwaves (see TremorSpell and the Earth spec): a front running from under its caster
 * along the ground, a block a tick, on a path worked out when it starts (TremorRules: it climbs
 * steps, follows drops, breaks against walls). Whatever it may hurt that stands where the front
 * passes is hit once: a little Earth damage, thrown up and slowed. Out of the way, or in mid-jump,
 * it misses. The hits are worked out here, on the server, never saved; clients are sent one
 * particle when it starts and draw the front from the same path (TremorOptions, TremorEmitter).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Tremors {
    /** How far a caster's feet may be above the ground for the shockwave to start under them. */
    private static final int MAX_STAND = 2;
    private static final float DAMAGE = 3f;
    private static final int CRYSTAL_TICKS = 200;
    private static final int SLOW_TICKS = 60;
    private static final double LIFT = 0.6;
    private static final double SEEN = 64;

    private static final class Quake {
        final ServerLevel level;
        final LivingEntity caster;
        final float power;
        final Predicate<LivingEntity> affects;
        final Vec3 origin;
        final Vec3 dir;
        final int[] heights;
        final Set<Integer> hit = new HashSet<>();
        int step;

        Quake(ServerLevel level, LivingEntity caster, float power, Predicate<LivingEntity> affects, Vec3 origin, Vec3 dir, int[] heights) {
            this.level = level;
            this.caster = caster;
            this.power = power;
            this.affects = affects;
            this.origin = origin;
            this.dir = dir;
            this.heights = heights;
        }
    }

    private static final List<Quake> ACTIVE = new ArrayList<>();

    private Tremors() {
    }

    /**
     * Sends a shockwave from under {@code caster} along {@code direction} (made horizontal), hitting
     * what {@code affects} allows (never the caster). False if there's no ground under them to run on.
     */
    public static boolean start(LivingEntity caster, Vec3 direction, float power, Predicate<LivingEntity> affects) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 dir = new Vec3(direction.x, 0, direction.z);
        if (dir.lengthSqr() < 1.0e-4) {
            // Looking straight down: the way they're turned.
            dir = Vec3.directionFromRotation(0, caster.getYRot());
        }
        dir = dir.normalize();
        Vec3 at = caster.position();
        Integer foot = footUnder(level, BlockPos.containing(at));
        if (foot == null) {
            return false;
        }
        int[] heights = TremorRules.path((x, y, z) -> solid(level, new BlockPos(x, y, z)), at.x, foot, at.z, dir.x, dir.z);
        if (!TremorRules.hasRoom(heights)) {
            return false;
        }
        Vec3 origin = new Vec3(at.x, foot, at.z);
        ACTIVE.add(new Quake(level, caster, power, affects, origin, dir, heights));
        TremorOptions drawn = TremorOptions.of(dir, heights);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(origin) < SEEN * SEEN) {
                level.sendParticles(player, drawn, true, origin.x, origin.y, origin.z, 1, 0, 0, 0, 0);
            }
        }
        return true;
    }

    /** The foot height under {@code pos}: the first open block above ground, at most a couple below. */
    @Nullable
    private static Integer footUnder(ServerLevel level, BlockPos pos) {
        if (solid(level, pos)) {
            return solid(level, pos.above()) ? null : pos.getY() + 1;
        }
        for (int down = 0; down <= MAX_STAND; down++) {
            if (solid(level, pos.below(down + 1))) {
                return pos.getY() - down;
            }
        }
        return null;
    }

    private static boolean solid(ServerLevel level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        for (Iterator<Quake> it = ACTIVE.iterator(); it.hasNext(); ) {
            Quake quake = it.next();
            if (quake.level != level) {
                continue;
            }
            quake.step++;
            shake(level, quake);
            if (quake.step >= quake.heights.length - 1) {
                it.remove();
            }
        }
    }

    /** One block of the way: what stands where the front is now is hit, unless it was already. */
    private static void shake(ServerLevel level, Quake quake) {
        Vec3 front = quake.origin.add(quake.dir.scale(quake.step));
        int foot = quake.heights[quake.step];
        Vec3 dir = quake.dir;
        double reach = TremorRules.HALF_WIDTH + 1.5;
        AABB box = new AABB(front.x - reach, foot - 1.5, front.z - reach, front.x + reach, foot + TremorRules.MAX_ABOVE + 0.5, front.z + reach);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                t -> t != quake.caster && t.isAlive() && quake.affects.test(t))) {
            Vec3 offset = target.position().subtract(front);
            double along = offset.x * dir.x + offset.z * dir.z;
            double across = offset.z * dir.x - offset.x * dir.z;
            if (!TremorRules.catches(along, across, target.getY() - foot, target.getBbWidth() / 2) || !quake.hit.add(target.getId())) {
                continue;
            }
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.EARTH, quake.caster, quake.caster), DAMAGE * quake.power);
            ElementalReactions.earthHit(target, quake.caster, CRYSTAL_TICKS);
            double hold = Math.max(0, 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            if (hold > 0) {
                target.setDeltaMovement(target.getDeltaMovement().add(0, LIFT * hold, 0));
                target.hurtMarked = true;
            }
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 2));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
