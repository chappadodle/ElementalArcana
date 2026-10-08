package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.api.FarIslesRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.List;

/**
 * Builds a Starfallen Observatory (see the Far Isles spec) in world coordinates round its middle
 * (cx, cz), its floor at cy: a round terrace of end stone bricks ringed with purpur, propped up with
 * end stone where the island falls away under it; four purpur pillars north, east, south and west
 * with end rods on top and a Star Lens at each one's foot, facing in; a dais of purpur in the middle
 * with the Astral Orrery on it; and round the dais, set in the floor, the chart: a tile of each
 * constellation's colour on the side where its lens must show it. The lenses start turned at random
 * (never all right). Every choice comes from the observatory's seed, so it's the same whichever
 * chunk is built first.
 */
public class ObservatoryPiece extends StructurePiece {
    /** The constellations' colours, as the chart tiles and the lenses show them (flame, wave, gale, stone). */
    public static final List<BlockState> TILES = List.of(Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState(),
            Blocks.WHITE_CONCRETE.defaultBlockState(), Blocks.LIME_CONCRETE.defaultBlockState());
    private static final int TERRACE = FarIslesRules.TERRACE;
    private static final int PILLAR = FarIslesRules.LENS_DISTANCE + 1;
    private static final int PILLAR_HEIGHT = 5;
    private static final int PROPS = 12;
    private static final int CLEAR = 9;

    private final long seed;
    private final int cx;
    private final int cy;
    private final int cz;

    public ObservatoryPiece(long seed, int cx, int cy, int cz) {
        super(ModEnd.OBSERVATORY_PIECE.get(), 0, new BoundingBox(cx - TERRACE - 1, cy - PROPS, cz - TERRACE - 1, cx + TERRACE + 1, cy + CLEAR, cz + TERRACE + 1));
        this.seed = seed;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        setOrientation(null);
    }

    public ObservatoryPiece(CompoundTag tag) {
        this(tag.getLong("seed"), tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("seed", seed);
        tag.putInt("x", cx);
        tag.putInt("y", cy);
        tag.putInt("z", cz);
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockState state, int x, int y, int z) {
        placeBlock(level, state, x, y, z, box);
    }

    /** Where the orrery stands: on the dais in the middle. */
    public BlockPos orrery() {
        return new BlockPos(cx, cy + 2, cz);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        terrace(level, box);
        pillars(level, box);
        dais(level, box);
        chart(level, box);
    }

    /** The terrace: end stone bricks inside, a purpur rim, propped up with end stone, the air above it cleared. */
    private void terrace(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - TERRACE; x <= cx + TERRACE; x++) {
            for (int z = cz - TERRACE; z <= cz + TERRACE; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > TERRACE + 0.5) {
                    continue;
                }
                put(level, box, d > TERRACE - 0.5 ? Blocks.PURPUR_BLOCK.defaultBlockState() : Blocks.END_STONE_BRICKS.defaultBlockState(), x, cy, z);
                for (int y = cy - 1; y >= cy - PROPS; y--) {
                    if (box.isInside(x, y, z) && !level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        break;
                    }
                    put(level, box, Blocks.END_STONE.defaultBlockState(), x, y, z);
                }
                for (int y = cy + 1; y <= cy + CLEAR; y++) {
                    put(level, box, Blocks.AIR.defaultBlockState(), x, y, z);
                }
            }
        }
    }

    /** Four pillars, north, east, south and west, end rods on top; a Star Lens at each one's foot, facing the orrery. */
    private void pillars(WorldGenLevel level, BoundingBox box) {
        int[] chart = AstralOrreryBlockEntity.chartAt(orrery());
        int[] lenses = new int[chart.length];
        RandomSource rng = RandomSource.create(seed);
        for (int i = 0; i < lenses.length; i++) {
            lenses[i] = rng.nextInt(FarIslesRules.CONSTELLATIONS.size());
        }
        if (FarIslesRules.matches(lenses, chart)) {
            lenses[0] = FarIslesRules.next(lenses[0]);
        }
        for (int i = 0; i < AstralOrreryBlockEntity.SIDES.size(); i++) {
            Direction side = AstralOrreryBlockEntity.SIDES.get(i);
            int px = cx + side.getStepX() * PILLAR;
            int pz = cz + side.getStepZ() * PILLAR;
            for (int y = cy + 1; y <= cy + PILLAR_HEIGHT; y++) {
                put(level, box, Blocks.PURPUR_PILLAR.defaultBlockState(), px, y, pz);
            }
            put(level, box, Blocks.PURPUR_BLOCK.defaultBlockState(), px, cy + PILLAR_HEIGHT + 1, pz);
            put(level, box, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP), px, cy + PILLAR_HEIGHT + 2, pz);
            int lx = cx + side.getStepX() * FarIslesRules.LENS_DISTANCE;
            int lz = cz + side.getStepZ() * FarIslesRules.LENS_DISTANCE;
            put(level, box, ModEnd.STAR_LENS.get().defaultBlockState().setValue(StarLensBlock.FACING, side.getOpposite())
                    .setValue(StarLensBlock.CONSTELLATION, lenses[i]), lx, cy + 1, lz);
        }
    }

    /** The dais (purpur, three by three) and the orrery on it. */
    private void dais(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz - 1; z <= cz + 1; z++) {
                put(level, box, Blocks.PURPUR_BLOCK.defaultBlockState(), x, cy + 1, z);
            }
        }
        put(level, box, ModEnd.ASTRAL_ORRERY.get().defaultBlockState(), cx, cy + 2, cz);
    }

    /** The chart in the floor round the dais: each side's tile, in the colour of the constellation its lens must show. */
    private void chart(WorldGenLevel level, BoundingBox box) {
        int[] chart = AstralOrreryBlockEntity.chartAt(orrery());
        for (int i = 0; i < AstralOrreryBlockEntity.SIDES.size(); i++) {
            Direction side = AstralOrreryBlockEntity.SIDES.get(i);
            BlockState tile = TILES.get(chart[i]);
            // A short bar of three, across the line from the dais to the lens.
            Direction across = side.getClockWise();
            for (int k = -1; k <= 1; k++) {
                put(level, box, tile, cx + side.getStepX() * 2 + across.getStepX() * k, cy, cz + side.getStepZ() * 2 + across.getStepZ() * k);
            }
            // And a line of the same colour from it out toward the lens.
            for (int step = 3; step < FarIslesRules.LENS_DISTANCE; step++) {
                put(level, box, tile, cx + side.getStepX() * step, cy, cz + side.getStepZ() * step);
            }
        }
    }
}
