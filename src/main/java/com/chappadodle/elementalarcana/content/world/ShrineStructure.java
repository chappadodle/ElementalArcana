package com.chappadodle.elementalarcana.content.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 * An elemental shrine (see ShrinePiece for what it looks like). It needs fairly level ground: the
 * corners of its 11x11 footprint may differ by at most 6 blocks, and its platform sits at their
 * average height. Only Water shrines stand in water.
 */
public class ShrineStructure extends Structure {
    public static final MapCodec<ShrineStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ShrineKind.CODEC.fieldOf("kind").forGetter(structure -> structure.kind)
    ).apply(instance, ShrineStructure::new));
    private static final int SIZE = 11;
    private static final int MAX_SLOPE = 6;

    private final ShrineKind kind;

    public ShrineStructure(StructureSettings settings, ShrineKind kind) {
        super(settings);
        this.kind = kind;
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
        boolean wet = false;
        for (int[] corner : new int[][]{{0, 0}, {SIZE - 1, 0}, {0, SIZE - 1}, {SIZE - 1, SIZE - 1}, {SIZE / 2, SIZE / 2}}) {
            int surface = generator.getFirstOccupiedHeight(x + corner[0], z + corner[1], Heightmap.Types.WORLD_SURFACE_WG, height, random);
            int floor = generator.getFirstOccupiedHeight(x + corner[0], z + corner[1], Heightmap.Types.OCEAN_FLOOR_WG, height, random);
            wet |= surface > floor;
            lowest = Math.min(lowest, surface);
            highest = Math.max(highest, surface);
            sum += surface;
        }
        if (highest - lowest > MAX_SLOPE || wet && kind != ShrineKind.WATER) {
            return Optional.empty();
        }
        // Level with the average ground (the platform takes the place of the top block): terrain
        // blending fills below the platform and carves above it.
        int groundY = Math.round(sum / 5f);
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        BlockPos origin = new BlockPos(x, groundY, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new ShrinePiece(kind, x, groundY, z, facing))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorld.SHRINE.get();
    }
}
