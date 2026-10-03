package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.slf4j.Logger;

/**
 * Keeps damage finite. The mod's bonuses multiply whatever hit comes in (Airborne, Eye of the
 * Storm, the element chart, levels), and a hit already at the largest float (the /kill command's)
 * overflows to infinity; the game's armour and absorption maths then leave the creature with NaN
 * health: neither alive nor dying, beyond commands and spells, never removed. So the mod's last
 * word on any hit is a finite amount (at most vanilla's own /kill), and a creature that turns up
 * with NaN health (saved in a world before this) is put down when it loads.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class DamageSafety {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DamageSafety() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        float amount = event.getAmount();
        if (Float.isNaN(amount)) {
            event.setAmount(0f);
        } else if (amount > Float.MAX_VALUE) {
            event.setAmount(Float.MAX_VALUE);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof LivingEntity living && Float.isNaN(living.getHealth())) {
            LOGGER.warn("Putting down {}: its health is NaN", living);
            living.setHealth(0f);
        }
    }
}
