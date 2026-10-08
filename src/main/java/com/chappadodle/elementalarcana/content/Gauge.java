package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A balance gauge for testing fights (`/arcana gauge start|stop`): while it runs for a player, it
 * counts the damage they deal and take, and keeps them standing (their health is topped up after
 * every hit), noting when the damage taken first passed their max health: the moment they would
 * have fallen. Stopping it reports the fight, against the foe named or else the biggest one they hit.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Gauge {
    private static final Map<UUID, Reading> READINGS = new HashMap<>();

    private static final class Reading {
        final long startTick;
        final int startLevel;
        final float startMaxHealth;
        float dealt;
        float taken;
        long fellAt = -1;
        long lastHitTick = -1;
        LivingEntity lastFoe;
        final Map<String, Float> takenBy = new java.util.TreeMap<>();
        final Map<String, Float> dealtTo = new java.util.TreeMap<>();

        Reading(long startTick, int startLevel, float startMaxHealth) {
            this.startTick = startTick;
            this.startLevel = startLevel;
            this.startMaxHealth = startMaxHealth;
        }
    }

    private Gauge() {
    }

    public static void start(ServerPlayer player) {
        READINGS.put(player.getUUID(), new Reading(player.serverLevel().getGameTime(), CreatureLevels.levelOf(player), player.getMaxHealth()));
        player.setHealth(player.getMaxHealth());
    }

    /** Ends the reading and describes it; {@code target} (if any) is the foe it was against. */
    public static Component stop(ServerPlayer player, LivingEntity target) {
        Reading reading = READINGS.remove(player.getUUID());
        if (reading == null) {
            return Component.literal("No gauge running");
        }
        if (target == null) {
            target = reading.lastFoe;
        }
        long end = reading.lastHitTick >= 0 && target != null && !target.isAlive() ? reading.lastHitTick : player.serverLevel().getGameTime();
        float seconds = Math.max(0.05f, (end - reading.startTick) / 20f);
        String foe = target == null ? "" : String.format(Locale.ROOT, "; foe %s Lv %d: %s, health %.1f/%.1f",
                target.getType().toShortString(), CreatureLevels.levelOf(target), target.isAlive() ? "standing" : "fallen",
                Math.max(0, target.getHealth()), target.getMaxHealth());
        String by = breakdown(reading.takenBy);
        String to = breakdown(reading.dealtTo);
        String fell = reading.fellAt < 0 ? "never fell" : String.format(Locale.ROOT, "would have fallen at %.1fs", (reading.fellAt - reading.startTick) / 20f);
        return Component.literal(String.format(Locale.ROOT,
                "Gauge: %.1fs, player Lv %d (max health %.1f); dealt %.1f (%.1f/s: %s); took %.1f (%.1f/s, %.0f%% of max health: %s); %s%s",
                seconds, reading.startLevel, reading.startMaxHealth, reading.dealt, reading.dealt / seconds, to,
                reading.taken, reading.taken / seconds, 100 * reading.taken / reading.startMaxHealth, by, fell, foe));
    }

    private static String breakdown(Map<String, Float> amounts) {
        StringBuilder out = new StringBuilder();
        amounts.entrySet().stream().sorted(Map.Entry.<String, Float>comparingByValue().reversed())
                .forEach(e -> out.append(out.isEmpty() ? "" : ", ").append(String.format(Locale.ROOT, "%s %.1f", e.getKey(), e.getValue())));
        return out.toString();
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (READINGS.isEmpty() || event.getEntity().level().isClientSide()) {
            return;
        }
        LivingEntity hurt = event.getEntity();
        if (event.getSource().getEntity() instanceof Player attacker && attacker != hurt && !(hurt instanceof Player)) {
            Reading reading = READINGS.get(attacker.getUUID());
            if (reading != null) {
                reading.dealt += event.getNewDamage();
                reading.dealtTo.merge(hurt.getType().toShortString(), event.getNewDamage(), Float::sum);
                // The foe is the biggest thing they hit (not a boss's summoned helpers).
                if (reading.lastFoe == null || hurt.getMaxHealth() >= reading.lastFoe.getMaxHealth()) {
                    reading.lastFoe = hurt;
                    reading.lastHitTick = hurt.level().getGameTime();
                }
            }
        }
        if (hurt instanceof ServerPlayer player) {
            Reading reading = READINGS.get(player.getUUID());
            if (reading != null) {
                reading.taken += event.getNewDamage();
                reading.takenBy.merge(event.getSource().getMsgId(), event.getNewDamage(), Float::sum);
                if (reading.fellAt < 0 && reading.taken >= reading.startMaxHealth) {
                    reading.fellAt = player.serverLevel().getGameTime();
                }
                player.setHealth(player.getMaxHealth());
            }
        }
    }
}
