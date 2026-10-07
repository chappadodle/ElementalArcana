package com.chappadodle.elementalarcana.content.forge;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds a Cinder Forge (see the Cinder Forges spec) in world coordinates round its middle (cx, cz),
 * its floor at cy (40, over the Nether's lava sea): a round hall carved out of the netherrack under
 * a vault, on a basalt plinth down to the lava sea; its wall of blackstone bricks, with arches north
 * and south where tunnels run out to the Nether's caves, and iron bars east and west before two
 * storerooms with a chest each; polished basalt pillars, chains and soul lanterns, the forge's tools
 * between the pillars, a ring of lava crossed by four bridges, and in the middle, on a dais, the
 * Ember Anvil with the Forge Heart under it. Every choice comes from the forge's seed and each block's
 * place, so it comes out the same whichever chunk is built first.
 */
public class CinderForgePiece extends StructurePiece {
    private static final ResourceKey<LootTable> STORES = ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/cinder_forge"));
    /** The hall's radius; its wall stands just past it. */
    static final int HALL = 12;
    private static final double WALL = HALL + 1.5;
    private static final double LAVA_IN = 5.0;
    private static final double LAVA_OUT = 6.2;
    private static final double DAIS = 2.2;
    private static final double PILLARS = 10.5;
    /** How far the tunnels run out from the middle, how high the vault rises over the floor at the middle (8 at the wall), how deep the plinth goes. */
    private static final int REACH = 26;
    private static final int VAULT = 14;
    private static final int PLINTH = 9;

    private final long seed;
    private final int cx;
    private final int cy;
    private final int cz;

    public CinderForgePiece(long seed, int cx, int cy, int cz) {
        super(ModForge.CINDER_FORGE_PIECE.get(), 0, new BoundingBox(cx - REACH, cy - PLINTH, cz - REACH, cx + REACH, cy + VAULT + 1, cz + REACH));
        this.seed = seed;
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        setOrientation(null);
    }

    public CinderForgePiece(CompoundTag tag) {
        this(tag.getLong("seed"), tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putLong("seed", seed);
        tag.putInt("x", cx);
        tag.putInt("y", cy);
        tag.putInt("z", cz);
    }

    /** A roll out of {@code sides} decided by a block's place (and this forge's seed). */
    private int roll(int x, int y, int z, int sides) {
        return Math.floorMod((int) ((Mth.getSeed(x, y, z) ^ seed) >>> 16), sides);
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockState state, int x, int y, int z) {
        placeBlock(level, state, x, y, z, box);
    }

    private BlockState brick(int x, int y, int z) {
        return roll(x, y, z, 7) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    /** The vault's height over the floor at {@code d} from the middle: 14 there, 8 at the wall. */
    private int vault(double d) {
        return 8 + (int) Math.round((VAULT - 8) * Math.sqrt(Math.max(0, 1 - (d / WALL) * (d / WALL))));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            ChunkPos chunk, BlockPos pivot) {
        hall(level, box);
        tunnels(level, box);
        storeroom(level, box, 1);
        storeroom(level, box, -1);
        wall(level, box);
        lavaRing(level, box);
        dais(level, box);
        pillars(level, box);
        tools(level, box);
        lanterns(level, box);
    }

    /** The hall carved out under its vault, on a basalt plinth down to the lava sea; its floor of blackstone, a gilded ring in it. */
    private void hall(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - HALL - 2; x <= cx + HALL + 2; x++) {
            for (int z = cz - HALL - 2; z <= cz + HALL + 2; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > WALL) {
                    continue;
                }
                for (int y = cy - PLINTH; y < cy; y++) {
                    put(level, box, Blocks.BASALT.defaultBlockState(), x, y, z);
                }
                BlockState floor = d > HALL ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                        : d > 8.5 && d <= 9.5 ? Blocks.GILDED_BLACKSTONE.defaultBlockState()
                        : brick(x, cy, z);
                put(level, box, floor, x, cy, z);
                for (int y = cy + 1; y <= cy + vault(d); y++) {
                    put(level, box, Blocks.AIR.defaultBlockState(), x, y, z);
                }
            }
        }
    }

    /** Tunnels north and south out to the Nether's caves: three wide, four high, a floor of basalt where they cross open air. */
    private void tunnels(WorldGenLevel level, BoundingBox box) {
        for (int side = -1; side <= 1; side += 2) {
            for (int step = HALL + 1; step <= REACH; step++) {
                int z = cz + side * step;
                for (int x = cx - 1; x <= cx + 1; x++) {
                    put(level, box, Blocks.BASALT.defaultBlockState(), x, cy, z);
                    for (int y = cy + 1; y <= cy + 4; y++) {
                        put(level, box, Blocks.AIR.defaultBlockState(), x, y, z);
                    }
                }
            }
        }
    }

    /** A storeroom east ({@code side} 1) or west (-1) behind the wall's bars: blackstone all round, a chest at the far end, a lantern. */
    private void storeroom(WorldGenLevel level, BoundingBox box, int side) {
        int near = HALL + 2;
        int far = HALL + 6;
        for (int step = near - 1; step <= far + 1; step++) {
            int x = cx + side * step;
            for (int z = cz - 3; z <= cz + 3; z++) {
                for (int y = cy; y <= cy + 5; y++) {
                    boolean inside = step >= near && step <= far && Math.abs(z - cz) <= 2 && y > cy && y < cy + 5;
                    if (step == near - 1 && !inside) {
                        continue;
                    }
                    put(level, box, inside ? Blocks.AIR.defaultBlockState() : brick(x, y, z), x, y, z);
                }
            }
        }
        Direction facing = side > 0 ? Direction.WEST : Direction.EAST;
        int chestX = cx + side * far;
        // (createChest sets the loot only where there's no chest yet, so it places the chest itself)
        createChest(level, box, RandomSource.create(seed + side), new BlockPos(chestX, cy + 1, cz), STORES,
                Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        put(level, box, Blocks.LAVA_CAULDRON.defaultBlockState(), chestX, cy + 1, cz - 2);
        put(level, box, Blocks.BARREL.defaultBlockState(), chestX, cy + 1, cz + 2);
        put(level, box, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), cx + side * (near + 2), cy + 4, cz);
    }

    /** The wall round the hall: arches north and south to the tunnels, iron bars east and west before the storerooms. */
    private void wall(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - HALL - 2; x <= cx + HALL + 2; x++) {
            for (int z = cz - HALL - 2; z <= cz + HALL + 2; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d <= HALL + 0.3 || d > WALL) {
                    continue;
                }
                boolean arch = Math.abs(x - cx) <= 1;
                boolean bars = Math.abs(z - cz) <= 1;
                for (int y = cy + 1; y <= cy + 8; y++) {
                    BlockState state;
                    if (arch && y <= cy + 4) {
                        state = Blocks.AIR.defaultBlockState();
                    } else if (bars && y <= cy + 3) {
                        state = Blocks.IRON_BARS.defaultBlockState();
                    } else if ((arch && y == cy + 5) || (bars && y == cy + 4)) {
                        state = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
                    } else {
                        state = y == cy + 8 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : brick(x, y, z);
                    }
                    put(level, box, state, x, y, z);
                }
            }
        }
    }

    /** A ring of lava in the floor round the dais, crossed by four bridges. */
    private void lavaRing(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - 7; x <= cx + 7; x++) {
            for (int z = cz - 7; z <= cz + 7; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > LAVA_IN && d <= LAVA_OUT && Math.abs(x - cx) > 1 && Math.abs(z - cz) > 1) {
                    put(level, box, Blocks.LAVA.defaultBlockState(), x, cy, z);
                }
            }
        }
    }

    /** The dais in the middle, a block high, with the anvil on it and the Forge Heart under it. */
    private void dais(WorldGenLevel level, BoundingBox box) {
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                if (Math.hypot(x - cx, z - cz) <= DAIS) {
                    put(level, box, Blocks.POLISHED_BLACKSTONE.defaultBlockState(), x, cy + 1, z);
                }
            }
        }
        put(level, box, ModForge.EMBER_ANVIL.get().defaultBlockState().setValue(EmberAnvilBlock.FACING, Direction.NORTH), cx, cy + 2, cz);
        put(level, box, ModForge.FORGE_HEART.get().defaultBlockState(), cx, cy, cz);
    }

    /** Eight pillars of polished basalt round the hall, floor to vault. */
    private void pillars(WorldGenLevel level, BoundingBox box) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            int x = cx + (int) Math.round(Math.cos(angle) * PILLARS);
            int z = cz + (int) Math.round(Math.sin(angle) * PILLARS);
            int top = cy + vault(Math.hypot(x - cx, z - cz));
            for (int y = cy + 1; y <= top; y++) {
                put(level, box, Blocks.POLISHED_BASALT.defaultBlockState(), x, y, z);
            }
        }
    }

    /** Between the pillars, the forge's tools, each facing the middle: blast furnaces, smithing tables, cauldrons of lava, grindstones. */
    private void tools(WorldGenLevel level, BoundingBox box) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * (i + 0.5) / 8;
            int x = cx + (int) Math.round(Math.cos(angle) * 8.6);
            int z = cz + (int) Math.round(Math.sin(angle) * 8.6);
            Direction toMiddle = Direction.getNearest(cx - x, 0, cz - z);
            BlockState tool = switch (i % 4) {
                case 0 -> Blocks.BLAST_FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING, toMiddle);
                case 1 -> Blocks.SMITHING_TABLE.defaultBlockState();
                case 2 -> Blocks.LAVA_CAULDRON.defaultBlockState();
                default -> Blocks.GRINDSTONE.defaultBlockState().setValue(GrindstoneBlock.FACE, AttachFace.FLOOR)
                        .setValue(GrindstoneBlock.FACING, toMiddle);
            };
            put(level, box, tool, x, cy + 1, z);
        }
    }

    /** Chains down from the vault with soul lanterns at their ends, between the pillars, and four over the dais. */
    private void lanterns(WorldGenLevel level, BoundingBox box) {
        for (int i = 0; i < 12; i++) {
            boolean inner = i >= 8;
            double angle = inner ? Math.PI * 2 * (i - 8 + 0.5) / 4 : Math.PI * 2 * (i + 0.5) / 8;
            double reach = inner ? 3.5 : 7.0;
            int x = cx + (int) Math.round(Math.cos(angle) * reach);
            int z = cz + (int) Math.round(Math.sin(angle) * reach);
            int top = cy + vault(Math.hypot(x - cx, z - cz));
            int end = cy + (inner ? 8 : 6);
            for (int y = top; y > end; y--) {
                put(level, box, Blocks.CHAIN.defaultBlockState(), x, y, z);
            }
            put(level, box, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), x, end, z);
        }
    }
}
