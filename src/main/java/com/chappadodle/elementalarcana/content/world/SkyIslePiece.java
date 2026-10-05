package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SkyIsleRules;
import com.chappadodle.elementalarcana.content.creature.ModCreatures;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Builds a Sky Isle (see its spec), Minecraft style: an upside-down mountain of earth and stone
 * (grass on top, ores and an amethyst glint in its rock, roots hanging from its underside), trees of
 * one wood, flowers, and a ruined pavilion of calcite pillars round a mosaic floor with a chest of sky
 * loot; wind wisps circle it. One isle in two has a smaller islet nearby. World coordinates, no
 * vanilla orientation; every chance is decided from the isle's seed or each block's place, so each
 * chunk's share comes out the same whichever is built first.
 */
public class SkyIslePiece extends StructurePiece {
    private static final ResourceKey<net.minecraft.world.level.storage.loot.LootTable> LOOT =
            ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/sky_isle"));
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final long seed;
    private final int cx;
    private final int top;
    private final int cz;
    private final int radius;
    private final int depth;
    private final boolean satellite;
    private final int satX;
    private final int satTop;
    private final int satZ;
    private final int wood;

    public SkyIslePiece(long seed, int cx, int top, int cz) {
        super(ModWorld.SKY_ISLE_PIECE.get(), 0, box(cx, top, cz));
        this.seed = seed;
        this.cx = cx;
        this.top = top;
        this.cz = cz;
        setOrientation(null);
        RandomSource random = RandomSource.create(seed);
        radius = SkyIsleRules.RADIUS_MIN + random.nextInt(SkyIsleRules.RADIUS_MAX - SkyIsleRules.RADIUS_MIN + 1);
        depth = SkyIsleRules.DEPTH_MIN + random.nextInt(SkyIsleRules.DEPTH_MAX - SkyIsleRules.DEPTH_MIN + 1);
        satellite = random.nextBoolean();
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = SkyIsleRules.SATELLITE_MIN + random.nextDouble() * (SkyIsleRules.SATELLITE_MAX - SkyIsleRules.SATELLITE_MIN);
        satX = cx + (int) Math.round(Math.cos(angle) * distance);
        satZ = cz + (int) Math.round(Math.sin(angle) * distance);
        satTop = top - 3 + random.nextInt(7);
        wood = random.nextInt(3);
    }

    public SkyIslePiece(CompoundTag tag) {
        this(tag.getLong("seed"), tag.getInt("x"), tag.getInt("top"), tag.getInt("z"));
    }

    private static BoundingBox box(int cx, int top, int cz) {
        return new BoundingBox(cx - SkyIsleRules.REACH, top - SkyIsleRules.DEPTH_MAX - 3, cz - SkyIsleRules.REACH,
                cx + SkyIsleRules.REACH, top + 12, cz + SkyIsleRules.REACH);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("seed", seed);
        tag.putInt("x", cx);
        tag.putInt("top", top);
        tag.putInt("z", cz);
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockState state, int x, int y, int z) {
        placeBlock(level, state, x, y, z, box);
    }

    /** A roll out of {@code sides} decided by a block's place (and this isle's seed). */
    private int roll(int x, int y, int z, int sides) {
        return Math.floorMod((int) ((Mth.getSeed(x, y, z) ^ seed) >>> 16), sides);
    }

    private BlockState rock(int x, int y, int z) {
        int pick = roll(x, y, z, 60);
        if (pick < 2) {
            return Blocks.COAL_ORE.defaultBlockState();
        }
        if (pick < 3) {
            return Blocks.IRON_ORE.defaultBlockState();
        }
        if (pick < 4) {
            return Blocks.COPPER_ORE.defaultBlockState();
        }
        if (pick < 5) {
            return Blocks.AMETHYST_BLOCK.defaultBlockState();
        }
        if (pick < 14) {
            return Blocks.ANDESITE.defaultBlockState();
        }
        if (pick < 20) {
            return Blocks.TUFF.defaultBlockState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        island(level, box, cx, top, cz, radius, depth);
        if (satellite) {
            island(level, box, satX, satTop, satZ, SkyIsleRules.SATELLITE_RADIUS, SkyIsleRules.SATELLITE_DEPTH);
        }
        pavilion(level, box, random);
        RandomSource trees = RandomSource.create(seed ^ 0x5EED);
        int count = 2 + trees.nextInt(2);
        for (int i = 0; i < count; i++) {
            double angle = trees.nextDouble() * Math.PI * 2;
            double distance = 3.6 + trees.nextDouble() * Math.max(0.5, radius - 4.6);
            tree(level, box, cx + (int) Math.round(Math.cos(angle) * distance), top, cz + (int) Math.round(Math.sin(angle) * distance),
                    4 + trees.nextInt(2));
        }
        if (satellite) {
            tree(level, box, satX, satTop, satZ, 4);
        }
        BlockPos guarded = new BlockPos(cx, top + 1, cz);
        if (box.isInside(guarded)) {
            guardians(level, guarded, trees);
        }
    }

    /** An isle's body: grass on top, earth under it, then rock tapering down to a point, roots hanging from it. */
    private void island(WorldGenLevel level, BoundingBox box, int x0, int y0, int z0, int r, int deep) {
        for (int x = x0 - r - 1; x <= x0 + r + 1; x++) {
            for (int z = z0 - r - 1; z <= z0 + r + 1; z++) {
                double distance = Math.hypot(x - x0, z - z0) + (roll(x, 0, z, 100) / 100.0 - 0.5) * 0.8;
                if (distance > r + 0.3) {
                    continue;
                }
                int bottom = 0;
                for (int below = 0; below <= deep; below++) {
                    double reach = SkyIsleRules.radiusAt(r, below, deep) + (roll(x, y0 - below, z, 10) / 10.0 - 0.5) * 0.6;
                    if (distance > reach) {
                        break;
                    }
                    BlockState state = below == 0 ? Blocks.GRASS_BLOCK.defaultBlockState()
                            : below <= 2 ? Blocks.DIRT.defaultBlockState() : rock(x, y0 - below, z);
                    put(level, box, state, x, y0 - below, z);
                    bottom = below;
                }
                if (bottom >= 3 && roll(x, y0 - bottom - 1, z, 4) == 0) {
                    put(level, box, Blocks.HANGING_ROOTS.defaultBlockState(), x, y0 - bottom - 1, z);
                }
                // Flowers and grass on top, away from the edge.
                if (distance < r - 0.6) {
                    int pick = roll(x, y0 + 1, z, 30);
                    BlockState plant = pick < 6 ? Blocks.SHORT_GRASS.defaultBlockState()
                            : pick == 6 ? Blocks.POPPY.defaultBlockState()
                            : pick == 7 ? Blocks.DANDELION.defaultBlockState()
                            : pick == 8 ? Blocks.CORNFLOWER.defaultBlockState()
                            : pick == 9 ? Blocks.OXEYE_DAISY.defaultBlockState() : null;
                    if (plant != null) {
                        put(level, box, plant, x, y0 + 1, z);
                    }
                }
            }
        }
    }

    /** The pavilion in the middle: a mosaic floor, a ring of calcite pillars (some broken), the chest. */
    private void pavilion(WorldGenLevel level, BoundingBox box, RandomSource random) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (dx * dx + dz * dz > 12) {
                    continue;
                }
                for (int y = 1; y <= 5; y++) {
                    put(level, box, AIR, cx + dx, top + y, cz + dz);
                }
                if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
                    BlockState tile = dx == 0 && dz == 0 ? Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState()
                            : (dx + dz) % 2 == 0 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.LIGHT_BLUE_TERRACOTTA.defaultBlockState();
                    put(level, box, tile, cx + dx, top, cz + dz);
                }
            }
        }
        int[][] pillars = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}};
        for (int[] p : pillars) {
            int x = cx + p[0];
            int z = cz + p[1];
            int height = roll(x, top, z, 3) == 0 ? 1 + roll(x, top + 1, z, 2) : 3;
            put(level, box, Blocks.SMOOTH_QUARTZ.defaultBlockState(), x, top, z);
            for (int y = 1; y <= height; y++) {
                put(level, box, Blocks.CALCITE.defaultBlockState(), x, top + y, z);
            }
            if (height == 3) {
                put(level, box, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), x, top + 4, z);
            } else if (roll(x, top, z + 1, 2) == 0) {
                // A broken piece fallen beside it.
                put(level, box, Blocks.CALCITE.defaultBlockState(), x + Integer.signum(p[0]), top + 1, z + Integer.signum(p[1]));
            }
        }
        BlockPos chest = new BlockPos(cx, top + 1, cz);
        if (box.isInside(chest)) {
            createChest(level, box, random, chest, LOOT, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        }
    }

    /** A small tree of the isle's wood: a trunk and a rounded crown of leaves that won't decay. */
    private void tree(WorldGenLevel level, BoundingBox box, int x, int ground, int z, int height) {
        BlockState log = switch (wood) {
            case 1 -> Blocks.BIRCH_LOG.defaultBlockState();
            case 2 -> Blocks.CHERRY_LOG.defaultBlockState();
            default -> Blocks.OAK_LOG.defaultBlockState();
        };
        BlockState leaves = (switch (wood) {
            case 1 -> Blocks.BIRCH_LEAVES;
            case 2 -> Blocks.CHERRY_LEAVES;
            default -> Blocks.OAK_LEAVES;
        }).defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int y = height - 2; y <= height + 1; y++) {
            int r = y >= height + 1 ? 1 : 2;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) == r && Math.abs(dz) == r && (r == 1 || roll(x + dx, ground + y, z + dz, 2) == 0)) {
                        continue;
                    }
                    put(level, box, leaves, x + dx, ground + y, z + dz);
                }
            }
        }
        put(level, box, Blocks.DIRT.defaultBlockState(), x, ground, z);
        for (int y = 1; y <= height; y++) {
            put(level, box, log, x, ground + y, z);
        }
    }

    /** Two or three wind wisps keep the isle, circling its pavilion. */
    private void guardians(WorldGenLevel level, BlockPos guarded, RandomSource random) {
        int count = 2 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            WispEntity wisp = ModCreatures.wisp(Element.WIND).create(level.getLevel());
            if (wisp == null) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2;
            wisp.moveTo(guarded.getX() + 0.5 + Math.cos(angle) * 3, guarded.getY() + 2 + random.nextInt(2),
                    guarded.getZ() + 0.5 + Math.sin(angle) * 3, random.nextFloat() * 360f, 0f);
            wisp.finalizeSpawn(level, level.getCurrentDifficultyAt(guarded), MobSpawnType.STRUCTURE, null);
            wisp.guard(guarded);
            wisp.setPersistenceRequired();
            level.addFreshEntityWithPassengers(wisp);
        }
    }
}
