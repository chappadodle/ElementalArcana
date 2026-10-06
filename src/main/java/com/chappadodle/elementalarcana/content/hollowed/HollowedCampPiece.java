package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds a Hollowed camp (see the Hollowed spec): a round clearing 21 blocks across at the camp's
 * height (the land is shaped to it as it generates: the structure's terrain adaptation, beard_thin,
 * fills under the piece's box and carves over it), cleared above and floored with blighted ground
 * (soul soil, gravel and packed mud, which no sapling, grass or flower takes to, so trees placed
 * after it keep out); the Hunger Obelisk at the middle on a pillar of polished blackstone and crying obsidian, a
 * purple candle at each corner of its base; three tents of black wool opening on it (the north one
 * the biggest, with the chest at its back); three soul campfires. World coordinates, no vanilla
 * orientation; every choice comes from the camp's seed and each block's place, so it comes out the
 * same whichever chunk is built first.
 */
public class HollowedCampPiece extends StructurePiece {
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/hollowed_camp"));
    private static final int RADIUS = 10;
    private static final int CLEAR_HEIGHT = 7;

    private final long seed;
    private final int cx;
    private final int cy;
    private final int cz;

    public HollowedCampPiece(long seed, int cx, int cy, int cz) {
        // The box's floor is the camp's floor: the terrain adaptation shapes the land to it.
        super(ModHollowed.CAMP_PIECE.get(), 0, new BoundingBox(cx - RADIUS - 1, cy - 1, cz - RADIUS - 1, cx + RADIUS + 1, cy + 13, cz + RADIUS + 1));
        this.seed = seed;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        setOrientation(null);
    }

    public HollowedCampPiece(CompoundTag tag) {
        this(tag.getLong("seed"), tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("seed", seed);
        tag.putInt("x", cx);
        tag.putInt("y", cy);
        tag.putInt("z", cz);
    }

    /** A roll out of {@code sides} decided by a block's place (and this camp's seed). */
    private int roll(int x, int z, int sides) {
        return Math.floorMod((int) ((Mth.getSeed(x, 0, z) ^ seed) >>> 16), sides);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        clearing(level, box);
        obelisk(level, box);
        // The north tent, the biggest, its chest at the back; then east and west, all opening on the obelisk.
        tent(level, box, cx, cz - 5, 0, -1, true);
        tent(level, box, cx + 5, cz + 2, 1, 0, false);
        tent(level, box, cx - 5, cz + 2, -1, 0, false);
        for (int[] fire : new int[][]{{4, -3}, {-4, -3}, {0, 6}}) {
            placeBlock(level, Blocks.BLACKSTONE.defaultBlockState(), cx + fire[0], cy - 1, cz + fire[1], box);
            placeBlock(level, Blocks.SOUL_CAMPFIRE.defaultBlockState(), cx + fire[0], cy, cz + fire[1], box);
        }
    }

    /** The clearing: cleared above the camp's floor (and trees' tops a little higher), floored with blighted ground. */
    private void clearing(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - RADIUS; x <= cx + RADIUS; x++) {
            for (int z = cz - RADIUS; z <= cz + RADIUS; z++) {
                double distance = Math.hypot(x - cx, z - cz);
                if (distance > RADIUS + 0.5 || !box.isInside(x, cy, z)) {
                    continue;
                }
                for (int y = cy; y <= cy + CLEAR_HEIGHT + 5; y++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (y <= cy + CLEAR_HEIGHT || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
                        placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                    }
                }
                int pick = roll(x, z, 10);
                BlockState floor = distance < 3 || pick < 5 ? Blocks.SOUL_SOIL.defaultBlockState()
                        : pick < 7 ? Blocks.GRAVEL.defaultBlockState() : Blocks.PACKED_MUD.defaultBlockState();
                placeBlock(level, floor, x, cy - 1, z, box);
            }
        }
    }

    /** The obelisk on its pillar, on a floor of blackstone bricks, a candle at each corner. */
    private void obelisk(WorldGenLevel level, BoundingBox box) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placeBlock(level, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), cx + dx, cy - 1, cz + dz, box);
                if (dx != 0 && dz != 0) {
                    placeBlock(level, Blocks.PURPLE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 3).setValue(CandleBlock.LIT, true),
                            cx + dx, cy, cz + dz, box);
                }
            }
        }
        placeBlock(level, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), cx, cy, cz, box);
        placeBlock(level, Blocks.CRYING_OBSIDIAN.defaultBlockState(), cx, cy + 1, cz, box);
        placeBlock(level, ModHollowed.OBELISK.get().defaultBlockState(), cx, cy + 2, cz, box);
    }

    /**
     * A tent of black wool, five wide and five deep, its open front at (fx, fz) facing the camp's
     * middle and its back {@code (bx, bz)} away: walls, then a sloping roof, then a purple ridge, the
     * back closed, a spruce pole standing out of the ridge at each end. The big one has a bedroll and
     * the chest.
     */
    private void tent(WorldGenLevel level, BoundingBox box, int fx, int fz, int bx, int bz, boolean big) {
        // Across the tent: perpendicular to its depth.
        int lx = -bz;
        int lz = bx;
        BlockState wool = Blocks.BLACK_WOOL.defaultBlockState();
        for (int depth = 0; depth <= 4; depth++) {
            for (int side = -2; side <= 2; side++) {
                int x = fx + bx * depth + lx * side;
                int z = fz + bz * depth + lz * side;
                int across = Math.abs(side);
                if (across == 2) {
                    placeBlock(level, wool, x, cy, z, box);
                } else if (across == 1) {
                    placeBlock(level, wool, x, cy + 1, z, box);
                    if (depth == 4) {
                        placeBlock(level, wool, x, cy, z, box);
                    }
                } else {
                    placeBlock(level, Blocks.PURPLE_WOOL.defaultBlockState(), x, cy + 2, z, box);
                    if (depth == 4) {
                        placeBlock(level, wool, x, cy, z, box);
                        placeBlock(level, wool, x, cy + 1, z, box);
                    }
                    if (depth == 0 || depth == 4) {
                        placeBlock(level, Blocks.SPRUCE_FENCE.defaultBlockState(), x, cy + 3, z, box);
                    }
                }
            }
        }
        if (big) {
            for (int depth = 1; depth <= 2; depth++) {
                placeBlock(level, Blocks.PURPLE_CARPET.defaultBlockState(), fx + bx * depth - lx, cy, fz + bz * depth - lz, box);
            }
            createChest(level, box, RandomSource.create(seed), fx + bx * 3, cy, fz + bz * 3, LOOT);
        }
    }
}
