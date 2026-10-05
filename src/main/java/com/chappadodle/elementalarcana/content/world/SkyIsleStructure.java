package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.api.SkyIsleRules;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Sky Isle (see its spec): an island floating 70 to 100 blocks over the highest ground under it
 * (never below y 170), over land or sea. The isle itself is code-built (SkyIslePiece).
 */
public class SkyIsleStructure extends Structure {
    public static final MapCodec<SkyIsleStructure> CODEC = simpleCodec(SkyIsleStructure::new);
    private static final int[][] SAMPLES = {{0, 0}, {10, 0}, {-10, 0}, {0, 10}, {0, -10}, {7, 7}, {-7, 7}, {7, -7}, {-7, -7}};

    public SkyIsleStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int mx = chunk.getMiddleBlockX();
        int mz = chunk.getMiddleBlockZ();
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState random = context.randomState();
        int ground = height.getMinBuildHeight();
        for (int[] sample : SAMPLES) {
            ground = Math.max(ground, generator.getFirstOccupiedHeight(mx + sample[0], mz + sample[1], Heightmap.Types.WORLD_SURFACE_WG, height, random));
        }
        WorldgenRandom rng = context.random();
        int top = SkyIsleRules.topY(ground, rng.nextDouble(), height.getMaxBuildHeight());
        if (top - SkyIsleRules.DEPTH_MAX < ground + 24) {
            // Mountains too near the ceiling: no room to float.
            return Optional.empty();
        }
        long seed = rng.nextLong();
        return Optional.of(new GenerationStub(new BlockPos(mx, top, mz), builder -> builder.addPiece(new SkyIslePiece(seed, mx, top, mz))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorld.SKY_ISLE.get();
    }
}
