package com.chappadodle.elementalarcana.content.forge;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A Cinder Forge (docs/superpowers/specs/2026-10-06-cinder-forges-design.md): a forge-hall code-built
 * (CinderForgePiece) at the middle of its chunk, its floor at y 40 over the Nether's lava sea,
 * whatever the netherrack does there: the hall carves its own cavern and stands on its own plinth.
 */
public class CinderForgeStructure extends Structure {
    public static final MapCodec<CinderForgeStructure> CODEC = simpleCodec(CinderForgeStructure::new);
    /** The hall's floor. */
    static final int FLOOR = 40;

    public CinderForgeStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX();
        int z = chunk.getMiddleBlockZ();
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(new BlockPos(x, FLOOR, z), builder -> builder.addPiece(new CinderForgePiece(seed, x, FLOOR, z))));
    }

    @Override
    public StructureType<?> type() {
        return ModForge.CINDER_FORGE.get();
    }
}
