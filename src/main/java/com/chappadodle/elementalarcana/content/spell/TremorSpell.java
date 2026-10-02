package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Earth's ground spell: a shockwave rolls along the ground for 10 blocks, throwing creatures up and
 * slowing them for 3 seconds, with a small hit of damage. It Crystallizes anything burning, wet or
 * frozen. Attuned Earth Magi cast it too.
 */
public class TremorSpell extends Spell {
    private static final int LENGTH = 10;
    private static final double HALF_WIDTH = 1.6;

    public TremorSpell() {
        super(ModSchools.EARTH, 30, 120);
    }

    @Override
    public CastResult cast(CastContext context) {
        Vec3 look = context.caster().getLookAngle();
        shockwave(context.level(), context.caster(), new Vec3(look.x, 0, look.z), context.power(), target -> true);
        return CastResult.SUCCESS;
    }

    /**
     * The shockwave itself: from {@code caster} along {@code direction} (horizontal), hurting, throwing
     * up and slowing everything {@code affects} allows (never the caster), with the cracking ground
     * drawn as block particles. Attuned creatures cast it too.
     */
    public static void shockwave(ServerLevel level, LivingEntity caster, Vec3 direction, float power, Predicate<LivingEntity> affects) {
        Vec3 dir = direction.lengthSqr() < 1.0e-6 ? new Vec3(0, 0, 1) : direction.normalize();
        Vec3 origin = caster.position();
        Set<Integer> hit = new HashSet<>();
        for (int step = 1; step <= LENGTH; step++) {
            Vec3 at = origin.add(dir.scale(step));
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(HALF_WIDTH, 1.5, HALF_WIDTH),
                    e -> e != caster && e.isAlive() && affects.test(e) && !hit.contains(e.getId()))) {
                hit.add(target.getId());
                SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.EARTH, caster, caster), 3f * power);
                ElementalReactions.earthHit(target, caster, 200);
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.6, 0));
                target.hurtMarked = true;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            }
            // The cracking ground: block crumbs from whatever is underfoot.
            BlockPos ground = BlockPos.containing(at.x, at.y - 0.5, at.z);
            BlockState state = level.getBlockState(ground);
            if (state.isAir()) {
                state = level.getBlockState(ground.below());
            }
            if (!state.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y + 0.1, at.z, 14, 0.5, 0.15, 0.5, 0.2);
            }
        }
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.6f);
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.4f, 0.5f);
    }
}
