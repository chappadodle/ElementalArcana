package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.network.ManaTidePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Mana tides (see the mana sense and weather spec): every 4 to 8 days (the first after 3 to 6), at
 * nightfall, a tide rises and runs until dawn. While it runs, mana regenerates faster, awakening,
 * Attunement and wisps come twice as often, and new creatures are 5 levels stronger. Kept with the
 * world; clients are told when it turns (the violet fog, client/ManaTideClient).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public class ManaTides extends SavedData {
    private static final String NAME = "elementalarcana_mana_tides";
    private static final long DAY = 24000;
    private static final long NIGHTFALL = 12500;
    private static final long DAWN = 23000;
    /** How long a tide forced by command runs. */
    private static final long FORCED_TICKS = 6000;

    private static boolean active;

    private long nextTideAt = -1;
    private long endsAt;

    /** Whether a mana tide runs now (server side; clients ask ManaTideClient). */
    public static boolean active() {
        return active;
    }

    private static ManaTides get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ManaTides::new, ManaTides::load, null), NAME);
    }

    private static ManaTides load(CompoundTag tag, HolderLookup.Provider registries) {
        ManaTides tides = new ManaTides();
        tides.nextTideAt = tag.getLong("next_tide_at");
        tides.endsAt = tag.getLong("ends_at");
        return tides;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("next_tide_at", nextTideAt);
        tag.putLong("ends_at", endsAt);
        return tag;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        if (now % 20 != 0) {
            return;
        }
        ManaTides tides = get(server);
        if (tides.nextTideAt < 0) {
            tides.nextTideAt = now + DAY * (3 + overworld.getRandom().nextInt(4));
            tides.setDirty();
        }
        long dayTime = overworld.getDayTime() % DAY;
        if (tides.endsAt <= now && now >= tides.nextTideAt && dayTime >= NIGHTFALL && dayTime < NIGHTFALL + 1000) {
            tides.endsAt = now + (DAWN - dayTime);
            tides.setDirty();
        }
        turn(server, tides, tides.endsAt > now);
    }

    /** Starts a tide now (an operator's), for 5 minutes. */
    public static void force(MinecraftServer server) {
        ManaTides tides = get(server);
        tides.endsAt = server.overworld().getGameTime() + FORCED_TICKS;
        tides.setDirty();
        turn(server, tides, true);
    }

    /** Ends the tide now (an operator's). */
    public static void calm(MinecraftServer server) {
        ManaTides tides = get(server);
        tides.endsAt = server.overworld().getGameTime();
        tides.setDirty();
        turn(server, tides, false);
    }

    private static void turn(MinecraftServer server, ManaTides tides, boolean now) {
        if (now == active) {
            return;
        }
        active = now;
        if (!now) {
            tides.nextTideAt = server.overworld().getGameTime() + DAY * (4 + server.overworld().getRandom().nextInt(5));
            tides.setDirty();
        }
        Component message = Component.translatable(now ? "message.elementalarcana.tide.rises" : "message.elementalarcana.tide.ebbs")
                .withStyle(now ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY);
        server.getPlayerList().broadcastSystemMessage(message, false);
        PacketDistributor.sendToAllPlayers(new ManaTidePayload(now));
    }

    @SubscribeEvent
    public static void onLogIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, new ManaTidePayload(active));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        active = false;
    }
}
