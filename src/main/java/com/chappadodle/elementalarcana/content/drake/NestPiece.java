package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.world.ShrineKind;
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
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.Locale;

/**
 * Builds a drake's nest (see the Drakes spec), Minecraft style: a round hollow 11 blocks across,
 * lined with gravel, coarse earth, bones and the drake's element's stuff (magma, snow and ice,
 * copper, calcite), ringed with tumbled stone and charred logs; its hoard in the middle (a chest
 * with gold beside it) and the unseen heart that calls its drake. World coordinates, no vanilla
 * orientation (see CryptRoomPiece); anything left to chance is decided from each block's place.
 */
public class NestPiece extends StructurePiece {
    private static final int SIZE = 13;
    private static final int MID = 6;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final Element element;

    public NestPiece(Element element, int x, int ground, int z) {
        super(ModDrakes.NEST_PIECE.get(), 0, new BoundingBox(x, ground - 5, z, x + SIZE - 1, ground + 7, z + SIZE - 1));
        this.element = element;
        setOrientation(null);
    }

    public NestPiece(CompoundTag tag) {
        super(ModDrakes.NEST_PIECE.get(), tag);
        Element read = Element.FIRE;
        for (Element candidate : Element.values()) {
            if (candidate.name().equalsIgnoreCase(tag.getString("element"))) {
                read = candidate;
            }
        }
        this.element = read;
        setOrientation(null);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("element", element.name().toLowerCase(Locale.ROOT));
    }

    private int ground() {
        return boundingBox.minY() + 5;
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockState state, int u, int y, int v) {
        placeBlock(level, state, boundingBox.minX() + u, ground() + y, boundingBox.minZ() + v, box);
    }

    private BlockState get(WorldGenLevel level, BoundingBox box, int u, int y, int v) {
        return getBlock(level, boundingBox.minX() + u, ground() + y, boundingBox.minZ() + v, box);
    }

    private int roll(int u, int y, int v, int sides) {
        return Math.floorMod((int) (Mth.getSeed(boundingBox.minX() + u, ground() + y, boundingBox.minZ() + v) >>> 16), sides);
    }

    /** The drake's own stuff in its nest. */
    private BlockState accent(int u, int v) {
        return switch (element) {
            case FIRE -> roll(u, 0, v, 2) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState();
            case ICE -> roll(u, 0, v, 2) == 0 ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.SNOW_BLOCK.defaultBlockState();
            case LIGHTNING -> roll(u, 0, v, 2) == 0 ? Blocks.RAW_COPPER_BLOCK.defaultBlockState() : Blocks.TUFF.defaultBlockState();
            default -> roll(u, 0, v, 2) == 0 ? Blocks.CALCITE.defaultBlockState() : Blocks.WHITE_TERRACOTTA.defaultBlockState();
        };
    }

    private BlockState floor(int u, int v) {
        int pick = roll(u, 0, v, 12);
        if (pick < 3) {
            return accent(u, v);
        }
        if (pick < 6) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        if (pick < 8) {
            return Blocks.BONE_BLOCK.defaultBlockState();
        }
        return element == Element.ICE ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
    }

    private BlockState stone(int u, int y, int v) {
        return switch (roll(u, y, v, 5)) {
            case 0 -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case 1 -> Blocks.ANDESITE.defaultBlockState();
            case 2 -> accent(u, v);
            default -> Blocks.COBBLESTONE.defaultBlockState();
        };
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        for (int u = 0; u < SIZE; u++) {
            for (int v = 0; v < SIZE; v++) {
                double r = Math.hypot(u - MID, v - MID);
                if (r > 6.5) {
                    continue;
                }
                for (int y = -1; y >= -5; y--) {
                    BlockState below = get(level, box, u, y, v);
                    if (!below.isAir() && below.getFluidState().isEmpty() && !below.canBeReplaced()) {
                        break;
                    }
                    put(level, box, Blocks.COBBLESTONE.defaultBlockState(), u, y, v);
                }
                for (int y = 1; y <= 7; y++) {
                    put(level, box, AIR, u, y, v);
                }
                if (r <= 5) {
                    // The hollow: a step lower toward the middle.
                    put(level, box, floor(u, v), u, 0, v);
                    if (r <= 3) {
                        put(level, box, floor(u, v + 7), u, -1, v);
                        put(level, box, AIR, u, 0, v);
                    }
                } else {
                    put(level, box, stone(u, 0, v), u, 0, v);
                    int height = 1 + roll(u, 1, v, 3);
                    for (int y = 1; y <= height && height > 1; y++) {
                        put(level, box, stone(u, y, v), u, y, v);
                    }
                }
            }
        }
        // Charred logs fallen across the rim, and bones in the hollow.
        put(level, box, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 2, 1, 4);
        put(level, box, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X), 3, 1, 4);
        put(level, box, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 9, 1, 8);
        put(level, box, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z), 9, 1, 9);
        put(level, box, Blocks.BONE_BLOCK.defaultBlockState(), 4, 0, 8);
        put(level, box, Blocks.BONE_BLOCK.defaultBlockState(), 8, 1, 3);
        // The hoard, sunk in the middle, gold beside it, and the heart above.
        BlockPos chest = new BlockPos(boundingBox.minX() + MID, ground(), boundingBox.minZ() + MID);
        if (box.isInside(chest)) {
            createChest(level, box, random, chest, ResourceKey.create(Registries.LOOT_TABLE,
                            ElementalArcana.id("chests/drake_hoard_" + element.name().toLowerCase(Locale.ROOT))),
                    Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH));
        }
        put(level, box, Blocks.RAW_GOLD_BLOCK.defaultBlockState(), MID - 1, 0, MID + 1);
        put(level, box, Blocks.GOLD_BLOCK.defaultBlockState(), MID + 1, 0, MID - 1);
        put(level, box, ModDrakes.NEST_HEART.get().defaultBlockState().setValue(NestHeartBlock.ELEMENT, ShrineKind.of(element)), MID, 2, MID);
    }
}
