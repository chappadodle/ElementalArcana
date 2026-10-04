package com.chappadodle.elementalarcana.content.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A ruin (see RuinPiece and the Ruins spec): one of three old layouts, on fairly level dry land.
 * The corners of its 9x9 footprint may differ by at most 4 blocks; it sits at their average height,
 * never below sea level or in water.
 */
public class RuinStructure extends Structure {
    public static final MapCodec<RuinStructure> CODEC = simpleCodec(RuinStructure::new);
    private static final int SIZE = 9;
    private static final int MAX_SLOPE = 4;

    public RuinStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX() - SIZE / 2;
        int z = chunk.getMiddleBlockZ() - SIZE / 2;
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState random = context.randomState();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int sum = 0;
        for (int[] corner : new int[][]{{0, 0}, {SIZE - 1, 0}, {0, SIZE - 1}, {SIZE - 1, SIZE - 1}, {SIZE / 2, SIZE / 2}}) {
            int surface = generator.getFirstOccupiedHeight(x + corner[0], z + corner[1], Heightmap.Types.WORLD_SURFACE_WG, height, random);
            int floor = generator.getFirstOccupiedHeight(x + corner[0], z + corner[1], Heightmap.Types.OCEAN_FLOOR_WG, height, random);
            if (surface > floor) {
                return Optional.empty();
            }
            lowest = Math.min(lowest, surface);
            highest = Math.max(highest, surface);
            sum += surface;
        }
        int groundY = Math.round(sum / 5f);
        if (highest - lowest > MAX_SLOPE || groundY < generator.getSeaLevel()) {
            return Optional.empty();
        }
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        RuinPiece.Layout layout = RuinPiece.Layout.values()[context.random().nextInt(RuinPiece.Layout.values().length)];
        return Optional.of(new GenerationStub(new BlockPos(x, groundY, z),
                builder -> builder.addPiece(new RuinPiece(layout, x, groundY, z, facing))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorld.RUIN.get();
    }
}
