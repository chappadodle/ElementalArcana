package com.chappadodle.elementalarcana.content.gear;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.StatGear;
import com.chappadodle.elementalarcana.core.StatPoints;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A wand or staff: held in either hand, it adds to Potency (and more) and to the Affinity of the
 * element it's attuned to (the {@link ModGear#ELEMENT} component, set by its recipe). Only the better
 * of two held foci counts.
 */
public class FocusItem extends Item implements StatGear {
    private final int tier;
    private final int requiredLevel;
    private final Map<String, Integer> baseStats;
    private final int affinity;

    public FocusItem(int tier, int requiredLevel, Map<String, Integer> baseStats, int affinity, Properties properties) {
        super(properties);
        this.tier = tier;
        this.requiredLevel = requiredLevel;
        this.baseStats = baseStats;
        this.affinity = affinity;
    }

    @Nullable
    public static Element elementOf(ItemStack stack) {
        return stack.get(ModGear.ELEMENT.get());
    }

    @Override
    public Map<String, Integer> statBonus(ItemStack stack) {
        Map<String, Integer> stats = new HashMap<>(baseStats);
        Element element = elementOf(stack);
        if (element != null) {
            stats.merge(StatPoints.affinityKey(element), affinity, Integer::sum);
        }
        return stats;
    }

    @Override
    public int requiredLevel() {
        return requiredLevel;
    }

    @Override
    public boolean countsIn(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
    }

    @Override
    public int focusTier() {
        return tier;
    }

    @Override
    public Component getName(ItemStack stack) {
        Element element = elementOf(stack);
        Component base = super.getName(stack);
        return element == null ? base : Component.translatable("item.elementalarcana.focus.named",
                Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT)), base).withColor(element.color());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        GearTooltips.add(this, stack, tooltip);
    }
}
