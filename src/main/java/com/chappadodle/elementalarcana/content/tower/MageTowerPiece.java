package com.chappadodle.elementalarcana.content.tower;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.people.ModPeople;
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
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds a mage tower, Minecraft style: a round tower 13 blocks across in its element's stone, five
 * floors of 6 blocks (hall, library, laboratory, armory and the sanctum with the Tower Heart), then
 * battlements and a crystal spire. A straight flight of stairs runs along the wall from each floor
 * to the next, on alternating sides. Local coordinates: the middle is (6, 6); floor slabs are at
 * y 0, 6, 12, 18, 24 and the roof at 30. The door faces local south.
 */
public class MageTowerPiece extends StructurePiece {
    private static final int MID = 6;
    private static final int FLOOR = 6;
    private static final int FLOORS = 5;
    private static final int ROOF = FLOOR * FLOORS;
    private static final double INSIDE = 5.0;
    private static final double OUTSIDE = 6.4;

    /** The blocks a tower is made of. */
    private record Palette(BlockState wall, BlockState accent, BlockState floor, BlockState stairs, BlockState pane,
                           BlockState glass, BlockState light, BlockState trim, BlockState pillar, BlockState foundation) {
    }

    private final Element element;

    public MageTowerPiece(Element element, int x, int groundY, int z, Direction facing) {
        super(ModTowers.MAGE_TOWER_PIECE.get(), 0, new BoundingBox(x, groundY, z, x + 12, groundY + ROOF + 8, z + 12));
        this.element = element;
        setOrientation(facing);
    }

    public MageTowerPiece(CompoundTag tag) {
        super(ModTowers.MAGE_TOWER_PIECE.get(), tag);
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
        tag.putString("element", element.name().toLowerCase(java.util.Locale.ROOT));
    }

    private Palette palette() {
        return switch (element) {
            case FIRE -> new Palette(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.RED_NETHER_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_BLACKSTONE.defaultBlockState(), Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS.defaultBlockState(),
                    Blocks.ORANGE_STAINED_GLASS_PANE.defaultBlockState(), Blocks.ORANGE_STAINED_GLASS.defaultBlockState(),
                    Blocks.SHROOMLIGHT.defaultBlockState(), Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                    Blocks.POLISHED_BASALT.defaultBlockState(), Blocks.BLACKSTONE.defaultBlockState());
            case WATER -> new Palette(Blocks.PRISMARINE_BRICKS.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState(),
                    Blocks.PRISMARINE.defaultBlockState(), Blocks.PRISMARINE_BRICK_STAIRS.defaultBlockState(),
                    Blocks.CYAN_STAINED_GLASS_PANE.defaultBlockState(), Blocks.CYAN_STAINED_GLASS.defaultBlockState(),
                    Blocks.SEA_LANTERN.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState(),
                    Blocks.DARK_PRISMARINE.defaultBlockState(), Blocks.PRISMARINE_BRICKS.defaultBlockState());
            case ICE -> new Palette(Blocks.PACKED_ICE.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(),
                    Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.POLISHED_DIORITE_STAIRS.defaultBlockState(),
                    Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.defaultBlockState(), Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(),
                    Blocks.SEA_LANTERN.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(),
                    Blocks.PACKED_ICE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState());
            case WIND -> new Palette(Blocks.CALCITE.defaultBlockState(), Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.SMOOTH_QUARTZ.defaultBlockState(), Blocks.QUARTZ_STAIRS.defaultBlockState(),
                    Blocks.WHITE_STAINED_GLASS_PANE.defaultBlockState(), Blocks.WHITE_STAINED_GLASS.defaultBlockState(),
                    Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(),
                    Blocks.QUARTZ_PILLAR.defaultBlockState(), Blocks.STONE.defaultBlockState());
            case EARTH -> new Palette(Blocks.DEEPSLATE_BRICKS.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                    Blocks.POLISHED_DEEPSLATE.defaultBlockState(), Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState(),
                    Blocks.GREEN_STAINED_GLASS_PANE.defaultBlockState(), Blocks.GREEN_STAINED_GLASS.defaultBlockState(),
                    Blocks.VERDANT_FROGLIGHT.defaultBlockState(), Blocks.MOSS_BLOCK.defaultBlockState(),
                    Blocks.DEEPSLATE_TILES.defaultBlockState(), Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        };
    }

    private static double radius(int x, int z) {
        return Math.hypot(x - MID, z - MID);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Palette p = palette();
        shell(level, box, p);
        for (int floor = 0; floor < FLOORS; floor++) {
            windows(level, box, p, floor * FLOOR);
            stairs(level, box, p, floor);
        }
        hall(level, box, p);
        library(level, box, random, p, FLOOR);
        laboratory(level, box, random, p, FLOOR * 2);
        armory(level, box, random, p, FLOOR * 3);
        sanctum(level, box, random, p, FLOOR * 4);
        roof(level, box, p);
    }

    /** The walls, floors and foundation; air inside. */
    private void shell(WorldGenLevel level, BoundingBox box, Palette p) {
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                double r = radius(x, z);
                if (r > OUTSIDE) {
                    continue;
                }
                fillColumnDown(level, p.foundation(), x, -1, z, box);
                boolean wall = r > INSIDE;
                for (int y = 0; y <= ROOF; y++) {
                    BlockState state;
                    if (y % FLOOR == 0) {
                        state = wall ? p.wall() : p.floor();
                    } else if (wall) {
                        state = y % FLOOR == FLOOR - 1 ? p.accent() : p.wall();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    placeBlock(level, state, x, y, z, box);
                }
                for (int y = ROOF + 1; y <= ROOF + 8; y++) {
                    placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                }
            }
        }
        // The door, in the south wall.
        placeBlock(level, Blocks.AIR.defaultBlockState(), MID, 1, 12, box);
        placeBlock(level, Blocks.AIR.defaultBlockState(), MID, 2, 12, box);
        placeBlock(level, p.accent(), MID, 3, 12, box);
    }

    /** Glass in the four quarters of the wall, two blocks tall (not over the door). */
    private void windows(WorldGenLevel level, BoundingBox box, Palette p, int base) {
        BlockState northSouth = p.pane().setValue(CrossCollisionBlock.NORTH, true).setValue(CrossCollisionBlock.SOUTH, true);
        BlockState eastWest = p.pane().setValue(CrossCollisionBlock.EAST, true).setValue(CrossCollisionBlock.WEST, true);
        for (int y = base + 2; y <= base + 3; y++) {
            placeBlock(level, eastWest, MID, y, 0, box);
            placeBlock(level, northSouth, 0, y, MID, box);
            placeBlock(level, northSouth, 12, y, MID, box);
            if (base > 0) {
                placeBlock(level, eastWest, MID, y, 12, box);
            }
        }
    }

    /**
     * The flight from {@code floor} up to the next: six steps along the north wall (rising east) on
     * even floors, along the south wall (rising west) on odd ones, with a gap in the ceiling above.
     */
    private void stairs(WorldGenLevel level, BoundingBox box, Palette p, int floor) {
        int base = floor * FLOOR;
        boolean even = floor % 2 == 0;
        int z = even ? 2 : 10;
        Direction up = even ? Direction.EAST : Direction.WEST;
        BlockState step = p.stairs().setValue(StairBlock.FACING, up);
        for (int i = 0; i < FLOOR; i++) {
            int x = even ? 3 + i : 9 - i;
            for (int y = base + 1; y < base + 1 + i; y++) {
                placeBlock(level, p.accent(), x, y, z, box);
            }
            placeBlock(level, step, x, base + 1 + i, z, box);
            if (i >= 3 && i < FLOOR - 1) {
                placeBlock(level, Blocks.AIR.defaultBlockState(), x, base + FLOOR, z, box);
            }
        }
    }

    private void hall(WorldGenLevel level, BoundingBox box, Palette p) {
        for (int[] at : new int[][]{{3, 4}, {9, 4}, {3, 8}, {9, 8}}) {
            for (int y = 1; y < FLOOR; y++) {
                placeBlock(level, p.pillar(), at[0], y, at[1], box);
            }
        }
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), MID, FLOOR - 1, MID, box);
        placeBlock(level, p.light(), MID, 0, MID, box);
    }

    private void library(WorldGenLevel level, BoundingBox box, RandomSource random, Palette p, int base) {
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                double r = radius(x, z);
                // Shelves along the wall, clear of the windows, the stairs (south wall) and the way up.
                boolean nearWindow = Math.abs(x - MID) <= 1 || Math.abs(z - MID) <= 1;
                if (r > 4.0 && r <= INSIDE && !nearWindow && z < 9) {
                    for (int y = base + 1; y <= base + 3; y++) {
                        placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(), x, y, z, box);
                    }
                }
            }
        }
        placeBlock(level, ModPeople.ARCANE_LECTERN.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH),
                MID, base + 1, MID, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), MID, base + FLOOR - 1, MID - 2, box);
        createChest(level, box, random, MID, base + 1, MID - 3, lootTable("library"));
    }

    private void laboratory(WorldGenLevel level, BoundingBox box, RandomSource random, Palette p, int base) {
        placeBlock(level, Blocks.BREWING_STAND.defaultBlockState(), 4, base + 1, 8, box);
        placeBlock(level, Blocks.BREWING_STAND.defaultBlockState(), 8, base + 1, 8, box);
        placeBlock(level, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3), MID, base + 1, 9, box);
        placeBlock(level, Blocks.AMETHYST_BLOCK.defaultBlockState(), 3, base + 1, MID, box);
        placeBlock(level, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP), 3, base + 2, MID, box);
        placeBlock(level, Blocks.AMETHYST_BLOCK.defaultBlockState(), 9, base + 1, MID, box);
        placeBlock(level, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP), 9, base + 2, MID, box);
        placeBlock(level, p.light(), MID, base, MID, box);
        createChest(level, box, random, MID, base + 1, 4, lootTable("laboratory"));
    }

    private void armory(WorldGenLevel level, BoundingBox box, RandomSource random, Palette p, int base) {
        placeBlock(level, Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.EAST), 4, base + 1, 4, box);
        placeBlock(level, Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, Direction.WEST), 8, base + 1, 4, box);
        placeBlock(level, Blocks.SMITHING_TABLE.defaultBlockState(), MID, base + 1, 3, box);
        placeBlock(level, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), MID, base + FLOOR - 1, MID, box);
        createChest(level, box, random, 3, base + 1, MID, lootTable("armory"));
    }

    /** The rune circle, the heart on its pedestal, and the reward chest. */
    private void sanctum(WorldGenLevel level, BoundingBox box, RandomSource random, Palette p, int base) {
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                double r = radius(x, z);
                if (r > 2.5 && r <= 3.5) {
                    placeBlock(level, p.trim(), x, base, z, box);
                }
            }
        }
        for (int[] at : new int[][]{{2, MID}, {10, MID}, {MID, 10}}) {
            placeBlock(level, p.light(), at[0], base, at[1], box);
        }
        placeBlock(level, p.accent(), MID, base + 1, MID, box);
        placeBlock(level, ModTowers.TOWER_HEART.get().defaultBlockState()
                .setValue(TowerHeartBlock.KIND, kind()).setValue(TowerHeartBlock.LIT, true), MID, base + 2, MID, box);
        createChest(level, box, random, MID, base + 1, 10, lootTable("sanctum"));
    }

    /** Battlements, and a crystal spire in the middle of the roof. */
    private void roof(WorldGenLevel level, BoundingBox box, Palette p) {
        for (int x = 0; x <= 12; x++) {
            for (int z = 0; z <= 12; z++) {
                double r = radius(x, z);
                if (r > INSIDE && r <= OUTSIDE && (x + z) % 2 == 0) {
                    placeBlock(level, p.accent(), x, ROOF + 1, z, box);
                }
            }
        }
        placeBlock(level, p.accent(), MID, ROOF + 1, MID, box);
        for (int y = ROOF + 2; y <= ROOF + 5; y++) {
            placeBlock(level, p.glass(), MID, y, MID, box);
        }
        placeBlock(level, p.light(), MID, ROOF + 6, MID, box);
        placeBlock(level, Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP), MID, ROOF + 7, MID, box);
    }

    private ShrineKind kind() {
        return ShrineKind.valueOf(element.name());
    }

    private static ResourceKey<LootTable> lootTable(String room) {
        return ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/mage_tower_" + room));
    }
}
