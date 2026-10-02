package com.chappadodle.elementalarcana.content.gear;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.StatGear;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.core.StatPoints;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * The stat points a player's gear adds: every piece worn in its own slot, and the better of the
 * foci held in the two hands, as long as the player's level meets the item's requirement. A shrine's
 * blessing adds here too (+5 Affinity of its element family and +2 Potency).
 */
public final class GearStats {
    private static final int BLESSING_AFFINITY = 5;
    private static final int BLESSING_POTENCY = 2;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private GearStats() {
    }

    public static Map<String, Integer> compute(Player player) {
        MagicData data = MagicAttachments.get(player);
        Map<String, Integer> stats = new HashMap<>();
        for (EquipmentSlot slot : ARMOR) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof StatGear gear && gear.countsIn(slot) && data.level() >= gear.requiredLevel()) {
                gear.statBonus(stack).forEach((key, value) -> stats.merge(key, value, Integer::sum));
            }
        }
        ItemStack best = ItemStack.EMPTY;
        int bestTier = 0;
        for (EquipmentSlot hand : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack stack = player.getItemBySlot(hand);
            if (stack.getItem() instanceof StatGear gear && gear.countsIn(hand) && gear.focusTier() > bestTier
                    && data.level() >= gear.requiredLevel()) {
                best = stack;
                bestTier = gear.focusTier();
            }
        }
        if (!best.isEmpty()) {
            ((StatGear) best.getItem()).statBonus(best).forEach((key, value) -> stats.merge(key, value, Integer::sum));
        }
        for (Element family : new Element[]{Element.FIRE, Element.WATER, Element.WIND, Element.EARTH}) {
            if (player.hasEffect(ModContent.blessing(family))) {
                stats.merge(StatPoints.affinityKey(family), BLESSING_AFFINITY, Integer::sum);
                stats.merge("potency", BLESSING_POTENCY, Integer::sum);
            }
        }
        return stats;
    }
}
