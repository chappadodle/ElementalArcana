package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
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

import java.util.Locale;
import java.util.Optional;

/**
 * A Sovereign's sanctum (see SanctumPiece for the building), one structure per element. It needs
 * fairly level ground at or above sea level (the probes across its 37-block floor within 10 blocks
 * of each other, 16 for the mountain Aerie), and dry ground, except the Tidehall, which may stand
 * in water up to 8 deep and is built up out of it. None stands within 1500 blocks of the world's
 * centre.
 */
public class SanctumStructure extends Structure {
    private static final Codec<Element> ELEMENT = Codec.STRING.comapFlatMap(name -> {
        for (Element element : SovereignRules.ELEMENTS) {
            if (element.name().equalsIgnoreCase(name)) {
                return DataResult.success(element);
            }
        }
        return DataResult.error(() -> "No sanctum for element: " + name);
    }, element -> element.name().toLowerCase(Locale.ROOT));
    public static final MapCodec<SanctumStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            ELEMENT.fieldOf("element").forGetter(structure -> structure.element)
    ).apply(instance, SanctumStructure::new));
    private static final int MIN_DISTANCE = 1500;
    private static final int PROBE = 18;
    private static final int MAX_SLOPE = 10;
    private static final int MAX_SLOPE_MOUNTAIN = 16;
    private static final int MAX_DEPTH = 8;

    private final Element element;

    public SanctumStructure(StructureSettings settings, Element element) {
        super(settings);
        this.element = element;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int cx = chunk.getMiddleBlockX();
        int cz = chunk.getMiddleBlockZ();
        if (Math.hypot(cx, cz) < MIN_DISTANCE) {
            return Optional.empty();
        }
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState random = context.randomState();
        int sea = generator.getSeaLevel();
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int sum = 0;
        int[][] probes = {{0, 0}, {-PROBE, -PROBE}, {PROBE, -PROBE}, {-PROBE, PROBE}, {PROBE, PROBE},
                {0, -PROBE}, {0, PROBE}, {-PROBE, 0}, {PROBE, 0}};
        for (int[] probe : probes) {
            int surface = generator.getFirstOccupiedHeight(cx + probe[0], cz + probe[1], Heightmap.Types.WORLD_SURFACE_WG, height, random);
            int floor = generator.getFirstOccupiedHeight(cx + probe[0], cz + probe[1], Heightmap.Types.OCEAN_FLOOR_WG, height, random);
            if (surface > floor && (element != Element.WATER || floor < sea - MAX_DEPTH)) {
                return Optional.empty();
            }
            lowest = Math.min(lowest, surface);
            highest = Math.max(highest, surface);
            sum += surface;
        }
        int groundY = Math.round(sum / (float) probes.length);
        int maxSlope = element == Element.WIND ? MAX_SLOPE_MOUNTAIN : MAX_SLOPE;
        if (highest - lowest > maxSlope || groundY < sea) {
            return Optional.empty();
        }
        if (element == Element.WATER) {
            groundY = Math.max(groundY, sea + 1);
        }
        int y = groundY;
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz),
                builder -> builder.addPiece(new SanctumPiece(element, cx - SanctumPiece.MID, y, cz - SanctumPiece.MID, facing))));
    }

    @Override
    public StructureType<?> type() {
        return ModSanctums.SANCTUM.get();
    }
}
