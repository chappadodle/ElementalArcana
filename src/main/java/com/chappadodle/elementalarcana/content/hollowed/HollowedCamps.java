package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.api.HollowedRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

/**
 * What a Hollowed camp does (see the Hollowed spec): bringing its people (and anyone's), and its
 * obelisk's shattering.
 */
public final class HollowedCamps {
    /** How long the obelisk's shattering weakens its people. */
    private static final int SHATTERED_TICKS = 600;

    private HollowedCamps() {
    }

    /** Brings one of the Hollowed to {@code at}, if there's room for it and ground under it; null if not. */
    @Nullable
    public static <T extends HollowedEntity> T summon(ServerLevel level, EntityType<T> type, BlockPos at, MobSpawnType reason) {
        if (!level.noCollision(type.getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))
                || !level.getBlockState(at.below()).isSolid()) {
            return null;
        }
        T hollowed = type.create(level);
        if (hollowed == null) {
            return null;
        }
        hollowed.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(hollowed, level, level.getCurrentDifficultyAt(at), reason, null);
        level.addFreshEntity(hollowed);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, hollowed.getX(), hollowed.getY(0.5), hollowed.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        return hollowed;
    }

    /** An obelisk at {@code pos} is broken: its people near are weakened and slowed for a while. */
    public static void shatter(ServerLevel level, BlockPos pos) {
        for (HollowedEntity hollowed : level.getEntitiesOfClass(HollowedEntity.class, new AABB(pos).inflate(HollowedRules.OBELISK_RADIUS))) {
            hollowed.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, SHATTERED_TICKS, 0));
            hollowed.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHATTERED_TICKS, 0));
            hollowed.removeEffect(MobEffects.REGENERATION);
            hollowed.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 120, 0.5, 1.5, 0.5, 0.4);
        level.sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.1);
        level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.5f, 0.5f);
        level.playSound(null, pos, SoundEvents.WARDEN_DEATH, SoundSource.HOSTILE, 0.8f, 1.6f);
    }
}
