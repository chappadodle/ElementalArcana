package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.ElementalArcana;
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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Builds a shrine, Minecraft style: a round platform of its element's stone (11 blocks across) on a
 * foundation, with the Shrine Core on a pedestal at the middle, pillars (or, for Earth, a ring of
 * standing stones) and an offering chest. Local coordinates: the middle is (5, 5), the platform is
 * at local y 0, the bottom of the piece, so the game's terrain blending (beard_thin) fills the ground
 * below it and slopes the ground above it away, instead of leaving it in a pit.
 */
public class ShrinePiece extends StructurePiece {
    private static final int GROUND = 0;
    private static final int MID = 5;

    /** The blocks a shrine is made of. */
    private record Palette(BlockState floor, BlockState trim, BlockState inner, BlockState pillar, BlockState cap,
                           BlockState pedestal, BlockState foundation) {
    }

    private final ShrineKind kind;

    public ShrinePiece(ShrineKind kind, int x, int groundY, int z, Direction facing) {
        super(ModWorld.SHRINE_PIECE.get(), 0, new BoundingBox(x, groundY, z, x + 10, groundY + 8, z + 10));
        this.kind = kind;
        setOrientation(facing);
    }

    public ShrinePiece(CompoundTag tag) {
        super(ModWorld.SHRINE_PIECE.get(), tag);
        ShrineKind read = ShrineKind.FIRE;
        for (ShrineKind candidate : ShrineKind.values()) {
            if (candidate.getSerializedName().equals(tag.getString("kind"))) {
                read = candidate;
            }
        }
        this.kind = read;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("kind", kind.getSerializedName());
    }

    private Palette palette() {
        return switch (kind) {
            case FIRE -> new Palette(Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState(), Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    Blocks.RED_NETHER_BRICKS.defaultBlockState(), Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                    Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true), Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState(),
                    Blocks.BLACKSTONE.defaultBlockState());
            case WATER -> new Palette(Blocks.PRISMARINE_BRICKS.defaultBlockState(), Blocks.DARK_PRISMARINE.defaultBlockState(),
                    Blocks.WATER.defaultBlockState(), Blocks.PRISMARINE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(),
                    Blocks.DARK_PRISMARINE.defaultBlockState(), Blocks.PRISMARINE_BRICKS.defaultBlockState());
            case ICE -> new Palette(Blocks.PACKED_ICE.defaultBlockState(), Blocks.BLUE_ICE.defaultBlockState(),
                    Blocks.SNOW_BLOCK.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState(), Blocks.SEA_LANTERN.defaultBlockState(),
                    Blocks.BLUE_ICE.defaultBlockState(), Blocks.PACKED_ICE.defaultBlockState());
            case WIND -> new Palette(Blocks.CALCITE.defaultBlockState(), Blocks.SMOOTH_QUARTZ.defaultBlockState(),
                    Blocks.POLISHED_DIORITE.defaultBlockState(), Blocks.CALCITE.defaultBlockState(), Blocks.END_ROD.defaultBlockState(),
                    Blocks.QUARTZ_PILLAR.defaultBlockState(), Blocks.STONE.defaultBlockState());
            case EARTH -> new Palette(Blocks.MOSS_BLOCK.defaultBlockState(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(),
                    Blocks.ROOTED_DIRT.defaultBlockState(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), Blocks.CHISELED_STONE_BRICKS.defaultBlockState(),
                    Blocks.CHISELED_STONE_BRICKS.defaultBlockState(), Blocks.STONE.defaultBlockState());
        };
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        Palette palette = palette();
        for (int x = 0; x <= 10; x++) {
            for (int z = 0; z <= 10; z++) {
                double r = Math.hypot(x - MID, z - MID);
                if (r > 5.4) {
                    continue;
                }
                // Room above the platform, then the platform on a foundation.
                for (int y = GROUND + 1; y <= GROUND + 8; y++) {
                    placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, box);
                }
                fillColumnDown(level, palette.foundation(), x, GROUND - 1, z, box);
                BlockState top;
                if (r >= 4.5) {
                    top = palette.trim();
                } else if (r <= 2.5 && r > 1.0) {
                    top = palette.inner();
                } else {
                    top = palette.floor();
                }
                placeBlock(level, top, x, GROUND, z, box);
            }
        }

        // The core on its pedestal.
        placeBlock(level, palette.pedestal(), MID, GROUND + 1, MID, box);
        placeBlock(level, ModWorld.SHRINE_CORE.get().defaultBlockState().setValue(ShrineCoreBlock.KIND, kind), MID, GROUND + 2, MID, box);

        if (kind == ShrineKind.EARTH) {
            standingStones(level, box, random, palette);
        } else {
            for (int[] at : new int[][]{{2, 2}, {8, 2}, {2, 8}, {8, 8}}) {
                for (int y = GROUND + 1; y <= GROUND + 3; y++) {
                    placeBlock(level, palette.pillar(), at[0], y, at[1], box);
                }
                placeBlock(level, palette.cap(), at[0], GROUND + 4, at[1], box);
            }
        }

        createChest(level, box, random, MID, GROUND + 1, MID + 3, lootTable());
    }

    /** Earth: six standing stones of uneven height in a ring, like an old henge. */
    private void standingStones(WorldGenLevel level, BoundingBox box, RandomSource random, Palette palette) {
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 / 6 * i + Math.PI / 6;
            int x = MID + (int) Math.round(Math.cos(angle) * 4);
            int z = MID + (int) Math.round(Math.sin(angle) * 4);
            int height = 3 + random.nextInt(2);
            for (int y = GROUND + 1; y <= GROUND + height; y++) {
                placeBlock(level, palette.pillar(), x, y, z, box);
            }
            placeBlock(level, palette.cap(), x, GROUND + height + 1, z, box);
        }
    }

    private ResourceKey<LootTable> lootTable() {
        return ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/shrine_" + kind.getSerializedName()));
    }
}
