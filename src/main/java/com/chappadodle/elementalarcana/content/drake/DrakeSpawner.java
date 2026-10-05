package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.api.ConfigRates;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.DrakeRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Wild drakes (see the Drakes spec): now and then, by day, a drake of the land's element is born 40 to
 * 60 blocks from a player who's high up in its lands (tide drakes over the sea instead), far from the
 * world's centre and never with another drake within WILD_SPACING. In a thunderstorm, half of them
 * are storm drakes, wherever they are. Nest drakes come from their nests (NestHeartBlockEntity).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class DrakeSpawner {
    private static final Map<Element, TagKey<Biome>> LANDS = new EnumMap<>(Element.class);

    static {
        for (Element element : DrakeRules.ELEMENTS) {
            LANDS.put(element, TagKey.create(Registries.BIOME, ElementalArcana.id("drakes/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    private DrakeSpawner() {
    }

    /** The drake whose lands {@code biome} is, or null; {@code nesting} leaves out the tide drake, which doesn't nest. */
    @Nullable
    public static Element landsOf(Holder<Biome> biome, boolean nesting) {
        for (Element element : DrakeRules.ELEMENTS) {
            if ((!nesting || element != Element.WATER) && biome.is(LANDS.get(element))) {
                return element;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % DrakeRules.WILD_CHECK_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !WispSpawner.canSpawn(level) || !level.isDay() || player.isSpectator()
                || Math.hypot(player.getX(), player.getZ()) < DrakeRules.MIN_DISTANCE
                || level.getRandom().nextFloat() >= ConfigRates.scaled(DrakeRules.WILD_CHANCE, ArcanaServerConfig.DRAKES.get())) {
            return;
        }
        Element element = landsOf(level.getBiome(player.blockPosition()), false);
        boolean high = player.getY() >= DrakeRules.WILD_MIN_Y;
        if (level.isThundering() && high && level.getRandom().nextBoolean()) {
            element = Element.LIGHTNING;
        }
        if (element == null || element != Element.WATER && !high) {
            return;
        }
        spawnNear(level, player, element);
    }

    /** Brings a drake of {@code element} in 40 to 60 blocks from {@code player}, soaring; null if another is too near. */
    @Nullable
    public static DrakeEntity spawnNear(ServerLevel level, ServerPlayer player, Element element) {
        if (!level.getEntitiesOfClass(DrakeEntity.class, player.getBoundingBox().inflate(DrakeRules.WILD_SPACING)).isEmpty()) {
            return null;
        }
        double angle = level.getRandom().nextDouble() * Math.PI * 2;
        double distance = 40 + level.getRandom().nextDouble() * 20;
        int x = (int) (player.getX() + Math.cos(angle) * distance);
        int z = (int) (player.getZ() + Math.sin(angle) * distance);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        DrakeEntity drake = ModDrakes.drake(element).create(level);
        if (drake == null) {
            return null;
        }
        drake.setHome(new BlockPos(x, ground, z));
        drake.moveTo(x + 0.5, ground + 24, z + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        EventHooks.finalizeMobSpawn(drake, level, level.getCurrentDifficultyAt(drake.blockPosition()), MobSpawnType.NATURAL, null);
        level.addFreshEntity(drake);
        return drake;
    }
}
