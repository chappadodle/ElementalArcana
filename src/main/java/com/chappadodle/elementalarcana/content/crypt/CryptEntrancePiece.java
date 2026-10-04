package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.CryptLayout;
import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.Locale;

/**
 * A crypt's way in (see the Arcane Crypts spec): the mausoleum on the surface, 9 blocks square, its
 * door at the back, and the stair that runs from its middle down along the heading, one block down
 * for each block on, to the rooms' floor, then level to the entry hall's back wall 28 blocks out.
 * Coordinates here are (u, v): v along the heading, u to its right, from the mausoleum's middle.
 */
public class CryptEntrancePiece extends StructurePiece {
    private static final int HALF = 4;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final Element element;
    private final int mx;
    private final int ground;
    private final int mz;
    private final int heading;
    private final int floorY;

    public CryptEntrancePiece(Element element, int mx, int ground, int mz, int heading, int floorY) {
        super(ModCrypts.CRYPT_ENTRANCE.get(), 0, box(mx, ground, mz, heading, floorY));
        this.element = element;
        this.mx = mx;
        this.ground = ground;
        this.mz = mz;
        this.heading = heading;
        this.floorY = floorY;
        // No orientation: world coordinates, and the piece turns its own blocks (see CryptRoomPiece).
        setOrientation(null);
    }

    public CryptEntrancePiece(CompoundTag tag) {
        super(ModCrypts.CRYPT_ENTRANCE.get(), tag);
        this.element = CryptRoomPiece.readElement(tag);
        this.mx = tag.getInt("mx");
        this.ground = tag.getInt("ground");
        this.mz = tag.getInt("mz");
        this.heading = tag.getInt("heading");
        this.floorY = tag.getInt("floor");
        setOrientation(null);
    }

    /** The middle of the mausoleum's floor. */
    public BlockPos mausoleum() {
        return new BlockPos(mx, ground, mz);
    }

    /** The way the stair runs down, from the mausoleum toward the rooms. */
    public Direction heading() {
        return Direction.from2DDataValue(heading);
    }

    public Element element() {
        return element;
    }

    private static BoundingBox box(int mx, int ground, int mz, int heading, int floorY) {
        int[] a = world(mx, mz, heading, -HALF, -HALF - 3);
        int[] b = world(mx, mz, heading, HALF, CryptRules.STAIR_RUN - 1);
        return new BoundingBox(Math.min(a[0], b[0]), floorY - 2, Math.min(a[1], b[1]), Math.max(a[0], b[0]), ground + 10, Math.max(a[1], b[1]));
    }

    private static int[] world(int mx, int mz, int heading, int u, int v) {
        int right = CryptLayout.right(heading);
        return new int[]{mx + u * CryptLayout.dx(right) + v * CryptLayout.dx(heading), mz + u * CryptLayout.dz(right) + v * CryptLayout.dz(heading)};
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("element", element.name().toLowerCase(Locale.ROOT));
        tag.putInt("mx", mx);
        tag.putInt("ground", ground);
        tag.putInt("mz", mz);
        tag.putInt("heading", heading);
        tag.putInt("floor", floorY);
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockState state, int u, int y, int v) {
        int[] at = world(mx, mz, heading, u, v);
        placeBlock(level, state, at[0], y, at[1], box);
    }

    private BlockState get(WorldGenLevel level, BoundingBox box, int u, int y, int v) {
        int[] at = world(mx, mz, heading, u, v);
        return getBlock(level, at[0], y, at[1], box);
    }

    private Direction direction(int d) {
        return Direction.from2DDataValue(d);
    }

    /** A roof stair on a ring at (u, v): its tall side toward the middle, so the tiers step up. */
    private Direction inward(int u, int v, int ring) {
        if (v == ring) {
            return direction(CryptLayout.opposite(heading));
        }
        if (v == -ring) {
            return direction(heading);
        }
        return direction(u == ring ? CryptLayout.left(heading) : CryptLayout.right(heading));
    }

    /** A wall block, now and then cracked (decided by the block's place, so every chunk agrees). */
    private BlockState wall(int u, int y, int v) {
        int[] at = world(mx, mz, heading, u, v);
        return (Mth.getSeed(at[0], y, at[1]) & 7) == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        CryptPalette palette = CryptPalette.of(element);
        buildMausoleum(level, box, palette);
        buildStair(level, box);
    }

    private void buildMausoleum(WorldGenLevel level, BoundingBox box, CryptPalette palette) {
        for (int u = -HALF; u <= HALF; u++) {
            for (int v = -HALF; v <= HALF; v++) {
                // A footing down to the ground.
                for (int y = ground - 1; y > ground - 8; y--) {
                    BlockState below = get(level, box, u, y, v);
                    if (!below.isAir() && below.getFluidState().isEmpty() && !below.canBeReplaced()) {
                        break;
                    }
                    put(level, box, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), u, y, v);
                }
                boolean edge = Math.abs(u) == HALF || Math.abs(v) == HALF;
                boolean corner = Math.abs(u) == HALF && Math.abs(v) == HALF;
                put(level, box, edge ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState(), u, ground, v);
                for (int y = ground + 1; y <= ground + 4; y++) {
                    put(level, box, corner ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : edge ? wall(u, y, v) : AIR, u, y, v);
                }
                // The roof: a ring of the crypt's trim, then stepped tiers up to its light.
                put(level, box, edge ? palette.trim() : Blocks.DEEPSLATE_TILES.defaultBlockState(), u, ground + 5, v);
                for (int y = ground + 6; y <= ground + 10; y++) {
                    put(level, box, AIR, u, y, v);
                }
            }
        }
        for (int tier = 1; tier <= 3; tier++) {
            int ring = HALF - tier;
            int y = ground + 5 + tier;
            for (int u = -ring; u <= ring; u++) {
                for (int v = -ring; v <= ring; v++) {
                    boolean onRing = Math.abs(u) == ring || Math.abs(v) == ring;
                    boolean corner = Math.abs(u) == ring && Math.abs(v) == ring;
                    BlockState block = !onRing || corner || ring == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                            : Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, inward(u, v, ring));
                    put(level, box, block, u, y, v);
                }
            }
        }
        put(level, box, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 0, ground + 8, 0);
        put(level, box, palette.light(), 0, ground + 9, 0);
        for (int u = -HALF; u <= HALF; u += 2 * HALF) {
            for (int v = -HALF; v <= HALF; v += 2 * HALF) {
                put(level, box, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), u, ground + 6, v);
            }
        }
        // A short paved way up to the door, cut through any slope behind the mausoleum.
        for (int v = -HALF - 3; v < -HALF; v++) {
            for (int u = -2; u <= 2; u++) {
                for (int y = ground - 1; y > ground - 6; y--) {
                    BlockState below = get(level, box, u, y, v);
                    if (!below.isAir() && below.getFluidState().isEmpty() && !below.canBeReplaced()) {
                        break;
                    }
                    put(level, box, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), u, y, v);
                }
                put(level, box, Math.abs(u) == 2 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                        : (Mth.getSeed(u, v, ground) & 3) == 0 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                        : Blocks.DEEPSLATE_TILES.defaultBlockState(), u, ground, v);
                for (int y = ground + 1; y <= ground + 5; y++) {
                    put(level, box, AIR, u, y, v);
                }
            }
        }
        // Lantern posts where the way meets the door's path.
        for (int u = -2; u <= 2; u += 4) {
            put(level, box, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState(), u, ground + 1, -HALF - 3);
            put(level, box, Blocks.SOUL_LANTERN.defaultBlockState(), u, ground + 2, -HALF - 3);
        }
        // The door at the back, three wide, under a carved lintel.
        for (int u = -1; u <= 1; u++) {
            for (int y = ground + 1; y <= ground + 3; y++) {
                put(level, box, AIR, u, y, -HALF);
            }
            put(level, box, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), u, ground + 4, -HALF);
        }
        // Lanterns inside, and a plaque of the crypt's trim on the far wall.
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        put(level, box, lantern, -2, ground + 4, -2);
        put(level, box, lantern, 2, ground + 4, -2);
        put(level, box, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), 0, ground + 3, HALF);
        put(level, box, palette.trim(), -1, ground + 3, HALF);
        put(level, box, palette.trim(), 1, ground + 3, HALF);
        put(level, box, palette.candles(3), -3, ground + 1, 3);
        put(level, box, palette.candles(2), 3, ground + 1, 3);
    }

    private void buildStair(WorldGenLevel level, BoundingBox box) {
        Direction up = direction(CryptLayout.opposite(heading));
        for (int v = -1; v < CryptRules.STAIR_RUN; v++) {
            int step = CryptRules.stepY(ground, floorY, v);
            boolean inside = v < HALF;
            for (int u = -2; u <= 2; u++) {
                if (Math.abs(u) == 2) {
                    // The stairwell's walls, under the mausoleum's floor while inside it.
                    int top = inside ? Math.min(step + 4, ground - 1) : step + 4;
                    for (int y = step - 1; y <= top; y++) {
                        put(level, box, wall(u, y, v), u, y, v);
                    }
                    continue;
                }
                put(level, box, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), u, step - 1, v);
                put(level, box, step > floorY
                        ? Blocks.DEEPSLATE_TILE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, up)
                        : Blocks.DEEPSLATE_TILES.defaultBlockState(), u, step, v);
                for (int y = step + 1; y <= step + 3; y++) {
                    put(level, box, AIR, u, y, v);
                }
                if (step + 4 < ground) {
                    put(level, box, Blocks.DEEPSLATE_TILES.defaultBlockState(), u, step + 4, v);
                }
            }
            if (v > 4 && v % 6 == 0) {
                // Soul torches down the stair, on alternate sides.
                int u = (v / 6) % 2 == 0 ? -1 : 1;
                Direction away = direction(u < 0 ? CryptLayout.right(heading) : CryptLayout.left(heading));
                put(level, box, Blocks.SOUL_WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, away), u, step + 2, v);
            }
        }
        // A low rail round the stairwell's opening in the mausoleum's floor.
        BlockState rail = Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState();
        for (int v = 0; v <= 3; v++) {
            put(level, box, rail, -2, ground + 1, v);
            put(level, box, rail, 2, ground + 1, v);
        }
        for (int u = -1; u <= 1; u++) {
            put(level, box, rail, u, ground + 1, 3);
        }
    }
}
