package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.HollowedRules;
import com.chappadodle.elementalarcana.content.hollow.ModHollow;
import com.chappadodle.elementalarcana.content.pouch.CharmPouches;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * The Hungerward Charm (see the Hollowed spec): carried, or in a Charm Pouch, it halves the mana the
 * Hollowed and the Hollow eat (they ask {@link #warded}) and takes a quarter off their hunger's harm.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Hungerward {
    private Hungerward() {
    }

    public static boolean warded(Player player) {
        return CharmPouches.carries(player, stack -> stack.is(ModHollowed.HUNGERWARD.get()));
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()
                && (event.getSource().is(ModHollowed.HOLLOWED_DAMAGE) || event.getSource().is(ModHollow.HUNGER)) && warded(player)) {
            event.setAmount(HollowedRules.damage(event.getAmount(), true));
        }
    }
}
