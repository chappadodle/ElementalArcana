package com.chappadodle.elementalarcana.content.forge;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.pouch.CharmPouches;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The Forgefire Charm (see the Cinder Forges spec), carried or in the Charm Pouch: fire and lava
 * harm its bearer a third less, and when they burn, the fire goes out twice as fast.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ForgeEvents {
    /** The share of fire's harm that gets through. */
    private static final float FIRE_TAKEN = 2f / 3f;

    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide() && event.getSource().is(DamageTypeTags.IS_FIRE)
                && CharmPouches.carries(player, stack -> stack.is(ModForge.FORGEFIRE_CHARM.get()))) {
            event.setAmount(event.getAmount() * FIRE_TAKEN);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.getRemainingFireTicks() > 0 && player.tickCount % 2 == 0
                && CharmPouches.carries(player, stack -> stack.is(ModForge.FORGEFIRE_CHARM.get()))) {
            // Two ticks more off its burning every other tick (one a tick, on top of the fire's own): it burns out twice as fast.
            player.setRemainingFireTicks(player.getRemainingFireTicks() - 2);
        }
    }
}
