package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.world.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Locale;

/**
 * Builds a sanctum, Minecraft style: an open-air arena 39 blocks across in its element's stone. A
 * round floor (rings and eight spokes of trim) on a foundation down to the ground, a wall with four
 * gates, eight pillars topped with light, and in the middle a stepped dais with four obelisks, the
 * Seal on top and the reward chest behind it. Each element adds its own ground: lava pits and
 * magma (the Caldera), pools and channels (the Tidehall), a low wall and tall pillars (the Aerie),
 * thick walls crowned with amethyst, moss and geodes (the Deepvault). Local coordinates: the middle
 * is (21, 21) and the floor is at y 0.
 */
public class SanctumPiece extends StructurePiece {
    public static final int MID = 21;
    private static final int SIZE = MID * 2 + 1;
    private static final int TOP = 22;
    private static final double FLOOR = 18.5;
    private static final double OUTSIDE = 20.5;
    private static final int PILLAR_RADIUS = 15;
    private static final double POOL_RADIUS = 11.3;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    /** The blocks a sanctum is made of. */
    private record Palette(BlockState wall, BlockState accent, BlockState floor, BlockState floorAlt, BlockState trim, BlockState ring,
                           BlockState pillar, BlockState light, BlockState foundation) {
    }

    private final Element element;

    public SanctumPiece(Element element, int x, int groundY, int z, Direction facing) {
        super(ModSanctums.SANCTUM_PIECE.get(), 0, new BoundingBox(x, groundY, z, x + SIZE - 1, groundY + TOP, z + SIZE - 1));
        this.element = element;
        setOrientation(facing);
    }

    public SanctumPiece(CompoundTag tag) {
        super(ModSanctums.SANCTUM_PIECE.get(), tag);
        Element read = Element.FIRE;
        for (Element candidate : Element.values()) {
            if (candidate.name().equalsIgnoreCase(tag.getString("element"))) {
                read = candidate;
            }
        }
        this.element = read;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("element", element.name().toLowerCase(Locale.ROOT));
    }

    private Palette palette() {
        return switch (element) {
            case WATER -> new Palette(Blocks.PRISMARINE_BRICKS.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState(),
                    Blocks.PRISMARINE.defaultBlockState(), Blocks.PRISMARINE_BRICKS.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState(),
                    Blocks.SEA_LANTERN.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(),
                    Blocks.PRISMARINE_BRICKS.defaultBlockState());
            case WIND -> new Palette(Blocks.CALCITE.defaultBlockState(), Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(),
                    Blocks.QUARTZ_BRICKS.defaultBlockState(), Blocks.QUARTZ_PILLAR.defaultBlockState(), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(),
                    Blocks.STONE.defaultBlockState());
            case EARTH -> new Palette(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(),
                    Blocks.AMETHYST_BLOCK.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(), Blocks.VERDANT_FROGLIGHT.defaultBlockState(),
                    Blocks.COBBLED_DEEPSLATE.defaultBlockState());
            default -> new Palette(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState(), Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.MAGMA_BLOCK.defaultBlockState(), Blocks.POLISHED_BASALT.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(),
                    Blocks.BLACKSTONE.defaultBlockState());
        };
    }

    private int wallHeight() {
        return switch (element) {
            case WIND -> 2;
            case EARTH -> 8;
            default -> 6;
        };
    }

    /** Where the wall begins: the Deepvault's is three blocks thick, the others' two. */
    private double wallInner() {
        return element == Element.EARTH ? 17.5 : FLOOR;
    }

    private int pillarHeight() {
        return switch (element) {
            case WIND -> 13;
            case EARTH -> 10;
            default -> 9;
        };
    }

    private static double radius(int x, int z) {
        return Math.hypot(x - MID, z - MID);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Palette p = palette();
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double r = radius(x, z);
                if (r > OUTSIDE) {
                    continue;
                }
                fillColumnDown(level, p.foundation(), x, -1, z, box);
                placeBlock(level, floorAt(p, x, z, r, random), x, 0, z, box);
                for (int y = 1; y <= TOP; y++) {
                    placeBlock(level, AIR, x, y, z, box);
                }
                if (r > wallInner()) {
                    for (int y = 1; y <= wallHeight(); y++) {
                        placeBlock(level, y == wallHeight() ? p.accent() : p.wall(), x, y, z, box);
                    }
                }
            }
        }
        gates(level, box, p);
        pillars(level, box, p);
        dais(level, box, random, p);
        switch (element) {
            case FIRE -> pools(level, box, Blocks.LAVA.defaultBlockState(), p.ring(), p.ring());
            case WATER -> {
                pools(level, box, Blocks.WATER.defaultBlockState(), p.trim(), p.light());
                channels(level, box);
            }
            case EARTH -> {
                geodes(level, box);
                crown(level, box);
            }
            default -> {
            }
        }
    }

    private BlockState floorAt(Palette p, int x, int z, double r, RandomSource random) {
        if (r <= 4.5 || r > 7.5 && r <= 8.5) {
            return p.trim();
        }
        if (r > 13 && r <= 14) {
            return p.ring();
        }
        double angle = Math.atan2(z - MID, x - MID);
        double spoke = Math.round(angle / (Math.PI / 4)) * (Math.PI / 4);
        if (r <= FLOOR && Math.abs(Math.sin(angle - spoke)) * r < 0.6) {
            return p.trim();
        }
        return random.nextFloat() < 0.15 ? p.floorAlt() : p.floor();
    }

    /** Four gates through the wall, three wide, with a lintel where the wall is tall enough. */
    private void gates(WorldGenLevel level, BoundingBox box, Palette p) {
        int open = Math.min(4, wallHeight());
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                boolean inGate = Math.abs(x - MID) <= 1 || Math.abs(z - MID) <= 1;
                if (!inGate || radius(x, z) <= wallInner() || radius(x, z) > OUTSIDE) {
                    continue;
                }
                for (int y = 1; y <= open; y++) {
                    placeBlock(level, AIR, x, y, z, box);
                }
                if (wallHeight() > open) {
                    placeBlock(level, p.accent(), x, open + 1, z, box);
                }
            }
        }
    }

    /** Eight pillars between the gates, topped with light (the Aerie's with end rods, the Deepvault's with amethyst). */
    private void pillars(WorldGenLevel level, BoundingBox box, Palette p) {
        int half = element == Element.WIND ? 0 : 1;
        int height = pillarHeight();
        for (int k = 0; k < 8; k++) {
            double angle = Math.toRadians(22.5 + 45 * k);
            int px = MID + (int) Math.round(Math.cos(angle) * PILLAR_RADIUS);
            int pz = MID + (int) Math.round(Math.sin(angle) * PILLAR_RADIUS);
            for (int dx = -half; dx <= half; dx++) {
                for (int dz = -half; dz <= half; dz++) {
                    for (int y = 1; y <= height; y++) {
                        placeBlock(level, p.pillar(), px + dx, y, pz + dz, box);
                    }
                }
            }
            placeBlock(level, p.light(), px, height + 1, pz, box);
            if (element == Element.WIND) {
                placeBlock(level, Blocks.END_ROD.defaultBlockState(), px, height + 2, pz, box);
            } else if (element == Element.EARTH) {
                placeBlock(level, Blocks.AMETHYST_CLUSTER.defaultBlockState(), px, height + 2, pz, box);
            }
        }
    }

    /** The stepped dais with the Seal on top, four obelisks around it, and the reward chest. */
    private void dais(WorldGenLevel level, BoundingBox box, RandomSource random, Palette p) {
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                placeBlock(level, p.wall(), MID + dx, 1, MID + dz, box);
                if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
                    placeBlock(level, p.trim(), MID + dx, 2, MID + dz, box);
                }
            }
        }
        for (int[] corner : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            placeBlock(level, p.light(), MID + corner[0], 3, MID + corner[1], box);
        }
        placeBlock(level, ModSanctums.SANCTUM_SEAL.get().defaultBlockState().setValue(SanctumSealBlock.KIND, ShrineKind.of(element)),
                MID, 3, MID, box);
        for (int[] corner : new int[][]{{4, 4}, {-4, 4}, {4, -4}, {-4, -4}}) {
            for (int y = 1; y <= 4; y++) {
                placeBlock(level, p.accent(), MID + corner[0], y, MID + corner[1], box);
            }
            placeBlock(level, p.light(), MID + corner[0], 5, MID + corner[1], box);
        }
        createChest(level, box, random, MID, 1, MID + 5, lootTable());
    }

    /** Four 3x3 pools on the diagonals, between the pillars, each in a rim (corners {@code corner}). */
    private void pools(WorldGenLevel level, BoundingBox box, BlockState fill, BlockState rim, BlockState corner) {
        for (int k = 0; k < 4; k++) {
            double angle = Math.toRadians(45 + 90 * k);
            int cx = MID + (int) Math.round(Math.cos(angle) * POOL_RADIUS);
            int cz = MID + (int) Math.round(Math.sin(angle) * POOL_RADIUS);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    boolean inside = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                    boolean isCorner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                    placeBlock(level, inside ? fill : isCorner ? corner : rim, cx + dx, 0, cz + dz, box);
                }
            }
        }
    }

    /** The Tidehall's channels: water from the dais out toward the four gates. */
    private void channels(WorldGenLevel level, BoundingBox box) {
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int d = 6; d <= 17; d++) {
            placeBlock(level, water, MID + d, 0, MID, box);
            placeBlock(level, water, MID - d, 0, MID, box);
            placeBlock(level, water, MID, 0, MID + d, box);
            placeBlock(level, water, MID, 0, MID - d, box);
        }
    }

    /** The Deepvault's geodes: amethyst set into the floor on the diagonals, crystals growing out of it. */
    private void geodes(WorldGenLevel level, BoundingBox box) {
        for (int k = 0; k < 4; k++) {
            double angle = Math.toRadians(45 + 90 * k);
            int cx = MID + (int) Math.round(Math.cos(angle) * POOL_RADIUS);
            int cz = MID + (int) Math.round(Math.sin(angle) * POOL_RADIUS);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    placeBlock(level, Blocks.AMETHYST_BLOCK.defaultBlockState(), cx + dx, 0, cz + dz, box);
                }
            }
            placeBlock(level, Blocks.AMETHYST_CLUSTER.defaultBlockState(), cx, 1, cz, box);
            for (int[] side : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                placeBlock(level, Blocks.MEDIUM_AMETHYST_BUD.defaultBlockState(), cx + side[0], 1, cz + side[1], box);
            }
        }
    }

    /** Amethyst crystals along the top of the Deepvault's wall. */
    private void crown(WorldGenLevel level, BoundingBox box) {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                double r = radius(x, z);
                boolean inGate = Math.abs(x - MID) <= 1 || Math.abs(z - MID) <= 1;
                if (!inGate && r > 19 && r <= 20 && (x * 7 + z * 3) % 5 == 0) {
                    placeBlock(level, Blocks.AMETHYST_CLUSTER.defaultBlockState(), x, wallHeight() + 1, z, box);
                }
            }
        }
    }

    private static ResourceKey<LootTable> lootTable() {
        return ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/sanctum"));
    }
}
