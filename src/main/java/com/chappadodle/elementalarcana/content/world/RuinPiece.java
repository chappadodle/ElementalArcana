package com.chappadodle.elementalarcana.content.world;

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
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Locale;

/**
 * Builds a ruin (see the Ruins spec), Minecraft style: old stone brick, weathered as it's placed
 * (blocks missing, cracked, mossy, vines and the odd cobweb), in one of three layouts on a 9x9
 * footprint: a Fallen Arch, a Stone Circle or a Ruined Hall, each with a chest of old things
 * (chests/ruin). Weathering is decided from each block's world position, so a ruin across several
 * chunks comes out the same whichever chunk is built first. Local (0, 0, 0) is the footprint's
 * corner at ground level; the middle is (4, 4).
 */
public class RuinPiece extends StructurePiece {
    private static final int MID = 4;
    private static final int CLEAR_HEIGHT = 9;
    private static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/ruin"));

    public enum Layout {
        ARCH, CIRCLE, HALL
    }

    private final Layout layout;

    public RuinPiece(Layout layout, int x, int groundY, int z, Direction facing) {
        super(ModWorld.RUIN_PIECE.get(), 0, new BoundingBox(x, groundY - 2, z, x + 8, groundY + CLEAR_HEIGHT, z + 8));
        this.layout = layout;
        setOrientation(facing);
    }

    public RuinPiece(CompoundTag tag) {
        super(ModWorld.RUIN_PIECE.get(), tag);
        Layout read = Layout.ARCH;
        for (Layout candidate : Layout.values()) {
            if (candidate.name().toLowerCase(Locale.ROOT).equals(tag.getString("layout"))) {
                read = candidate;
            }
        }
        this.layout = read;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("layout", layout.name().toLowerCase(Locale.ROOT));
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        // A worn floor of old stone and grass, the air above it cleared of trees.
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                for (int y = 1; y <= CLEAR_HEIGHT; y++) {
                    placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                }
                fillColumnDown(level, Blocks.DIRT.defaultBlockState(), x, -1, z, box);
                double r = Math.hypot(x - MID, z - MID);
                placeBlock(level, r <= 4.2 && chance(x, 0, z, 70) ? oldStone(x, 0, z) : Blocks.GRASS_BLOCK.defaultBlockState(), x, 0, z, box);
            }
        }
        switch (layout) {
            case ARCH -> arch(level, box, random);
            case CIRCLE -> circle(level, box, random);
            default -> hall(level, box, random);
        }
        rubble(level, box);
    }

    /** Fallen Arch: two broken pillars, the arch half down between them, and a cracked altar with the chest. */
    private void arch(WorldGenLevel level, BoundingBox box, RandomSource random) {
        for (int y = 1; y <= 5; y++) {
            wall(level, box, 1, y, MID);
        }
        for (int y = 1; y <= 3; y++) {
            wall(level, box, 7, y, MID);
        }
        // What's left of the arch: across from the tall pillar, stopping short.
        for (int x = 2; x <= 4; x++) {
            wall(level, box, x, 5, MID);
        }
        placeBlock(level, stair(Direction.EAST, true), 5, 5, MID, box);
        // The altar, a step in front.
        placeBlock(level, Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), MID, 1, MID + 2, box);
        placeBlock(level, slab(), MID - 1, 1, MID + 2, box);
        placeBlock(level, slab(), MID + 1, 1, MID + 2, box);
        createChest(level, box, random, MID, 1, MID + 3, LOOT);
    }

    /** Stone Circle: a ring of broken pillars of uneven height round a sunken floor, the chest at the middle under a slab. */
    private void circle(WorldGenLevel level, BoundingBox box, RandomSource random) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 / 8 * i;
            int x = MID + (int) Math.round(Math.cos(angle) * 3.6);
            int z = MID + (int) Math.round(Math.sin(angle) * 3.6);
            int height = 1 + Math.floorMod(hash(x, 7, z), 4);
            for (int y = 1; y <= height; y++) {
                wall(level, box, x, y, z);
            }
        }
        for (int x = MID - 1; x <= MID + 1; x++) {
            for (int z = MID - 1; z <= MID + 1; z++) {
                placeBlock(level, oldStone(x, -1, z), x, -1, z, box);
                placeBlock(level, Blocks.AIR.defaultBlockState(), x, 0, z, box);
            }
        }
        createChest(level, box, random, MID, -1, MID, LOOT);
        placeBlock(level, slab(), MID, 0, MID, box);
    }

    /** Ruined Hall: the stumps of a small hall's walls, a fallen-in floor, a bookshelf or two and a lectern by the chest. */
    private void hall(WorldGenLevel level, BoundingBox box, RandomSource random) {
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                boolean edge = x == 1 || x == 7 || z == 1 || z == 7;
                if (!edge || z == 7 && (x == 3 || x == 4 || x == 5)) {
                    continue;
                }
                // Higher at the back, falling away to stumps at the front.
                int height = Math.max(1, 4 - z / 2 - Math.floorMod(hash(x, 3, z), 2));
                for (int y = 1; y <= height; y++) {
                    wall(level, box, x, y, z);
                }
            }
        }
        placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(), 2, 1, 2, box);
        placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(), 2, 2, 2, box);
        placeBlock(level, Blocks.BOOKSHELF.defaultBlockState(), 6, 1, 2, box);
        placeBlock(level, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, Direction.SOUTH), MID - 1, 1, 3, box);
        createChest(level, box, random, MID + 1, 1, 3, LOOT);
    }

    /** Fallen stones scattered round. */
    private void rubble(WorldGenLevel level, BoundingBox box) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                if (chance(x, 9, z, 7) && getBlock(level, x, 1, z, box).isAir()) {
                    placeBlock(level, chance(x, 10, z, 50) ? Blocks.COBBLESTONE.defaultBlockState() : slab(), x, 1, z, box);
                }
            }
        }
    }

    /** A block of old wall: stone brick, mostly cracked or mossy, sometimes gone, with vines and cobwebs about it. */
    private void wall(WorldGenLevel level, BoundingBox box, int x, int y, int z) {
        if (y > 1 && chance(x, y, z, 12)) {
            if (chance(x, y + 50, z, 15)) {
                placeBlock(level, Blocks.COBWEB.defaultBlockState(), x, y, z, box);
            }
            return;
        }
        placeBlock(level, oldStone(x, y, z), x, y, z, box);
        if (chance(x, y + 100, z, 18)) {
            Direction side = Direction.from2DDataValue(Math.floorMod(hash(x, y + 200, z), 4));
            BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(side.getOpposite()), true);
            int vx = x + side.getStepX();
            int vz = z + side.getStepZ();
            if (getBlock(level, vx, y, vz, box).isAir()) {
                placeBlock(level, vine, vx, y, vz, box);
            }
        }
    }

    /** Old stone: plain, cracked or mossy brick, now and then cobble or chiseled. */
    private BlockState oldStone(int x, int y, int z) {
        int roll = Math.floorMod(hash(x, y + 300, z), 100);
        if (roll < 30) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        if (roll < 55) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (roll < 65) {
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        if (roll < 68) {
            return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        }
        return Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static BlockState slab() {
        return Blocks.STONE_BRICK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
    }

    private static BlockState stair(Direction facing, boolean upsideDown) {
        return Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, upsideDown ? Half.TOP : Half.BOTTOM);
    }

    /** Whether the weathering at local (x, y, z) comes out within {@code percent} of 100: the same for every chunk. */
    private boolean chance(int x, int y, int z, int percent) {
        return Math.floorMod(hash(x, y, z), 100) < percent;
    }

    /** A hash of local (x, y, z)'s world position, so weathering doesn't depend on which chunk builds it. */
    private int hash(int x, int y, int z) {
        BlockPos world = getWorldPos(x, y, z);
        return (int) (Mth.getSeed(world.getX(), world.getY(), world.getZ()) >>> 16);
    }
}
