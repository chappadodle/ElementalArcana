package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.api.WispRingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Builds a Wisp Ring (see its spec), as fairy rings grow: a band of dark podzol round the middle,
 * three blocks out, with red and brown mushrooms on it and flowers between them; short grass cleared
 * inside; a mossy stone at the middle. Each block follows the ground where it stands. World
 * coordinates, no vanilla orientation; every choice comes from the ring's seed and each block's
 * place, so it comes out the same whichever chunk is built first.
 */
public class WispRingPiece extends StructurePiece {
    private static final BlockState[] FLOWERS = {Blocks.POPPY.defaultBlockState(), Blocks.CORNFLOWER.defaultBlockState(),
            Blocks.OXEYE_DAISY.defaultBlockState(), Blocks.ALLIUM.defaultBlockState(), Blocks.AZURE_BLUET.defaultBlockState()};

    private final long seed;
    private final int cx;
    private final int cy;
    private final int cz;

    public WispRingPiece(long seed, int cx, int cy, int cz) {
        super(ModWorld.WISP_RING_PIECE.get(), 0, new BoundingBox(cx - WispRingRules.RADIUS - 1, cy - 6, cz - WispRingRules.RADIUS - 1,
                cx + WispRingRules.RADIUS + 1, cy + 6, cz + WispRingRules.RADIUS + 1));
        this.seed = seed;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        setOrientation(null);
    }

    public WispRingPiece(CompoundTag tag) {
        this(tag.getLong("seed"), tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("seed", seed);
        tag.putInt("x", cx);
        tag.putInt("y", cy);
        tag.putInt("z", cz);
    }

    /** A roll out of {@code sides} decided by a block's place (and this ring's seed). */
    private int roll(int x, int z, int sides) {
        return Math.floorMod((int) ((Mth.getSeed(x, 0, z) ^ seed) >>> 16), sides);
    }

    /** The first open block over the ground at (x, z), within a few blocks of the ring's height; MIN_VALUE if none. */
    private int groundTop(WorldGenLevel level, int x, int z) {
        for (int y = cy + 4; y >= cy - 5; y--) {
            BlockState state = level.getBlockState(new BlockPos(x, y, z));
            if (state.is(BlockTags.DIRT) || state.is(Blocks.GRASS_BLOCK) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Blocks.SAND)) {
                return y + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        int reach = WispRingRules.RADIUS + 1;
        for (int x = cx - reach; x <= cx + reach; x++) {
            for (int z = cz - reach; z <= cz + reach; z++) {
                if (!box.isInside(x, cy, z)) {
                    continue;
                }
                double distance = Math.hypot(x - cx, z - cz);
                if (distance > WispRingRules.RADIUS + 0.7) {
                    continue;
                }
                // The ground here, found by looking down (worldgen's heightmaps are gone once a chunk is done,
                // as when a ring is placed by command): the first block of earth, the air over it.
                int top = groundTop(level, x, z);
                if (top == Integer.MIN_VALUE) {
                    continue;
                }
                if (x == cx && z == cz) {
                    // The stone at the middle stands on whatever ground is there.
                    placeBlock(level, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), x, top, z, box);
                    placeBlock(level, Blocks.MOSS_CARPET.defaultBlockState(), x, top + 1, z, box);
                    continue;
                }
                BlockState ground = level.getBlockState(new BlockPos(x, top - 1, z));
                if (!ground.is(Blocks.GRASS_BLOCK) && !ground.is(Blocks.DIRT) && !ground.is(Blocks.PODZOL) && !ground.is(Blocks.COARSE_DIRT)) {
                    continue;
                }
                BlockPos above = new BlockPos(x, top, z);
                if (distance >= WispRingRules.RADIUS - 0.5) {
                    // The ring: podzol (mushrooms keep to it, in any light), a mushroom or a flower on it.
                    placeBlock(level, Blocks.PODZOL.defaultBlockState(), x, top - 1, z, box);
                    int pick = roll(x, z, 10);
                    BlockState growth = pick < 4 ? Blocks.RED_MUSHROOM.defaultBlockState()
                            : pick < 7 ? Blocks.BROWN_MUSHROOM.defaultBlockState()
                            : FLOWERS[roll(z, x, FLOWERS.length)];
                    placeBlock(level, growth, x, top, z, box);
                } else if (level.getBlockState(above).is(Blocks.SHORT_GRASS) || level.getBlockState(above).is(Blocks.TALL_GRASS)) {
                    // Inside: kept clear, the grass trodden short.
                    placeBlock(level, Blocks.AIR.defaultBlockState(), x, top, z, box);
                }
            }
        }
    }
}
