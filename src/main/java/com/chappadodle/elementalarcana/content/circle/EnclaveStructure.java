package com.chappadodle.elementalarcana.content.circle;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * The Circle's Enclave (docs/superpowers/specs/2026-10-06-the-circle-enclave-design.md): a walled
 * compound round a white Spire, code-built (EnclavePiece) at the middle of its chunk. The ground is
 * sampled across its square; it's raised at the middle height of the samples (the land is shaped to
 * it), and never over water or on ground that falls away more than eight blocks.
 */
public class EnclaveStructure extends Structure {
    public static final MapCodec<EnclaveStructure> CODEC = simpleCodec(EnclaveStructure::new);
    private static final int[][] SAMPLES = {{0, 0}, {18, 0}, {-18, 0}, {0, 18}, {0, -18}, {14, 14}, {-14, 14}, {14, -14}, {-14, -14},
            {9, 0}, {-9, 0}, {0, 9}, {0, -9}};

    public EnclaveStructure(StructureSettings settings) {
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
        if (sorted[sorted.length - 1] - sorted[0] > 8) {
            return Optional.empty();
        }
        int floor = sorted[sorted.length / 2];
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, floor, z), builder -> builder.addPiece(new EnclavePiece(seed, x, floor, z))));
    }

    @Override
    public StructureType<?> type() {
        return ModCircle.ENCLAVE.get();
    }
}
