package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.gear.FocusItem;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * What a Sovereign leaves when it falls: its heart, of its element (the elementalarcana:element
 * component, which also tints it). The key to archmage gear, and to the way to the Hollow.
 */
public class SovereignHeartItem extends Item {

    public SovereignHeartItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Element element) {
        ItemStack stack = new ItemStack(ModSanctums.SOVEREIGN_HEART.get());
        stack.set(ModGear.ELEMENT.get(), element);
        return stack;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Element element = FocusItem.elementOf(stack);
        if (element != null) {
            tooltip.add(Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT))
                    .withColor(element.color()));
        }
        tooltip.add(Component.translatable("item.elementalarcana.sovereign_heart.desc").withStyle(ChatFormatting.GRAY));
    }
}
