package com.chappadodle.elementalarcana.api;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * An item that adds to its wearer's stats while worn or held (wands, staves, robes). Its bonuses go
 * into the same stats as stat points and skill tree nodes (see content/gear/GearStats), keyed like
 * StatPoints: a Stat's key ("potency") or an Affinity ("affinity/fire").
 */
public interface StatGear {

    /** The stat points {@code stack} adds. */
    Map<String, Integer> statBonus(ItemStack stack);

    /** The level a wearer needs before the item does anything (0 = none). */
    int requiredLevel();

    /** Whether the item counts in {@code slot} (armor in its own slot; foci in either hand). */
    boolean countsIn(EquipmentSlot slot);

    /** For foci: how strong a focus it is, so only the better one of two held counts. 0 = not a focus. */
    default int focusTier() {
        return 0;
    }
}
