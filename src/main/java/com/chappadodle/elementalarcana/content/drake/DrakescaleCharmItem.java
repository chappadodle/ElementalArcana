package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * A Drakescale Charm (see the Drakes spec): carried anywhere in the inventory, its element's
 * everyday harm can't hurt you (DrakescaleCharms).
 */
public class DrakescaleCharmItem extends Item {
    private final Element element;

    public DrakescaleCharmItem(Element element, Properties properties) {
        super(properties);
        this.element = element;
    }

    public Element element() {
        return element;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.drakescale_charm." + element.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.GRAY));
    }
}
