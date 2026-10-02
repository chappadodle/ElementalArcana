package com.chappadodle.elementalarcana.content.tower;

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
 * What a Magister leaves when it falls: a core of its element (the elementalarcana:element
 * component, which also tints its gem). The key to master gear.
 */
public class GuardianCoreItem extends Item {

    public GuardianCoreItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Element element) {
        ItemStack stack = new ItemStack(ModTowers.GUARDIAN_CORE.get());
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
        tooltip.add(Component.translatable("item.elementalarcana.guardian_core.desc").withStyle(ChatFormatting.GRAY));
    }
}
