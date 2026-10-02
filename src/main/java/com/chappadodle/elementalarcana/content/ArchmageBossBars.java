package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.content.tower.TowerMageEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * An Archmage announces itself with a boss bar ("Zombie Archmage") for players within 32 blocks,
 * like the Wither's. The bar is purple for every element, so it doesn't give the element away.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ArchmageBossBars {
    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();
    private static final double VIEW_DISTANCE = 32;
    private static final int UPDATE_INTERVAL_TICKS = 5;

    private ArchmageBossBars() {
    }

    /** "Zombie Archmage", or for a tower's Magister "Magister of Fire". */
    private static Component title(LivingEntity mob, CreatureMagic magic) {
        if (mob instanceof TowerMageEntity mage && mage.isMagister()) {
            return Component.translatable("bossbar.elementalarcana.magister",
                    Component.translatable("school.elementalarcana." + magic.element().name().toLowerCase(Locale.ROOT)));
        }
        return Component.translatable("bossbar.elementalarcana.archmage", mob.getType().getDescription());
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().tickCount % UPDATE_INTERVAL_TICKS != 0 || !(event.getEntity() instanceof LivingEntity mob)
                || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        CreatureMagic magic = Attunement.get(mob);
        if (magic == null || magic.rank() != AttunementRank.ARCHMAGE) {
            return;
        }
        ServerBossEvent bar = BARS.computeIfAbsent(mob.getUUID(), id -> new ServerBossEvent(title(mob, magic),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS));
        bar.setProgress(mob.getHealth() / mob.getMaxHealth());
        for (ServerPlayer player : List.copyOf(bar.getPlayers())) {
            if (player.level() != level || player.distanceTo(mob) > VIEW_DISTANCE) {
                bar.removePlayer(player);
            }
        }
        for (ServerPlayer player : level.players()) {
            if (player.distanceTo(mob) <= VIEW_DISTANCE) {
                bar.addPlayer(player);
            }
        }
    }

    /** Death, despawn or chunk unload: take the bar down. */
    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        ServerBossEvent bar = BARS.remove(event.getEntity().getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BARS.clear();
    }
}
