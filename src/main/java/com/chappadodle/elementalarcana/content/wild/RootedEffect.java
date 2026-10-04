package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

/**
 * Rooted (a Thornwood Treant's stamp): roots hold the feet fast, so no walking and no jumping,
 * though the hands are free. Bits of root and earth keep crumbling around the feet while it lasts.
 */
public class RootedEffect extends MobEffect {
    public RootedEffect() {
        super(MobEffectCategory.HARMFUL, 0x6B4A2B);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ElementalArcana.id("rooted_speed"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, ElementalArcana.id("rooted_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            double width = entity.getBbWidth() * 0.5 + 0.2;
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState()),
                    entity.getX(), entity.getY() + 0.2, entity.getZ(), 3, width, 0.15, width, 0.0);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()),
                    entity.getX(), entity.getY() + 0.05, entity.getZ(), 2, width, 0.05, width, 0.0);
        }
        return true;
    }
}
