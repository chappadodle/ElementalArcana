package com.chappadodle.elementalarcana.content.gear;

import com.chappadodle.elementalarcana.content.forge.ModForge;
import com.chappadodle.elementalarcana.api.TemperRules;
import com.chappadodle.elementalarcana.content.relic.Relics;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.StatGear;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.core.StatPoints;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * The stat points a player's gear adds: every piece worn in its own slot, and the better of the
 * foci held in the two hands, as long as the player's level meets the item's requirement. A shrine's
 * blessing adds here too (+5 Affinity of its element family and +2 Potency), and so do the Elixirs
 * of Focus and Warding, and the relic the player bears (see Relics). A piece tempered at an Ember
 * Anvil adds one more to each of its stats a temper (TemperRules).
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
                bonus(gear, stack).forEach((key, value) -> stats.merge(key, value, Integer::sum));
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
            bonus((StatGear) best.getItem(), best).forEach((key, value) -> stats.merge(key, value, Integer::sum));
        }
        for (Element family : new Element[]{Element.FIRE, Element.WATER, Element.WIND, Element.EARTH}) {
            if (player.hasEffect(ModContent.blessing(family))) {
                stats.merge(StatPoints.affinityKey(family), BLESSING_AFFINITY, Integer::sum);
                stats.merge("potency", BLESSING_POTENCY, Integer::sum);
            }
        }
        elixir(player, ModBrews.FOCUS, "focus", stats);
        elixir(player, ModBrews.WARDING, "ward", stats);
        Relics.addStats(player, stats);
        return stats;
    }

    /** A piece's stat points, tempered as it is. */
    public static Map<String, Integer> bonus(StatGear gear, ItemStack stack) {
        return TemperRules.tempered(gear.statBonus(stack), stack.getOrDefault(ModForge.TEMPERED.get(), 0));
    }

    /** An elixir drunk (see ModBrews): its stat, more for a strong one. */
    private static void elixir(Player player, Holder<MobEffect> effect, String stat, Map<String, Integer> stats) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null) {
            stats.merge(stat, ModBrews.ELIXIR_STAT + ModBrews.ELIXIR_STAT_PER_LEVEL * instance.getAmplifier(), Integer::sum);
        }
    }
}
