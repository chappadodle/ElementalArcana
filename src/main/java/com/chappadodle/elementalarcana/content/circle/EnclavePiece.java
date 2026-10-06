package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Herb;
import com.chappadodle.elementalarcana.content.people.ModPeople;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds the Circle's Enclave (see its spec), in world coordinates round its middle (cx, cz), its
 * courtyard's ground at cy - 1: walls of stone bricks 41 blocks square with a squat tower at each
 * corner and a gate in the south wall; in the middle the Spire, a round tower of white stone, its
 * library on the ground floor (the Circle Heart under its middle), the Archmagister's study above,
 * an observatory under a glass dome at the top, a ladder up the north wall (through trapdoors); the
 * herb garden in the north-west, the duelling ring in the north-east, two cottages either side of
 * the main path, a well east of the Spire and practice targets west of it; paths and lanterns.
 * Every choice comes from the Enclave's seed and each block's place, so it comes out the same
 * whichever chunk is built first.
 */
public class EnclavePiece extends StructurePiece {
    private static final ResourceKey<LootTable> LIBRARY = ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/enclave_library"));
    /** How far the walls stand from the middle. */
    static final int HALF = 20;
    /** The Spire: its wall's outer and inner radius, a storey's height (floors at cy, cy + 6, cy + 12; the dome from cy + 18). */
    private static final double SPIRE_OUT = 5.4;
    private static final double SPIRE_IN = 4.4;
    static final int STOREY = 6;
    static final int TOP = 3 * STOREY + 6;

    private final long seed;
    private final int cx;
    private final int cy;
    private final int cz;

    public EnclavePiece(long seed, int cx, int cy, int cz) {
        super(ModCircle.ENCLAVE_PIECE.get(), 0, new BoundingBox(cx - HALF - 1, cy - 1, cz - HALF - 1, cx + HALF + 1, cy + TOP, cz + HALF + 1));
        this.seed = seed;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        setOrientation(null);
    }

    public EnclavePiece(CompoundTag tag) {
        this(tag.getLong("seed"), tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("seed", seed);
        tag.putInt("x", cx);
        tag.putInt("y", cy);
        tag.putInt("z", cz);
    }

    /** A roll out of {@code sides} decided by a block's place (and this Enclave's seed). */
    private int roll(int x, int y, int z, int sides) {
        return Math.floorMod((int) ((Mth.getSeed(x, y, z) ^ seed) >>> 16), sides);
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockState state, int x, int y, int z) {
        placeBlock(level, state, x, y, z, box);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        courtyard(level, box);
        walls(level, box);
        paths(level, box);
        garden(level, box);
        ring(level, box);
        cottage(level, box, cx + 12, cz + 11, Direction.WEST);
        cottage(level, box, cx - 12, cz + 11, Direction.EAST);
        well(level, box);
        targets(level, box);
        spire(level, box);
        lanterns(level, box);
    }

    /** The courtyard: grass at the ground, open above (trees and all cleared). */
    private void courtyard(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - HALF; x <= cx + HALF; x++) {
            for (int z = cz - HALF; z <= cz + HALF; z++) {
                if (!box.isInside(x, cy, z)) {
                    continue;
                }
                put(level, box, Blocks.GRASS_BLOCK.defaultBlockState(), x, cy - 1, z);
                for (int y = cy; y <= cy + TOP; y++) {
                    put(level, box, Blocks.AIR.defaultBlockState(), x, y, z);
                }
            }
        }
    }

    private BlockState brick(int x, int y, int z) {
        int pick = roll(x, y, z, 12);
        return pick == 0 ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : pick == 1 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState() : Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** The walls, four high with crenels; a squat tower at each corner; the gate under an arch in the south wall. */
    private void walls(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - HALF; x <= cx + HALF; x++) {
            for (int z = cz - HALF; z <= cz + HALF; z++) {
                if (Math.abs(x - cx) != HALF && Math.abs(z - cz) != HALF) {
                    continue;
                }
                for (int y = cy; y <= cy + 3; y++) {
                    put(level, box, brick(x, y, z), x, y, z);
                }
                if ((x + z) % 2 == 0) {
                    put(level, box, brick(x, cy + 4, z), x, cy + 4, z);
                }
            }
        }
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int tx = cx + sx * (HALF - 1);
                int tz = cz + sz * (HALF - 1);
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        for (int y = cy; y <= cy + 5; y++) {
                            put(level, box, brick(tx + dx, y, tz + dz), tx + dx, y, tz + dz);
                        }
                        if ((Math.abs(dx) == 2 || Math.abs(dz) == 2) && (dx + dz) % 2 == 0) {
                            put(level, box, brick(tx + dx, cy + 6, tz + dz), tx + dx, cy + 6, tz + dz);
                        }
                    }
                }
                put(level, box, Blocks.LANTERN.defaultBlockState(), tx, cy + 6, tz);
            }
        }
        // The gate: three wide, three high, a lintel of chiseled bricks, a lantern either side on top.
        int gz = cz + HALF;
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int y = cy; y <= cy + 2; y++) {
                put(level, box, Blocks.AIR.defaultBlockState(), x, y, gz);
            }
        }
        for (int x = cx - 2; x <= cx + 2; x++) {
            put(level, box, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), x, cy + 3, gz);
        }
        put(level, box, Blocks.LANTERN.defaultBlockState(), cx - 2, cy + 4, gz);
        put(level, box, Blocks.LANTERN.defaultBlockState(), cx + 2, cy + 4, gz);
    }

    /** The main path from the gate to the Spire's door, a path round the Spire, and branches to the cottages. */
    private void paths(WorldGenLevel level, BoundingBox box) {
        for (int z = cz + 6; z <= cz + HALF; z++) {
            for (int x = cx - 1; x <= cx + 1; x++) {
                put(level, box, x == cx ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState(), x, cy - 1, z);
            }
        }
        for (int x = cx - 10; x <= cx + 10; x++) {
            put(level, box, Blocks.POLISHED_ANDESITE.defaultBlockState(), x, cy - 1, cz + 11);
        }
        for (int x = cx - 8; x <= cx + 8; x++) {
            for (int z = cz - 8; z <= cz + 8; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > SPIRE_OUT + 0.6 && d <= SPIRE_OUT + 2.1) {
                    put(level, box, Blocks.POLISHED_ANDESITE.defaultBlockState(), x, cy - 1, z);
                }
            }
        }
    }

    /** The ground each herb grows on in the garden. */
    private static BlockState soil(Herb herb) {
        return switch (herb) {
            case EMBERBLOOM -> Blocks.SAND.defaultBlockState();
            case MOONLILY -> Blocks.WATER.defaultBlockState();
            case DEEPCAP -> Blocks.MOSS_BLOCK.defaultBlockState();
            case FROSTCAP -> Blocks.SNOW_BLOCK.defaultBlockState();
            case PRISMLEAF -> Blocks.CALCITE.defaultBlockState();
            case STORMTHISTLE -> Blocks.COARSE_DIRT.defaultBlockState();
            case SKYPLUME, SUNPETAL -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    /** The herb garden in the north-west: a bed of each herb on its own ground, in two rows, a hedge round them. */
    private void garden(WorldGenLevel level, BoundingBox box) {
        Herb[] herbs = Herb.values();
        for (int i = 0; i < herbs.length; i++) {
            int x0 = cx - 17 + (i % 4) * 3;
            int z0 = cz - 17 + (i / 4) * 3;
            BlockState plant = BuiltInRegistries.BLOCK.get(ElementalArcana.id(herbs[i].id())).defaultBlockState();
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    put(level, box, soil(herbs[i]), x0 + dx, cy - 1, z0 + dz);
                    put(level, box, plant, x0 + dx, cy, z0 + dz);
                }
            }
        }
        BlockState hedge = Blocks.AZALEA_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true);
        for (int x = cx - 19; x <= cx - 5; x++) {
            for (int z = cz - 19; z <= cz - 9; z++) {
                boolean edge = x == cx - 19 || x == cx - 5 || z == cz - 19 || z == cz - 9;
                boolean opening = z == cz - 9 && x >= cx - 13 && x <= cx - 11;
                if (edge && !opening) {
                    put(level, box, hedge, x, cy, z);
                }
            }
        }
    }

    /** The duelling ring in the north-east: smooth stone in a rim of polished andesite, open to the south-west, lanterns round it. */
    private void ring(WorldGenLevel level, BoundingBox box) {
        int rx = cx + 12;
        int rz = cz - 12;
        for (int x = rx - 7; x <= rx + 7; x++) {
            for (int z = rz - 7; z <= rz + 7; z++) {
                double d = Math.hypot(x - rx, z - rz);
                if (d <= 5.5) {
                    put(level, box, Blocks.SMOOTH_STONE.defaultBlockState(), x, cy - 1, z);
                } else if (d <= 6.5) {
                    put(level, box, Blocks.POLISHED_ANDESITE.defaultBlockState(), x, cy - 1, z);
                    boolean opening = x < rx - 2 && z > rz + 2;
                    if (!opening) {
                        put(level, box, Blocks.POLISHED_ANDESITE_SLAB.defaultBlockState(), x, cy, z);
                    }
                }
            }
        }
        for (int[] post : new int[][]{{5, 5}, {-5, -5}, {5, -5}, {-5, 5}}) {
            int px = rx + post[0] + Integer.signum(post[0]);
            int pz = rz + post[1] + Integer.signum(post[1]);
            put(level, box, Blocks.SPRUCE_FENCE.defaultBlockState(), px, cy, pz);
            put(level, box, Blocks.LANTERN.defaultBlockState(), px, cy + 1, pz);
        }
    }

    /** A cottage five blocks square round (x, z), its door in the wall toward {@code door}: a bed and an Arcane Lectern inside. */
    private void cottage(WorldGenLevel level, BoundingBox box, int x, int z, Direction door) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean wall = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                boolean corner = Math.abs(dx) == 2 && Math.abs(dz) == 2;
                put(level, box, Blocks.SPRUCE_PLANKS.defaultBlockState(), x + dx, cy - 1, z + dz);
                for (int y = cy; y <= cy + 2; y++) {
                    BlockState state = !wall ? Blocks.AIR.defaultBlockState()
                            : corner ? Blocks.SPRUCE_LOG.defaultBlockState()
                            : y == cy ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.WHITE_TERRACOTTA.defaultBlockState();
                    put(level, box, state, x + dx, y, z + dz);
                }
                put(level, box, Blocks.SPRUCE_SLAB.defaultBlockState(), x + dx, cy + 3, z + dz);
            }
        }
        // The door, a window opposite it, the bed and the lectern at the back.
        int ddx = door.getStepX();
        int ddz = door.getStepZ();
        put(level, box, Blocks.AIR.defaultBlockState(), x + 2 * ddx, cy, z + 2 * ddz);
        put(level, box, Blocks.AIR.defaultBlockState(), x + 2 * ddx, cy + 1, z + 2 * ddz);
        put(level, box, Blocks.GLASS_PANE.defaultBlockState(), x - 2 * ddx, cy + 1, z - 2 * ddz);
        int bx = x - ddx;
        BlockState foot = Blocks.RED_BED.defaultBlockState().setValue(BedBlock.FACING, Direction.NORTH).setValue(BedBlock.PART, BedPart.FOOT);
        put(level, box, foot, bx, cy, z + 1);
        put(level, box, foot.setValue(BedBlock.PART, BedPart.HEAD), bx, cy, z);
        put(level, box, ModPeople.ARCANE_LECTERN.get().defaultBlockState(), bx, cy, z - 1);
        put(level, box, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), x, cy + 2, z);
    }

    /** A well east of the Spire: water four deep in a shaft of stone bricks, a rim, posts and a roof of spruce slabs. */
    private void well(WorldGenLevel level, BoundingBox box) {
        int wx = cx + 12;
        int wz = cz + 1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean middle = dx == 0 && dz == 0;
                boolean corner = dx != 0 && dz != 0;
                put(level, box, Blocks.STONE_BRICKS.defaultBlockState(), wx + dx, cy - 5, wz + dz);
                for (int y = cy - 4; y <= cy - 1; y++) {
                    put(level, box, middle ? Blocks.WATER.defaultBlockState() : brick(wx + dx, y, wz + dz), wx + dx, y, wz + dz);
                }
                if (!middle) {
                    put(level, box, corner ? brick(wx + dx, cy, wz + dz) : Blocks.STONE_BRICK_SLAB.defaultBlockState(), wx + dx, cy, wz + dz);
                }
                if (corner) {
                    put(level, box, Blocks.SPRUCE_FENCE.defaultBlockState(), wx + dx, cy + 1, wz + dz);
                    put(level, box, Blocks.SPRUCE_FENCE.defaultBlockState(), wx + dx, cy + 2, wz + dz);
                }
                put(level, box, Blocks.SPRUCE_SLAB.defaultBlockState(), wx + dx, cy + 3, wz + dz);
            }
        }
        put(level, box, Blocks.CHAIN.defaultBlockState(), wx, cy + 2, wz);
        put(level, box, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), wx, cy + 1, wz);
    }

    /** Practice targets west of the Spire, where the Circle's students aim their first spells: targets on hay bales. */
    private void targets(WorldGenLevel level, BoundingBox box) {
        for (int dz = -3; dz <= 3; dz += 3) {
            put(level, box, Blocks.HAY_BLOCK.defaultBlockState(), cx - 18, cy, cz + dz);
            put(level, box, Blocks.TARGET.defaultBlockState(), cx - 18, cy + 1, cz + dz);
        }
        // Where they stand to cast: a line of stone bricks, nine blocks off.
        for (int dz = -4; dz <= 4; dz++) {
            put(level, box, Blocks.STONE_BRICKS.defaultBlockState(), cx - 9, cy - 1, cz + dz);
        }
    }

    /** The Spire: walls, floors, windows, the door, the ladder, the dome; then each storey's furnishings. */
    private void spire(WorldGenLevel level, BoundingBox box) {
        int domeBase = cy + 3 * STOREY;
        for (int x = cx - 6; x <= cx + 6; x++) {
            for (int z = cz - 6; z <= cz + 6; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > SPIRE_OUT) {
                    continue;
                }
                put(level, box, Blocks.QUARTZ_BRICKS.defaultBlockState(), x, cy - 1, z);
                boolean wall = d > SPIRE_IN;
                boolean cardinal = x == cx || z == cz;
                for (int y = cy; y < domeBase; y++) {
                    int storey = (y - cy) / STOREY;
                    int up = (y - cy) % STOREY;
                    BlockState state;
                    if (wall) {
                        if (storey == 2 && up >= 1 && !(Math.abs(x - cx) == Math.abs(z - cz) || cardinal)) {
                            // The observatory: open to the sky but for its pillars.
                            state = Blocks.GLASS_PANE.defaultBlockState();
                        } else if (cardinal && (up == 2 || up == 3) && !(storey == 0 && z > cz) && !(x == cx && z < cz)) {
                            // Windows at the cardinal points, but not over the door, nor behind the ladder (it needs the wall).
                            state = Blocks.GLASS_PANE.defaultBlockState();
                        } else {
                            state = up == 0 ? Blocks.QUARTZ_BRICKS.defaultBlockState()
                                    : up == STOREY - 1 ? Blocks.SMOOTH_QUARTZ.defaultBlockState() : Blocks.CALCITE.defaultBlockState();
                        }
                    } else if (up == 0 && storey > 0) {
                        state = storey == 1 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : Blocks.QUARTZ_BRICKS.defaultBlockState();
                    } else {
                        state = Blocks.AIR.defaultBlockState();
                    }
                    put(level, box, state, x, y, z);
                }
            }
        }
        // A band of gold under the dome.
        for (int x = cx - 6; x <= cx + 6; x++) {
            for (int z = cz - 6; z <= cz + 6; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > SPIRE_IN && d <= SPIRE_OUT) {
                    put(level, box, Blocks.GOLD_BLOCK.defaultBlockState(), x, domeBase, z);
                }
            }
        }
        // The dome: a shell of glass narrowing up to a golden tip.
        for (int h = 1; h <= 5; h++) {
            double r = Math.sqrt(Math.max(0, SPIRE_OUT * SPIRE_OUT - h * h * 1.1));
            for (int x = cx - 6; x <= cx + 6; x++) {
                for (int z = cz - 6; z <= cz + 6; z++) {
                    double d = Math.hypot(x - cx, z - cz);
                    if (d <= r && (d > r - 1.2 || h == 5)) {
                        put(level, box, Blocks.GLASS.defaultBlockState(), x, domeBase + h, z);
                    }
                }
            }
        }
        put(level, box, Blocks.GOLD_BLOCK.defaultBlockState(), cx, domeBase + 5, cz);
        put(level, box, Blocks.LIGHTNING_ROD.defaultBlockState(), cx, domeBase + 6, cz);
        // The door, under an arch, on the south.
        put(level, box, Blocks.AIR.defaultBlockState(), cx, cy, cz + 5);
        put(level, box, Blocks.AIR.defaultBlockState(), cx, cy + 1, cz + 5);
        put(level, box, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(), cx, cy + 2, cz + 5);
        // The ladder up the north wall, through a trapdoor in each floor (open, it climbs on as a ladder;
        // shut, no one walks into the hole and falls a storey).
        for (int y = cy; y < cy + 2 * STOREY + 1; y++) {
            BlockState rung = (y - cy) % STOREY == 0 && y > cy
                    ? Blocks.DARK_OAK_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.HALF, Half.TOP)
                    : Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
            put(level, box, rung, cx, y, cz - 4);
        }
        library(level, box);
        study(level, box);
        observatory(level, box);
        put(level, box, ModCircle.CIRCLE_HEART.get().defaultBlockState(), cx, cy - 1, cz);
    }

    /** The library: shelves round the wall (not at the door or the ladder), two lecterns, a chest, a hanging lantern. */
    private void library(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - 5; x <= cx + 5; x++) {
            for (int z = cz - 5; z <= cz + 5; z++) {
                double d = Math.hypot(x - cx, z - cz);
                boolean door = z > cz + 2 && Math.abs(x - cx) <= 1;
                boolean ladder = x == cx && z < cz - 2;
                if (d > SPIRE_IN - 1.05 && d <= SPIRE_IN && !door && !ladder) {
                    for (int y = cy; y <= cy + 2; y++) {
                        put(level, box, Blocks.BOOKSHELF.defaultBlockState(), x, y, z);
                    }
                }
            }
        }
        put(level, box, Blocks.LECTERN.defaultBlockState(), cx - 2, cy, cz);
        put(level, box, Blocks.LECTERN.defaultBlockState(), cx + 2, cy, cz);
        createChest(level, box, RandomSource.create(seed), cx + 2, cy, cz - 2, LIBRARY);
        put(level, box, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx, cy + STOREY - 1, cz + 1);
    }

    /**
     * The study: shelves round the wall between the windows, the Circle's gold banners either side of
     * the ladder, the Archmagister's chair and desk (a candle, a potted azalea), an enchanting table
     * between two shelves, a brewing stand, a carpet, a hanging lantern.
     */
    private void study(WorldGenLevel level, BoundingBox box) {
        // Everything stands on the study's floor (its planks are at cy + STOREY).
        int y = cy + STOREY + 1;
        for (int x = cx - 4; x <= cx + 4; x++) {
            for (int z = cz - 4; z <= cz + 4; z++) {
                double d = Math.hypot(x - cx, z - cz);
                // Not before the windows, nor the ladder (the cardinal points, a block either side).
                if (d > SPIRE_IN - 1.05 && d <= SPIRE_IN && Math.abs(x - cx) > 1 && Math.abs(z - cz) > 1) {
                    put(level, box, Blocks.BOOKSHELF.defaultBlockState(), x, y, z);
                    put(level, box, Blocks.BOOKSHELF.defaultBlockState(), x, y + 1, z);
                }
            }
        }
        put(level, box, Blocks.YELLOW_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH), cx - 1, y + 3, cz - 4);
        put(level, box, Blocks.YELLOW_WALL_BANNER.defaultBlockState().setValue(WallBannerBlock.FACING, Direction.SOUTH), cx + 1, y + 3, cz - 4);
        put(level, box, Blocks.ENCHANTING_TABLE.defaultBlockState(), cx + 2, y, cz - 2);
        put(level, box, Blocks.BOOKSHELF.defaultBlockState(), cx + 3, y, cz - 1);
        put(level, box, Blocks.BOOKSHELF.defaultBlockState(), cx + 1, y, cz - 3);
        put(level, box, Blocks.DARK_OAK_PLANKS.defaultBlockState(), cx - 2, y, cz - 2);
        put(level, box, Blocks.DARK_OAK_PLANKS.defaultBlockState(), cx - 1, y, cz - 2);
        put(level, box, Blocks.CANDLE.defaultBlockState().setValue(BlockStateProperties.LIT, true), cx - 2, y + 1, cz - 2);
        put(level, box, Blocks.POTTED_AZALEA.defaultBlockState(), cx - 1, y + 1, cz - 2);
        put(level, box, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH), cx - 2, y, cz - 1);
        put(level, box, Blocks.BREWING_STAND.defaultBlockState(), cx + 2, y, cz + 2);
        for (int x = cx - 1; x <= cx + 1; x++) {
            for (int z = cz; z <= cz + 2; z++) {
                put(level, box, Blocks.RED_CARPET.defaultBlockState(), x, y, z);
            }
        }
        put(level, box, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx, y + STOREY - 2, cz + 1);
    }

    /** The observatory: a quartz pillar with an amethyst on top, a "telescope" (a rod) pointing north, lanterns at its foot. */
    private void observatory(WorldGenLevel level, BoundingBox box) {
        int y = cy + 2 * STOREY + 1;
        put(level, box, Blocks.QUARTZ_PILLAR.defaultBlockState(), cx, y, cz);
        put(level, box, Blocks.QUARTZ_PILLAR.defaultBlockState(), cx, y + 1, cz);
        put(level, box, Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP), cx, y + 2, cz);
        put(level, box, Blocks.LIGHTNING_ROD.defaultBlockState().setValue(LightningRodBlock.FACING, Direction.NORTH), cx, y + 1, cz - 1);
        put(level, box, Blocks.LANTERN.defaultBlockState(), cx + 2, y, cz + 2);
        put(level, box, Blocks.LANTERN.defaultBlockState(), cx - 2, y, cz + 2);
    }

    /** Lanterns on posts along the main path and about the courtyard, so nothing spawns inside. */
    private void lanterns(WorldGenLevel level, BoundingBox box) {
        int[][] posts = {{-2, 8}, {2, 8}, {-2, 14}, {2, 14}, {-15, 3}, {15, 3}, {-15, -6}, {17, 0}, {-8, 17}, {8, 17}, {0, -10}, {-8, -2}, {8, -2}};
        for (int[] post : posts) {
            int x = cx + post[0];
            int z = cz + post[1];
            put(level, box, Blocks.SPRUCE_FENCE.defaultBlockState(), x, cy, z);
            put(level, box, Blocks.LANTERN.defaultBlockState(), x, cy + 1, z);
        }
    }
}
