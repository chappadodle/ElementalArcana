package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CryptLayout;
import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.tower.MageTowerStructure;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * An Arcane Crypt (docs/superpowers/specs/2026-10-04-arcane-crypts-design.md): a mausoleum in the
 * chunk's middle, a stair down along a random heading, and the rooms CryptLayout plans, all on one
 * floor at least COVER blocks under the lowest ground above any of them (the stair must reach it).
 * Never within MIN_DISTANCE of the world's centre, nor where the mausoleum would stand in water. Its
 * element is the land's (as a mage tower's), now and then its kin.
 */
public class CryptStructure extends Structure {
    public static final MapCodec<CryptStructure> CODEC = simpleCodec(CryptStructure::new);
    private static final Map<Element, TagKey<Biome>> BIOME_ELEMENTS = new EnumMap<>(Element.class);

    static {
        for (Element element : MageTowerStructure.TOWER_ELEMENTS) {
            BIOME_ELEMENTS.put(element, TagKey.create(Registries.BIOME, ElementalArcana.id("attunes/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    public CryptStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int mx = chunk.getMiddleBlockX();
        int mz = chunk.getMiddleBlockZ();
        if (Math.hypot(mx, mz) < CryptRules.MIN_DISTANCE) {
            return Optional.empty();
        }
        ChunkGenerator generator = context.chunkGenerator();
        LevelHeightAccessor height = context.heightAccessor();
        RandomState randomState = context.randomState();
        int ground = generator.getFirstOccupiedHeight(mx, mz, Heightmap.Types.WORLD_SURFACE_WG, height, randomState);
        if (ground > generator.getFirstOccupiedHeight(mx, mz, Heightmap.Types.OCEAN_FLOOR_WG, height, randomState)) {
            // The mausoleum would stand in water.
            return Optional.empty();
        }
        WorldgenRandom random = context.random();
        int heading = random.nextInt(4);
        List<CryptLayout.Room> rooms = CryptLayout.plan(random::nextInt, heading);
        int lowest = ground;
        for (CryptLayout.Room room : rooms) {
            for (int[] cell : CryptLayout.cellsOf(room)) {
                int[] middle = cellMiddle(mx, mz, heading, cell[0], cell[1]);
                lowest = Math.min(lowest, generator.getFirstOccupiedHeight(middle[0], middle[1], Heightmap.Types.OCEAN_FLOOR_WG, height, randomState));
            }
        }
        int floorY = lowest - CryptRules.COVER;
        if (!CryptRules.stairReaches(ground, floorY) || floorY < height.getMinBuildHeight() + 8) {
            return Optional.empty();
        }
        Element element = CryptRules.element(landElement(context, mx, ground, mz), random.nextInt(CryptRules.KIN_ODDS) == 0);
        List<CryptRoomPiece> pieces = new ArrayList<>();
        for (CryptLayout.Room room : rooms) {
            int[] middle = cellMiddle(mx, mz, heading, room.cellX(), room.cellZ());
            if (room.kind() == CryptLayout.Kind.CHAMBER) {
                // The chamber's middle lies six blocks on from the middle of the cell it's entered through.
                int cx = middle[0] + CryptLayout.dx(room.forward()) * 6;
                int cz = middle[1] + CryptLayout.dz(room.forward()) * 6;
                int half = CryptRules.CHAMBER_SIZE / 2;
                pieces.add(new CryptRoomPiece(room.kind(), element, cx - half, floorY, cz - half, room.forward(), room.doors()));
            } else {
                int half = CryptLayout.CELL / 2;
                pieces.add(new CryptRoomPiece(room.kind(), element, middle[0] - half, floorY, middle[1] - half, room.forward(), room.doors()));
            }
        }
        CryptEntrancePiece entrance = new CryptEntrancePiece(element, mx, ground, mz, heading, floorY);
        return Optional.of(new GenerationStub(new BlockPos(mx, ground, mz), builder -> {
            builder.addPiece(entrance);
            pieces.forEach(builder::addPiece);
        }));
    }

    /** The middle of a cell, in blocks: cells are counted from the entry hall, ENTRY_OFFSET blocks on from the mausoleum. */
    static int[] cellMiddle(int mx, int mz, int heading, int cellX, int cellZ) {
        return new int[]{mx + CryptLayout.dx(heading) * CryptLayout.ENTRY_OFFSET + cellX * CryptLayout.CELL,
                mz + CryptLayout.dz(heading) * CryptLayout.ENTRY_OFFSET + cellZ * CryptLayout.CELL};
    }

    private static Element landElement(GenerationContext context, int x, int y, int z) {
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z),
                context.randomState().sampler());
        List<Element> elements = new ArrayList<>();
        BIOME_ELEMENTS.forEach((element, tag) -> {
            if (biome.is(tag)) {
                elements.add(element);
            }
        });
        List<Element> from = elements.isEmpty() ? MageTowerStructure.TOWER_ELEMENTS : elements;
        return from.get(context.random().nextInt(from.size()));
    }

    @Override
    public StructureType<?> type() {
        return ModCrypts.CRYPT.get();
    }
}
