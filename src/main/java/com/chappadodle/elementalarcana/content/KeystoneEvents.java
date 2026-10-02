package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Keystones;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** The keystones that live on game events (see Keystones for the rest): Gale Step's soft landings. */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class KeystoneEvents {

    private KeystoneEvents() {
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof Player player && MagicAttachments.get(player).hasKeystone(Keystones.GALE_STEP)) {
            event.setDamageMultiplier(0f);
        }
    }
}
