package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.ConfigRates;
import com.chappadodle.elementalarcana.api.HollowedRules;
import com.chappadodle.elementalarcana.api.StarfallRules;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The Hollowed's dusk patrols (see the Hollowed spec): as the Overworld turns to night, each
 * awakened player of level 15 or more (not in creative or spectating) may be hunted, by the chance
 * HollowedRules gives for their level at the server's rate: a band comes from 24 to 40 blocks off,
 * on open ground, with the player as its quarry, and the player hears it coming.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class HollowedPatrols {
    private static final long DUSK = 13000;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 40;
    /** The Overworld's day time when it last ticked (-1: not yet), so dusk is rolled for once as it passes. */
    private static long lastDayTime = -1;

    private HollowedPatrols() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long dayTime = level.getDayTime();
        long before = lastDayTime < 0 ? dayTime : lastDayTime;
        lastDayTime = dayTime;
        if (before == dayTime || !StarfallRules.crosses(before, dayTime, DUSK) || !WispSpawner.canSpawn(level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            MagicData data = MagicAttachments.get(player);
            if (player.isSpectator() || player.isCreative() || !data.isAwakened()) {
                continue;
            }
            double chance = ConfigRates.scaled(HollowedRules.patrolChance(data.level()), ArcanaServerConfig.HOLLOWED_PATROLS.get());
            if (level.getRandom().nextDouble() < chance) {
                send(level, player);
            }
        }
    }

    /** Sends a band for {@code player}, sized by their level; how many came (none if there was no open ground). */
    public static int send(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        int magicLevel = MagicAttachments.get(player).level();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getFluidState(ground.below()).isEmpty() || !level.getFluidState(ground).isEmpty()) {
                continue;
            }
            List<EntityType<? extends HollowedEntity>> band = new ArrayList<>();
            for (int i = 0; i < HollowedRules.patrolAcolytes(magicLevel); i++) {
                band.add(ModHollowed.ACOLYTE.get());
            }
            for (int i = 0; i < HollowedRules.patrolDevourers(magicLevel); i++) {
                band.add(ModHollowed.DEVOURER.get());
            }
            int came = 0;
            for (int i = 0; i < band.size(); i++) {
                BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ground.offset(i % 2 * 2 - 1, 0, i / 2 * 2 - 1));
                HollowedEntity hollowed = HollowedCamps.summon(level, band.get(i), at, MobSpawnType.EVENT);
                if (hollowed != null) {
                    hollowed.setTarget(player);
                    came++;
                }
            }
            if (came > 0) {
                player.sendSystemMessage(Component.translatable("message.elementalarcana.hollowed.patrol").withStyle(ChatFormatting.DARK_PURPLE));
                player.playNotifySound(SoundEvents.WARDEN_NEARBY_CLOSER, SoundSource.HOSTILE, 0.7f, 0.6f);
                return came;
            }
        }
        return 0;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        lastDayTime = -1;
    }
}
