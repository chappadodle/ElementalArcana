package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.FarIslesRules;
import com.chappadodle.elementalarcana.content.end.ModEnd;
import com.chappadodle.elementalarcana.api.NetherCreatureRules;
import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.api.ConfigRates;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.WildRules;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Where the Creatures of the Wild come from (see their specs): every 20 seconds each player rolls for
 * each kind, and one that comes up is born 20 to 40 blocks off in its own place, never two of a kind
 * within 48 blocks. Treants stand up among the trees, wraiths drift over the snow at night,
 * salamanders bask in the badlands, deserts and on the Nether's floor, harpies circle high over the
 * peaks by day, crawlers creep on cave floors far underground, and lurkers lie in swamp water; the
 * Nether's wraiths and hounds, and the Stargazers of the End's outer islands.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class WildSpawner {
    public static final TagKey<Biome> TREANT_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/treant"));
    public static final TagKey<Biome> WRAITH_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/frost_wraith"));
    public static final TagKey<Biome> SALAMANDER_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/salamander"));
    public static final TagKey<Biome> HARPY_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/gale_harpy"));
    public static final TagKey<Biome> LURKER_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/bog_lurker"));
    public static final TagKey<Biome> ASH_WRAITH_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/ash_wraith"));
    public static final TagKey<Biome> HOUND_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/cinder_hound"));
    public static final TagKey<Biome> STARGAZER_LANDS = TagKey.create(Registries.BIOME, ElementalArcana.id("wild/stargazer"));
    /** A wild creature's chance at the server's rate for them. */
    private static double wild(float chance) {
        return ConfigRates.scaled(chance, ArcanaServerConfig.WILD_CREATURES.get());
    }

    private static final int TRIES = 6;

    /** Every kind, for the spawner and the test command. */
    public enum Kind {
        TREANT, WRAITH, SALAMANDER, HARPY, CRAWLER, LURKER, ASH_WRAITH, HOUND, STARGAZER
    }

    private WildSpawner() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || (player.tickCount + player.getId() * 11) % WildRules.CHECK_TICKS != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!WispSpawner.canSpawn(level)) {
            return;
        }
        RandomSource random = player.getRandom();
        if (level.dimension() == Level.OVERWORLD) {
            if (random.nextFloat() < wild(WildRules.TREANT_CHANCE)) {
                trySpawn(level, Kind.TREANT, player.position(), random, false);
            }
            if (!level.isDay() && random.nextFloat() < wild(WildRules.WRAITH_CHANCE)) {
                trySpawn(level, Kind.WRAITH, player.position(), random, false);
            }
            if (level.isDay() && random.nextFloat() < wild(WildRules.HARPY_CHANCE)) {
                trySpawn(level, Kind.HARPY, player.position(), random, false);
            }
            if (player.getY() < WildRules.CRAWLER_MAX_Y + 16 && random.nextFloat() < wild(WildRules.CRAWLER_CHANCE)) {
                trySpawn(level, Kind.CRAWLER, player.position(), random, false);
            }
            if (random.nextFloat() < wild(WildRules.LURKER_CHANCE)) {
                trySpawn(level, Kind.LURKER, player.position(), random, false);
            }
        }
        if (level.dimension() == Level.NETHER) {
            if (random.nextFloat() < wild(NetherCreatureRules.ASH_WRAITH_CHANCE)) {
                trySpawn(level, Kind.ASH_WRAITH, player.position(), random, false);
            }
            if (random.nextFloat() < wild(NetherCreatureRules.HOUND_CHANCE)) {
                trySpawn(level, Kind.HOUND, player.position(), random, false);
            }
        }
        if (level.dimension() == Level.END && random.nextFloat() < wild(FarIslesRules.STARGAZER_CHANCE)) {
            trySpawn(level, Kind.STARGAZER, player.position(), random, false);
        }
        if ((level.dimension() == Level.OVERWORLD || level.dimension() == Level.NETHER) && random.nextFloat() < wild(WildRules.SALAMANDER_CHANCE)) {
            trySpawn(level, Kind.SALAMANDER, player.position(), random, false);
        }
    }

    /**
     * Tries to bring one of {@code kind} in 20 to 40 blocks from {@code around}. {@code forced} (the
     * test command) skips the land and spacing checks. Returns the creature, or null if there was no
     * place for it.
     */
    @Nullable
    public static Mob trySpawn(ServerLevel level, Kind kind, Vec3 around, RandomSource random, boolean forced) {
        for (int i = 0; i < TRIES; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = WildRules.MIN_DISTANCE + random.nextDouble() * (WildRules.MAX_DISTANCE - WildRules.MIN_DISTANCE);
            int x = Mth.floor(around.x + Math.cos(angle) * distance);
            int z = Mth.floor(around.z + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            BlockPos pos = place(level, kind, x, Mth.floor(around.y), z);
            if (pos == null || !forced && !inItsLand(level, kind, pos)) {
                continue;
            }
            if (!forced && !level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(WildRules.SPACING),
                    mob -> mob.getType() == type(kind)).isEmpty()) {
                continue;
            }
            BlockPos at = switch (kind) {
                case WRAITH -> pos.above(3 + random.nextInt(3));
                case ASH_WRAITH -> pos.above(2 + random.nextInt(2));
                case STARGAZER -> pos.above(1 + random.nextInt(3));
                case HARPY -> pos.above(10 + random.nextInt(7));
                default -> pos;
            };
            Mob mob = type(kind).spawn(level, at, forced ? MobSpawnType.COMMAND : MobSpawnType.NATURAL);
            if (mob != null) {
                if (kind == Kind.HOUND) {
                    packMates(level, at, random, forced);
                }
                return mob;
            }
        }
        return null;
    }

    /** A hound's pack: one to three more, close by (where there's room). */
    private static void packMates(ServerLevel level, BlockPos first, RandomSource random, boolean forced) {
        int more = NetherCreatureRules.packSize(random.nextDouble()) - 1;
        for (int i = 0, tries = 0; i < more && tries < 12; tries++) {
            BlockPos pos = place(level, Kind.HOUND, first.getX() + random.nextInt(5) - 2, first.getY(), first.getZ() + random.nextInt(5) - 2);
            if (pos != null && type(Kind.HOUND).spawn(level, pos, forced ? MobSpawnType.COMMAND : MobSpawnType.NATURAL) != null) {
                i++;
            }
        }
    }

    public static EntityType<? extends Mob> type(Kind kind) {
        return switch (kind) {
            case TREANT -> ModWild.TREANT.get();
            case WRAITH -> ModWild.FROST_WRAITH.get();
            case SALAMANDER -> ModWild.SALAMANDER.get();
            case HARPY -> ModWild.HARPY.get();
            case CRAWLER -> ModWild.CRAWLER.get();
            case LURKER -> ModWild.LURKER.get();
            case ASH_WRAITH -> ModWild.ASH_WRAITH.get();
            case HOUND -> ModWild.CINDER_HOUND.get();
            case STARGAZER -> ModEnd.STARGAZER.get();
        };
    }

    private static boolean inItsLand(ServerLevel level, Kind kind, BlockPos pos) {
        return switch (kind) {
            case TREANT -> level.getBiome(pos).is(TREANT_LANDS);
            case WRAITH -> level.getBiome(pos).is(WRAITH_LANDS);
            case SALAMANDER -> level.getBiome(pos).is(SALAMANDER_LANDS);
            case HARPY -> level.getBiome(pos).is(HARPY_LANDS);
            case LURKER -> level.getBiome(pos).is(LURKER_LANDS);
            case ASH_WRAITH -> level.getBiome(pos).is(ASH_WRAITH_LANDS);
            case HOUND -> level.getBiome(pos).is(HOUND_LANDS);
            case STARGAZER -> level.getBiome(pos).is(STARGAZER_LANDS);
            // Deep in the dark: under the crawler's height and out of the light.
            case CRAWLER -> pos.getY() < WildRules.CRAWLER_MAX_Y && level.getBrightness(LightLayer.SKY, pos) == 0
                    && level.getBrightness(LightLayer.BLOCK, pos) <= 7;
        };
    }

    /** Where at (x, z) {@code kind} could be born, near {@code nearY}, or null if nowhere. */
    @Nullable
    private static BlockPos place(ServerLevel level, Kind kind, int x, int nearY, int z) {
        if (kind == Kind.CRAWLER || level.dimensionType().hasCeiling()) {
            // Under a roof (caves, the Nether): the first floor with room above it, down from a little over nearY.
            for (int y = Math.min(nearY + 8, level.getMaxBuildHeight() - 3); y > Math.max(nearY - 24, level.getMinBuildHeight()); y--) {
                BlockPos pos = new BlockPos(x, y, z);
                if (fits(level, kind, pos)) {
                    return pos;
                }
            }
            return null;
        }
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (kind == Kind.LURKER) {
            // In the water, just under its surface.
            BlockPos water = top.below();
            return fits(level, kind, water) ? water : null;
        }
        return fits(level, kind, top) ? top : null;
    }

    /** Sturdy dry ground (a forest floor for a treant) with room above it, or for a lurker, deep enough water. */
    private static boolean fits(ServerLevel level, Kind kind, BlockPos pos) {
        if (kind == Kind.LURKER) {
            return level.getFluidState(pos).is(FluidTags.WATER) && level.getFluidState(pos.below()).is(FluidTags.WATER)
                    && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        }
        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isFaceSturdy(level, pos.below(), Direction.UP) || !ground.getFluidState().isEmpty()) {
            return false;
        }
        if (kind == Kind.TREANT && !ground.is(BlockTags.DIRT)) {
            return false;
        }
        int radius = kind == Kind.TREANT || kind == Kind.CRAWLER ? 1 : 0;
        int height = switch (kind) {
            case TREANT -> 3;
            case WRAITH, HARPY -> 5;
            case ASH_WRAITH, STARGAZER -> 4;
            case SALAMANDER, CRAWLER, LURKER, HOUND -> 1;
        };
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, 0, -radius), pos.offset(radius, height - 1, radius))) {
            BlockState state = level.getBlockState(p);
            if (!state.getCollisionShape(level, p).isEmpty() || !state.getFluidState().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
