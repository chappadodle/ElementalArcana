package com.chappadodle.elementalarcana.compat.jade;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Adds "Level 12" to Jade's tooltip for creatures (players' levels are private). Can be turned off
 * in Jade's config ("Creature level").
 */
public enum CreatureLevelProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ElementalArcana.id("creature_level");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (accessor.getEntity() instanceof LivingEntity living) {
            int level = CreatureLevels.displayLevel(living);
            if (level > 0) {
                tooltip.add(Component.translatable("jade.elementalarcana.level", level));
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
