package com.chappadodle.elementalarcana.content.tower;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A mage tower (see MageTowerPiece for the building). It needs fairly level, dry ground at or above
 * sea level: the corners and middle of its 13x13 footprint may differ by at most 8 blocks. It never
 * stands within 800 blocks of the world's centre, where new players start. Its element comes from
 * the biome (the attunes/<element> tags), or is any element where the biome has none.
 */
public class MageTowerStructure extends Structure {
    public static final MapCodec<MageTowerStructure> CODEC = simpleCodec(MageTowerStructure::new);
    private static final int SIZE = 13;
    private static final int MAX_SLOPE = 8;
    private static final int MIN_DISTANCE = 800;
    private static final Map<Element, TagKey<Biome>> BIOME_ELEMENTS = new EnumMap<>(Element.class);
    /** The elements towers are built of (each has its own stone): the base elements and Ice. */
    public static final List<Element> TOWER_ELEMENTS = List.of(Element.FIRE, Element.WATER, Element.ICE, Element.WIND, Element.EARTH);

    static {
        for (Element element : TOWER_ELEMENTS) {
            BIOME_ELEMENTS.put(element, TagKey.create(Registries.BIOME, ElementalArcana.id("attunes/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    public MageTowerStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        if (Math.hypot(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ()) < MIN_DISTANCE) {
            return Optional.empty();
        }
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
        Element element = elementAt(context, x + SIZE / 2, groundY, z + SIZE / 2);
        Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        BlockPos origin = new BlockPos(x, groundY, z);
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new MageTowerPiece(element, x, groundY, z, facing))));
    }

    private static Element elementAt(GenerationContext context, int x, int y, int z) {
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                context.randomState().sampler());
        List<Element> elements = new ArrayList<>();
        BIOME_ELEMENTS.forEach((element, tag) -> {
            if (biome.is(tag)) {
                elements.add(element);
            }
        });
        if (elements.isEmpty()) {
            return TOWER_ELEMENTS.get(context.random().nextInt(TOWER_ELEMENTS.size()));
        }
        return elements.get(context.random().nextInt(elements.size()));
    }

    @Override
    public StructureType<?> type() {
        return ModTowers.MAGE_TOWER.get();
    }
}
