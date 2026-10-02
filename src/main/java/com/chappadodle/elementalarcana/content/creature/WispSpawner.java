package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Brings wisps into the world (see the wisps spec). In the wild, a spawner of its own (not the mob
 * cap) checks around each player once a minute: with a 1 in 4 chance a wisp appears 16 to 32 blocks
 * away, over open ground, of an element the biome holds (the {@code wisps/<element>} biome tags),
 * unless 2 are already within 48 blocks. A thunderstorm stirs them up (a 1 in 2 chance), and under
 * its open sky half of them are Lightning wisps whatever the biome: the storm is where they live.
 * Shrines call their guardians through here too.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WispSpawner {
    private static final int CHECK_TICKS = 20 * 60;
    private static final double CHANCE = 0.25;
    private static final double STORM_CHANCE = 0.5;
    private static final int MIN_DISTANCE = 16;
    private static final int MAX_DISTANCE = 32;
    private static final int CROWD_RADIUS = 48;
    private static final int CROWD = 2;
    /** A shrine keeps this many guardians. */
    private static final int GUARDIANS = 2;
    private static final Map<Element, TagKey<Biome>> BIOMES = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            BIOMES.put(element, TagKey.create(Registries.BIOME, ElementalArcana.id("wisps/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    private WispSpawner() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || (player.tickCount + player.getId()) % CHECK_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (canSpawn(level) && player.getRandom().nextDouble() < (level.isThundering() ? STORM_CHANCE : CHANCE)) {
            trySpawnWild(level, player, null);
        }
    }

    /** Wisps come only where monsters may: not on Peaceful, not with mob spawning off. */
    public static boolean canSpawn(ServerLevel level) {
        return level.getDifficulty() != Difficulty.PEACEFUL && level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
    }

    /**
     * Tries to bring a wisp into the open 16 to 32 blocks from {@code player}. With {@code forced}
     * (the test command), it's that element, whatever the biome and however many are near.
     * Returns the wisp, or null if there was no place for one.
     */
    @Nullable
    public static WispEntity trySpawnWild(ServerLevel level, ServerPlayer player, @Nullable Element forced) {
        RandomSource random = player.getRandom();
        for (int tries = 0; tries < 8; tries++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            BlockPos ground = openGround(level, Mth.floor(player.getX() + Math.cos(angle) * distance),
                    Mth.floor(player.getZ() + Math.sin(angle) * distance), player.getBlockY());
            if (ground == null) {
                continue;
            }
            Element element = forced != null ? forced
                    : level.isThundering() && level.canSeeSky(ground) && random.nextBoolean() ? Element.LIGHTNING
                    : biomeElement(level, ground, random);
            if (element == null) {
                continue;
            }
            if (forced == null && level.getEntitiesOfClass(WispEntity.class, new AABB(ground).inflate(CROWD_RADIUS)).size() >= CROWD) {
                return null;
            }
            WispEntity wisp = spawnAt(level, element, ground.above(1 + random.nextInt(2)), MobSpawnType.NATURAL);
            if (wisp != null) {
                return wisp;
            }
        }
        return null;
    }

    /** Tops a shrine's guardians up to two: wisps of its element that appear around the core and stay near it. */
    public static void spawnGuardians(ServerLevel level, BlockPos core, Element element) {
        int present = level.getEntitiesOfClass(WispEntity.class, new AABB(core).inflate(WispEntity.GUARD_RADIUS * 2),
                wisp -> wisp.element() == element).size();
        RandomSource random = level.getRandom();
        for (int i = present; i < GUARDIANS; i++) {
            for (int tries = 0; tries < 8; tries++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double distance = 2 + random.nextDouble() * 2.5;
                BlockPos at = BlockPos.containing(core.getX() + 0.5 + Math.cos(angle) * distance, core.getY() + random.nextInt(2),
                        core.getZ() + 0.5 + Math.sin(angle) * distance);
                WispEntity wisp = spawnAt(level, element, at, MobSpawnType.STRUCTURE);
                if (wisp != null) {
                    wisp.guard(core);
                    break;
                }
            }
        }
    }

    /** A wisp appearing at {@code at}, in a little burst of light, if there's room for it there. */
    @Nullable
    public static WispEntity spawnAt(ServerLevel level, Element element, BlockPos at, MobSpawnType reason) {
        EntityType<WispEntity> type = ModCreatures.wisp(element);
        if (!level.hasChunkAt(at) || !level.noCollision(type.getSpawnAABB(at.getX() + 0.5, at.getY(), at.getZ() + 0.5))) {
            return null;
        }
        WispEntity wisp = type.spawn(level, at, reason);
        if (wisp != null) {
            level.sendParticles(ParticleTypes.END_ROD, wisp.getX(), wisp.getY(0.5), wisp.getZ(), 12, 0.2, 0.2, 0.2, 0.05);
            level.playSound(null, wisp.getX(), wisp.getY(), wisp.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 0.8f, 1.6f);
        }
        return wisp;
    }

    /**
     * The first open block over the ground at (x, z), or null if there is none (or the chunk isn't
     * loaded). Under a ceiling (the Nether) it looks for a floor near {@code nearY} with room above.
     */
    @Nullable
    private static BlockPos openGround(ServerLevel level, int x, int z, int nearY) {
        if (!level.hasChunkAt(new BlockPos(x, nearY, z))) {
            return null;
        }
        if (!level.dimensionType().hasCeiling()) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            return y > level.getMinBuildHeight() ? new BlockPos(x, y, z) : null;
        }
        for (int y = nearY + 8; y >= nearY - 16; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.isEmptyBlock(pos.below()) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())
                    && level.isEmptyBlock(pos.above(2)) && level.getFluidState(pos.below()).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    /** One of the elements the biome at {@code pos} holds, at random, or null if it holds none. */
    @Nullable
    private static Element biomeElement(ServerLevel level, BlockPos pos, RandomSource random) {
        Holder<Biome> biome = level.getBiome(pos);
        List<Element> elements = new ArrayList<>();
        BIOMES.forEach((element, tag) -> {
            if (biome.is(tag)) {
                elements.add(element);
            }
        });
        return elements.isEmpty() ? null : elements.get(random.nextInt(elements.size()));
    }
}
