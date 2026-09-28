package com.chappadodle.elementalarcana.content;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Soaked: drips water and puts out any fire it catches. Fire and ice react with it (see ElementalReactions). */
public class WetEffect extends MobEffect {

    public WetEffect() {
        super(MobEffectCategory.NEUTRAL, 0x3F8CE8);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 5 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.isOnFire()) {
            entity.clearFire();
        }
        if (entity.level() instanceof ServerLevel level) {
            double width = entity.getBbWidth() * 0.45;
            level.sendParticles(ParticleTypes.FALLING_WATER, entity.getX(), entity.getY(0.6), entity.getZ(), 2, width, entity.getBbHeight() * 0.3, width, 0);
        }
        return true;
    }
}
