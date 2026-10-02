package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Boulder's Avalanche (Lv 10): where the boulder lands, rocks rain down on the area for 5 seconds,
 * one every few ticks, each crushing what it hits. Server-side, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Rockfall {
    private static final int DURATION_TICKS = 100;
    private static final int GAP_TICKS = 4;
    private static final double RADIUS = 4.0;
    private static final double HEIGHT = 11;

    private record Fall(ServerLevel level, Vec3 center, UUID owner, float power, int spellLevel, long endsAt) {
    }

    private static final List<Fall> FALLS = new ArrayList<>();

    private Rockfall() {
    }

    public static void start(ServerLevel level, Vec3 center, Entity owner, float power, int spellLevel) {
        FALLS.add(new Fall(level, center, owner.getUUID(), power, spellLevel, level.getGameTime() + DURATION_TICKS));
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || FALLS.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Fall> it = FALLS.iterator(); it.hasNext(); ) {
            Fall fall = it.next();
            if (fall.level() != level) {
                continue;
            }
            Entity owner = level.getEntity(fall.owner());
            if (now >= fall.endsAt() || owner == null || !owner.isAlive()) {
                it.remove();
                continue;
            }
            if (now % GAP_TICKS == 0) {
                double angle = level.random.nextDouble() * Math.PI * 2;
                double distance = Math.sqrt(level.random.nextDouble()) * RADIUS;
                Vec3 from = fall.center().add(Math.cos(angle) * distance, HEIGHT, Math.sin(angle) * distance);
                BoulderSpell.dropRock(owner, from, fall.power(), fall.spellLevel());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FALLS.clear();
    }
}
