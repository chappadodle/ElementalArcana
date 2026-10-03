package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Stormeye's Eye of the Storm (level 9; see StormeyeEntity): an Airborne creature a tornado holds
 * takes double damage from everything. Who's held is kept by creature id, each until the tick
 * after the tornado last held it; never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Stormeyes {
    private static final Map<Integer, Long> IN_EYE = new HashMap<>();

    private Stormeyes() {
    }

    static void holdInEye(LivingEntity target, long now) {
        IN_EYE.put(target.getId(), now + 1);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Long until = IN_EYE.get(target.getId());
        if (until == null) {
            return;
        }
        if (until < target.level().getGameTime()) {
            IN_EYE.remove(target.getId());
        } else if (target.hasEffect(ModContent.AIRBORNE)) {
            event.setAmount(event.getAmount() * 2);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        IN_EYE.clear();
    }
}
