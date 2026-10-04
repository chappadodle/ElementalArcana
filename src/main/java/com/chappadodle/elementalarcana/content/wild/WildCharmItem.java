package com.chappadodle.elementalarcana.content.wild;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * One of the Trophies of the Wild (see its spec): carried anywhere in the inventory, it teaches its
 * creature's trick (WildCharms). Its tooltip says which.
 */
public class WildCharmItem extends Item {
    public WildCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
