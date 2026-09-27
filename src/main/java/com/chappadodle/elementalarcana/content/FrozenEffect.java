package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Frozen solid: can't move, jump or deal melee damage, and can't be knocked around. The target
 * stays frosted over (vanilla freeze overlay) and glitters with frost while it lasts.
 */
public class FrozenEffect extends MobEffect {

    public FrozenEffect() {
        super(MobEffectCategory.HARMFUL, 0x9EE6FF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ElementalArcana.id("frozen_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, ElementalArcana.id("frozen_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, ElementalArcana.id("frozen_damage"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.KNOCKBACK_RESISTANCE, ElementalArcana.id("frozen_knockback"), 1.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.setTicksFrozen(Math.max(entity.getTicksFrozen(), entity.getTicksRequiredToFreeze() + 10));
        if (entity.level() instanceof ServerLevel level) {
            double width = entity.getBbWidth() * 0.5;
            level.sendParticles(ModContent.FROST_SPARKLE.get(), entity.getX(), entity.getY(0.5), entity.getZ(), 2, width, entity.getBbHeight() * 0.4, width, 0.0);
            level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY(0.8), entity.getZ(), 1, width, 0.1, width, 0.0);
        }
        return true;
    }
}
