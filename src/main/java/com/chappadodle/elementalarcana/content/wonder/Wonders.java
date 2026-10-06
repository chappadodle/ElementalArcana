package com.chappadodle.elementalarcana.content.wonder;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.ConfigRates;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.WonderRules;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.flora.HerbBlock;
import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the Wonders of the Wild come from (see their spec). In the Overworld: every five seconds at
 * night, glowmoths may come to a herb or flower near each player (WonderRules counts them); every
 * ten seconds, a skyray may come within sight of a player out over the sea. And a player gliding
 * near a skyray (elytra or the mod's glide) has ridden its wake (the advancement; the lift itself is
 * the gliding client's, SkyrayWake).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Wonders {
    private static final double MOTH_CHANCE = 0.8;
    private static final double SKYRAY_CHANCE = 0.5;

    private Wonders() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        long time = level.getGameTime();
        if (time % 100 == 0 && WonderRules.mothTime(level.getDayTime())) {
            for (ServerPlayer player : level.players()) {
                if (level.getRandom().nextDouble() < ConfigRates.scaled(MOTH_CHANCE, ArcanaServerConfig.GLOWMOTHS.get())) {
                    bringMoths(level, player);
                }
            }
        }
        if (time % 200 == 0) {
            for (ServerPlayer player : level.players()) {
                if (level.getBiome(player.blockPosition()).is(BiomeTags.IS_OCEAN)
                        && level.getRandom().nextDouble() < ConfigRates.scaled(SKYRAY_CHANCE, ArcanaServerConfig.SKYRAYS.get())) {
                    bringSkyray(level, player, false);
                }
            }
        }
    }

    /** Brings two or three glowmoths to a herb or flower within 16 blocks of {@code player}; how many came. */
    public static int bringMoths(ServerLevel level, ServerPlayer player) {
        if (player.isSpectator()) {
            return 0;
        }
        RandomSource random = level.getRandom();
        int near = level.getEntitiesOfClass(GlowmothEntity.class, player.getBoundingBox().inflate(32)).size();
        int count = WonderRules.mothsToBring(near, random.nextDouble());
        if (count == 0) {
            return 0;
        }
        // Every column in reach, for the plants on top: one is picked (herbs are few, so none is left to chance).
        List<BlockPos> plants = new ArrayList<>();
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        for (int dx = -WonderRules.MOTH_REACH; dx <= WonderRules.MOTH_REACH; dx++) {
            for (int dz = -WonderRules.MOTH_REACH; dz <= WonderRules.MOTH_REACH; dz++) {
                column.set(player.getBlockX() + dx, 0, player.getBlockZ() + dz);
                if (!level.hasChunkAt(column)) {
                    continue;
                }
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                // The plant: at the top (a flower blocks no one), or just under it (a Moonlily, a lily pad, does).
                if (isPlant(level.getBlockState(top))) {
                    plants.add(top);
                } else if (isPlant(level.getBlockState(top.below()))) {
                    plants.add(top.below());
                }
            }
        }
        if (plants.isEmpty()) {
            return 0;
        }
        BlockPos plant = plants.get(random.nextInt(plants.size()));
        BlockState state = level.getBlockState(plant);
        Element element = state.getBlock() instanceof HerbBlock herb ? herb.herb().element() : Element.values()[random.nextInt(Element.values().length)];
        for (int i = 0; i < count; i++) {
            GlowmothEntity moth = ModWonders.GLOWMOTH.get().create(level);
            if (moth == null) {
                return i;
            }
            moth.setElement(element);
            moth.moveTo(plant.getX() + random.nextDouble(), plant.getY() + 1 + random.nextDouble() * 1.5, plant.getZ() + random.nextDouble(),
                    random.nextFloat() * 360f, 0);
            level.addFreshEntity(moth);
        }
        return count;
    }

    /** What draws glowmoths: a herb, or a flower. */
    private static boolean isPlant(BlockState state) {
        return state.getBlock() instanceof HerbBlock || state.is(BlockTags.FLOWERS);
    }

    /**
     * Brings a skyray within sight of {@code player}, high over the sea (48 to 96 blocks off, or 16 to
     * 32 when {@code near}, for the test command, over any ground), unless three are already about;
     * whether one came.
     */
    public static boolean bringSkyray(ServerLevel level, ServerPlayer player, boolean near) {
        if (level.getEntitiesOfClass(SkyrayEntity.class, player.getBoundingBox().inflate(128)).size() >= WonderRules.SKYRAYS_NEAR) {
            return false;
        }
        RandomSource random = level.getRandom();
        for (int tries = 0; tries < 8; tries++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = near ? 16 + random.nextDouble() * 16 : 48 + random.nextDouble() * 48;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z)) || !near && !level.getBiome(new BlockPos(x, 63, z)).is(BiomeTags.IS_OCEAN)) {
                continue;
            }
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            int y = Math.max(ground + 30, WonderRules.SKYRAY_MIN_Y + random.nextInt(WonderRules.SKYRAY_MAX_Y - WonderRules.SKYRAY_MIN_Y));
            if (y > WonderRules.SKYRAY_MAX_Y + 24) {
                continue;
            }
            SkyrayEntity skyray = ModWonders.SKYRAY.get().create(level);
            if (skyray == null) {
                return false;
            }
            skyray.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0);
            level.addFreshEntity(skyray);
            return true;
        }
        return false;
    }

    /** A glider near a skyray has ridden its wake. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0
                || !(player.isFallFlying() || player.getData(MagicAttachments.GLIDING))) {
            return;
        }
        if (!player.serverLevel().getEntitiesOfClass(SkyrayEntity.class, player.getBoundingBox().inflate(WonderRules.WAKE_NOTICE)).isEmpty()) {
            MagicTriggers.fire(player, "skyray_wake", null, 1);
        }
    }
}
