package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The Drakescale Charms' ward (see the Drakes spec): a player carrying one can't be hurt by its
 * element's everyday harm: fire and burning (not lava), freezing, lightning strikes, drowning, falls.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class DrakescaleCharms {

    private DrakescaleCharms() {
    }

    /** Whether {@code player} carries a charm of {@code element}. */
    public static boolean carries(Player player, Element element) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).getItem() instanceof DrakescaleCharmItem charm && charm.element() == element) {
                return true;
            }
        }
        return false;
    }

    /** The element whose charm wards off {@code source}, or null. */
    static Element wardedBy(DamageSource source) {
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.HOT_FLOOR)) {
            return Element.FIRE;
        }
        if (source.is(DamageTypes.FREEZE)) {
            return Element.ICE;
        }
        if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            return Element.LIGHTNING;
        }
        if (source.is(DamageTypes.DROWN)) {
            return Element.WATER;
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return Element.WIND;
        }
        return null;
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        Element element = wardedBy(event.getSource());
        if (element != null && carries(player, element)) {
            event.setCanceled(true);
            if (element == Element.FIRE) {
                player.clearFire();
            }
        }
    }
}
