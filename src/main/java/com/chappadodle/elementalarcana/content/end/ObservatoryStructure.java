package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.api.FarIslesRules;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Starfallen Observatory (docs/superpowers/specs/2026-10-08-the-far-isles-design.md): a terrace on
 * the top of an outer End island, at least 1000 blocks from the centre, where there's island under
 * all of it. Code-built (ObservatoryPiece) at the middle of its chunk.
 */
public class ObservatoryStructure extends Structure {
    public static final MapCodec<ObservatoryStructure> CODEC = simpleCodec(ObservatoryStructure::new);
    private static final int[][] SAMPLES = {{0, 0}, {7, 0}, {-7, 0}, {0, 7}, {0, -7}};
    /** Land under each sample at least this high (the End's islands float well over its floor). */
    private static final int MIN_GROUND = 30;

    public ObservatoryStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        if (!FarIslesRules.farEnough(x, z)) {
            return Optional.empty();
        }
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState random = context.randomState();
        int top = Integer.MIN_VALUE;
        for (int[] sample : SAMPLES) {
            int ground = generator.getFirstOccupiedHeight(x + sample[0], z + sample[1], Heightmap.Types.WORLD_SURFACE_WG, height, random);
            if (ground < MIN_GROUND) {
                return Optional.empty();
            }
            top = Math.max(top, ground);
        }
        int floor = top;
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, floor, z), builder -> builder.addPiece(new ObservatoryPiece(seed, x, floor, z))));
    }

    @Override
    public StructureType<?> type() {
        return ModEnd.OBSERVATORY.get();
    }
}
