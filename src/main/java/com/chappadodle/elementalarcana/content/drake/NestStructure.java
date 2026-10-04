package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.DrakeRules;
import com.chappadodle.elementalarcana.api.Element;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A drake's nest (see the Drakes spec): on high ground (NEST_MIN_Y and up), dry and fairly level for
 * its 13 blocks, in the lands of a drake that nests (fire, frost, storm, gale), never within
 * MIN_DISTANCE of the world's centre.
 */
public class NestStructure extends Structure {
    public static final MapCodec<NestStructure> CODEC = simpleCodec(NestStructure::new);
    public static final int NEST_MIN_Y = 100;
    private static final int SIZE = 13;
    private static final int MAX_SLOPE = 6;

    public NestStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int mx = chunk.getMiddleBlockX();
        int mz = chunk.getMiddleBlockZ();
        if (Math.hypot(mx, mz) < DrakeRules.MIN_DISTANCE) {
            return Optional.empty();
        }
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState random = context.randomState();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        for (int[] corner : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}, {0, 0}}) {
            int x = mx + corner[0];
            int z = mz + corner[1];
            int surface = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, height, random);
            if (surface > generator.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, height, random)) {
                return Optional.empty();
            }
            lowest = Math.min(lowest, surface);
            highest = Math.max(highest, surface);
        }
        int ground = generator.getFirstOccupiedHeight(mx, mz, Heightmap.Types.WORLD_SURFACE_WG, height, random);
        if (ground < NEST_MIN_Y || highest - lowest > MAX_SLOPE) {
            return Optional.empty();
        }
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(mx), QuartPos.fromBlock(ground), QuartPos.fromBlock(mz),
                random.sampler());
        Element element = DrakeSpawner.landsOf(biome, true);
        if (element == null) {
            return Optional.empty();
        }
        return Optional.of(new GenerationStub(new BlockPos(mx, ground, mz),
                builder -> builder.addPiece(new NestPiece(element, mx - SIZE / 2, ground, mz - SIZE / 2))));
    }

    @Override
    public StructureType<?> type() {
        return ModDrakes.NEST.get();
    }
}
