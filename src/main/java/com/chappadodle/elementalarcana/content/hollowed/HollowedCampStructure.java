package com.chappadodle.elementalarcana.content.hollowed;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * A Hollowed camp (docs/superpowers/specs/2026-10-06-the-hollowed-design.md): a clearing with tents
 * round a Hunger Obelisk, code-built (HollowedCampPiece) at the middle of its chunk. The ground is
 * sampled across the clearing; the camp is pitched at the middle height of the samples, and never on
 * water or on ground that falls away more than six blocks.
 */
public class HollowedCampStructure extends Structure {
    public static final MapCodec<HollowedCampStructure> CODEC = simpleCodec(HollowedCampStructure::new);
    private static final int[][] SAMPLES = {{0, 0}, {8, 0}, {-8, 0}, {0, 8}, {0, -8}, {6, 6}, {-6, 6}, {6, -6}, {-6, -6}};

    public HollowedCampStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int[] heights = new int[SAMPLES.length];
        for (int i = 0; i < SAMPLES.length; i++) {
            int sx = x + SAMPLES[i][0];
            int sz = z + SAMPLES[i][1];
            int ground = context.chunkGenerator().getFirstOccupiedHeight(sx, sz, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
                    context.randomState());
            int surface = context.chunkGenerator().getFirstOccupiedHeight(sx, sz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                    context.randomState());
            if (surface != ground) {
                return Optional.empty();
            }
            heights[i] = ground;
        }
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        if (sorted[sorted.length - 1] - sorted[0] > 6) {
            return Optional.empty();
        }
        int floor = sorted[sorted.length / 2];
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, floor, z), builder -> builder.addPiece(new HollowedCampPiece(seed, x, floor, z))));
    }

    @Override
    public StructureType<?> type() {
        return ModHollowed.CAMP.get();
    }
}
