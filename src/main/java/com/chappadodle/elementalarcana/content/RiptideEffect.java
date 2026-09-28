package com.chappadodle.elementalarcana.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/** Riptide mark (Hydro Jet Lv 4): a ring of water circles the target until a hit bursts it. */
public class RiptideEffect extends MobEffect {

    public RiptideEffect() {
        super(MobEffectCategory.HARMFUL, 0x1F5FC8);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 2 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            float angle = entity.tickCount * 0.5f;
            double radius = entity.getBbWidth() * 0.5 + 0.35;
            for (int i = 0; i < 2; i++) {
                float a = angle + i * Mth.PI;
                level.sendParticles(ModContent.HYDRO_DROP.get(), entity.getX() + Mth.cos(a) * radius, entity.getY(0.5),
                        entity.getZ() + Mth.sin(a) * radius, 0, -Mth.sin(a), 0.1, Mth.cos(a), 0.08);
            }
        }
        return true;
    }
}
