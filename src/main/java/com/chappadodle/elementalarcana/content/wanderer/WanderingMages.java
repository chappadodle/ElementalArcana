package com.chappadodle.elementalarcana.content.wanderer;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.ConfigRates;
import com.chappadodle.elementalarcana.api.StarfallRules;
import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Where Wandering Mages come from (see its spec): each Overworld morning, if none is about, one may
 * come (a chance in three, at the server's rate) to an awakened player, on open ground 24 to 40
 * blocks away, and stay two days; the player senses it, and which way.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WanderingMages {
    private static final long MORNING = 1000;
    private static final double CHANCE = 1.0 / 3.0;
    private static final int MIN_DISTANCE = 24;
    private static final int MAX_DISTANCE = 40;
    private static final int STAY_TICKS = 48000;
    /** The Overworld's day time when it last ticked (-1: not yet), so morning is rolled for once as it passes, not every tick time stands still on it. */
    private static long lastDayTime = -1;

    private WanderingMages() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long dayTime = level.getDayTime();
        long before = lastDayTime < 0 ? dayTime : lastDayTime;
        lastDayTime = dayTime;
        if (before == dayTime || !StarfallRules.crosses(before, dayTime, MORNING)
                || !level.getEntities(ModWanderer.WANDERING_MAGE.get(), mage -> true).isEmpty()
                || level.getRandom().nextDouble() >= ConfigRates.scaled(CHANCE, ArcanaServerConfig.WANDERING_MAGE.get())) {
            return;
        }
        List<ServerPlayer> awakened = level.players().stream()
                .filter(player -> !player.isSpectator() && MagicAttachments.get(player).isAwakened())
                .toList();
        if (!awakened.isEmpty()) {
            call(level, awakened.get(level.getRandom().nextInt(awakened.size())));
        }
    }

    /** Brings a Wandering Mage to open ground 24 to 40 blocks from {@code player}; whether one came. */
    public static boolean call(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getFluidState(pos.below()).isEmpty() || !level.getFluidState(pos).isEmpty()
                    || !level.noCollision(ModWanderer.WANDERING_MAGE.get().getSpawnAABB(x + 0.5, pos.getY(), z + 0.5))) {
                continue;
            }
            WanderingMageEntity mage = ModWanderer.WANDERING_MAGE.get().create(level);
            if (mage == null) {
                return false;
            }
            mage.moveTo(x + 0.5, pos.getY(), z + 0.5, random.nextFloat() * 360f, 0);
            mage.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            mage.setDespawnDelay(STAY_TICKS);
            mage.setWanderTarget(player.blockPosition());
            level.addFreshEntity(mage);
            Component direction = Component.translatable("direction.elementalarcana." + StarfallRules.direction(x - player.getX(), z - player.getZ()));
            player.sendSystemMessage(Component.translatable("message.elementalarcana.wandering_mage.near", direction).withStyle(ChatFormatting.LIGHT_PURPLE));
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.8f, 0.6f);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        lastDayTime = -1;
    }
}
