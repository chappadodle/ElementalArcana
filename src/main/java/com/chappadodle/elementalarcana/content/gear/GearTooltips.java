package com.chappadodle.elementalarcana.content.gear;

import com.chappadodle.elementalarcana.content.forge.ModForge;
import com.chappadodle.elementalarcana.api.TemperRules;
import com.chappadodle.elementalarcana.api.StatGear;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The stat lines on gear tooltips: "+3 Potency", "+3 Fire Affinity" (tempered, if it is: and how far), and a level requirement. */
public final class GearTooltips {

    private GearTooltips() {
    }

    public static void add(StatGear gear, ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable(gear.focusTier() > 0 ? "tooltip.elementalarcana.gear.held" : "tooltip.elementalarcana.gear.worn")
                .withStyle(ChatFormatting.GRAY));
        for (Map.Entry<String, Integer> entry : new TreeMap<>(GearStats.bonus(gear, stack)).entrySet()) {
            tooltip.add(Component.translatable("tooltip.elementalarcana.gear.stat", entry.getValue(), statName(entry.getKey()))
                    .withStyle(ChatFormatting.BLUE));
        }
        int tempered = stack.getOrDefault(ModForge.TEMPERED.get(), 0);
        if (tempered > 0) {
            tooltip.add(Component.translatable("tooltip.elementalarcana.gear.tempered", tempered, TemperRules.MAX).withStyle(ChatFormatting.GOLD));
        }
        if (gear.requiredLevel() > 0) {
            tooltip.add(Component.translatable("tooltip.elementalarcana.gear.requires", gear.requiredLevel()).withStyle(ChatFormatting.GOLD));
        }
    }

    /** A stat key's name: "Potency", or "Fire Affinity". */
    public static Component statName(String key) {
        if (key.startsWith("affinity/")) {
            return Component.translatable("stat.elementalarcana.affinity",
                    Component.translatable("school.elementalarcana." + key.substring("affinity/".length())));
        }
        return Component.translatable("stat.elementalarcana." + key);
    }
}
