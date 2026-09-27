package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** A cone of water in front of the caster: pushes, extinguishes, and hurts water-sensitive mobs. */
public class TidalWaveSpell extends Spell {
    private static final double RANGE = 6.0;
    // cos of the cone's half-angle (~40 degrees)
    private static final double CONE_COS = 0.76;

    public TidalWaveSpell() {
        super(ModSchools.WATER, 25, 40);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        Vec3 eye = context.eyePosition();
        Vec3 look = context.look();

        caster.clearFire();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(RANGE),
                entity -> entity != caster && entity.isAlive())) {
            Vec3 toTarget = target.position().add(0, target.getBbHeight() / 2, 0).subtract(eye);
            if (toTarget.length() > RANGE || toTarget.normalize().dot(look) < CONE_COS) {
                continue;
            }
            target.knockback(1.2 * context.power(), -look.x, -look.z);
            target.setDeltaMovement(target.getDeltaMovement().add(0, 0.25, 0));
            target.hurtMarked = true;
            if (target.isOnFire()) {
                target.clearFire();
            }
            if (target.isSensitiveToWater()) {
                target.hurt(level.damageSources().drown(), 5f * context.power());
            }
        }

        for (double distance = 1; distance <= RANGE; distance += 1) {
            Vec3 point = eye.add(look.scale(distance));
            double spread = 0.3 + distance * 0.2;
            extinguishFireAround(level, BlockPos.containing(point), (int) Math.ceil(spread));
            level.sendParticles(ParticleTypes.SPLASH, point.x, point.y, point.z, 20, spread, spread * 0.5, spread, 0.1);
            level.sendParticles(ParticleTypes.FISHING, point.x, point.y - 0.5, point.z, 6, spread, 0.2, spread, 0.02);
        }
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1f, 0.8f);
        return CastResult.SUCCESS;
    }

    private static void extinguishFireAround(ServerLevel level, BlockPos center, int radius) {
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                level.removeBlock(pos, false);
                any = true;
            }
        }
        if (any) {
            level.playSound(null, center, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.7f, 1f);
        }
    }
}
