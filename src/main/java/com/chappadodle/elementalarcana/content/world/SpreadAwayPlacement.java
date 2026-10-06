package com.chappadodle.elementalarcana.content.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * Random spread, kept clear of other structures: a structure of this set doesn't start within
 * {@code chunk_count} chunks of where any set in {@code sets} could start (vanilla's exclusion_zone
 * keeps clear of only one). Written as {@code "type": "elementalarcana:spread_away"} with the usual
 * random_spread fields (but no spread_type: always linear) and {@code "keep_away": {"sets": [...],
 * "chunk_count": n}}. The sets kept clear of must not keep clear of this one in turn.
 */
public class SpreadAwayPlacement extends RandomSpreadStructurePlacement {
    /** The structure sets to keep clear of, and by how many chunks. */
    public record KeepAway(HolderSet<StructureSet> sets, int chunkCount) {
        public static final Codec<KeepAway> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.homogeneousList(Registries.STRUCTURE_SET).fieldOf("sets").forGetter(KeepAway::sets),
                Codec.intRange(1, 16).fieldOf("chunk_count").forGetter(KeepAway::chunkCount)).apply(instance, KeepAway::new));
    }

    public static final MapCodec<SpreadAwayPlacement> CODEC = RecordCodecBuilder.<SpreadAwayPlacement>mapCodec(instance -> placementCodec(instance)
            .and(instance.group(
                    Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
                    Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
                    KeepAway.CODEC.fieldOf("keep_away").forGetter(SpreadAwayPlacement::keepAway)))
            .apply(instance, SpreadAwayPlacement::new));

    private final KeepAway keepAway;

    public SpreadAwayPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency, int salt,
                               Optional<ExclusionZone> exclusionZone, int spacing, int separation, KeepAway keepAway) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, RandomSpreadType.LINEAR);
        this.keepAway = keepAway;
    }

    public KeepAway keepAway() {
        return keepAway;
    }

    @Override
    public boolean applyInteractionsWithOtherStructures(ChunkGeneratorStructureState state, int x, int z) {
        for (Holder<StructureSet> set : keepAway.sets()) {
            if (state.hasStructureChunkInRange(set, x, z, keepAway.chunkCount())) {
                return false;
            }
        }
        return super.applyInteractionsWithOtherStructures(state, x, z);
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModWorld.SPREAD_AWAY.get();
    }
}
