package com.chappadodle.elementalarcana.content.world;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * A Wisp Ring (docs/superpowers/specs/2026-10-06-wisp-rings-design.md): a ring of mushrooms and
 * flowers on the ground, where wisps dance at night. Placed at the middle of its chunk, on the
 * surface (the ring itself is code-built: WispRingPiece). Never in water.
 */
public class WispRingStructure extends Structure {
    public static final MapCodec<WispRingStructure> CODEC = simpleCodec(WispRingStructure::new);

    public WispRingStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        int ground = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
                context.randomState());
        int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
                context.randomState());
        if (surface != ground) {
            // Water over the ground: no ring.
            return Optional.empty();
        }
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, ground, z), builder -> builder.addPiece(new WispRingPiece(seed, x, ground, z))));
    }

    @Override
    public StructureType<?> type() {
        return ModWorld.WISP_RING.get();
    }
}
