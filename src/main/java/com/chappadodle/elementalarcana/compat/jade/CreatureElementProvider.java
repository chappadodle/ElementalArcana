package com.chappadodle.elementalarcana.compat.jade;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

/**
 * Adds "Element: Fire" (in the element's color) to Jade's tooltip for elemental creatures. Can be
 * turned off in Jade's config ("Creature element").
 */
public enum CreatureElementProvider implements IEntityComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ElementalArcana.id("creature_element");

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!(accessor.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Element element = CreatureElements.elementOf(living);
        if (element == null) {
            return;
        }
        Component name = Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT))
                .withColor(element.color());
        tooltip.add(Component.translatable("jade.elementalarcana.element", name));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
