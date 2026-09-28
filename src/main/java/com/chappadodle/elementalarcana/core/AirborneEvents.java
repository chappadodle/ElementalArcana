package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Airborne: a creature launched by wind takes 25% more damage from everything while it's off the ground. */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class AirborneEvents {
    private static final float AIRBORNE_BONUS = 1.25f;

    private AirborneEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.hasEffect(ModContent.AIRBORNE) && !target.onGround()) {
            event.setAmount(event.getAmount() * AIRBORNE_BONUS);
        }
    }
}
