package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A fragment of an element's magic, dropped by elemental creatures (see CreatureRewards). It has
 * no use yet: it is a material for later features. Its name is shown in the element's color.
 */
public class ElementalEssenceItem extends Item {
    private final Element element;

    public ElementalEssenceItem(Element element, Properties properties) {
        super(properties);
        this.element = element;
    }

    public Element element() {
        return element;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withColor(element.color());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.essence.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
