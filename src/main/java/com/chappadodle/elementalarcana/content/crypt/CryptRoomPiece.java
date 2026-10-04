package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CryptLayout;
import com.chappadodle.elementalarcana.api.CryptLayout.Kind;
import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.world.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

/**
 * One room of a crypt, or its burial chamber (see the Arcane Crypts spec), in deepslate with the
 * crypt's colours (CryptPalette). Rooms are 13 blocks square with their walls (11 inside), 5 high
 * inside; the chamber 25 and 9. A door is a 3-wide gap in the middle of a wall; a gate's way out is
 * filled with its seal, under its keystone. Coordinates here are (u, v): v along the room's way out,
 * u to its right, from the room's corner; y from its floor. Anything left to chance (glyphs,
 * cobwebs, cracks) is decided from the room's or the block's place, so every chunk builds the same.
 */
public class CryptRoomPiece extends StructurePiece {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final float GLYPH_SHARE = 0.55f;

    private final Kind kind;
    private final Element element;
    private final int forward;
    private final int doors;

    public CryptRoomPiece(Kind kind, Element element, int minX, int floorY, int minZ, int forward, int doors) {
        super(ModCrypts.CRYPT_ROOM.get(), 0, box(kind, minX, floorY, minZ));
        this.kind = kind;
        this.element = element;
        this.forward = forward;
        this.doors = doors;
        // No orientation: the piece places in world coordinates and turns its own blocks. (A vanilla
        // orientation would mirror every state's north and south.)
        setOrientation(null);
    }

    public CryptRoomPiece(CompoundTag tag) {
        super(ModCrypts.CRYPT_ROOM.get(), tag);
        Kind read = Kind.TOMBS;
        for (Kind candidate : Kind.values()) {
            if (candidate.name().equalsIgnoreCase(tag.getString("kind"))) {
                read = candidate;
            }
        }
        this.kind = read;
        this.element = readElement(tag);
        this.forward = tag.getInt("forward");
        this.doors = tag.getInt("doors");
        setOrientation(null);
    }

    static Element readElement(CompoundTag tag) {
        for (Element candidate : Element.values()) {
            if (candidate.name().equalsIgnoreCase(tag.getString("element"))) {
                return candidate;
            }
        }
        return Element.EARTH;
    }

    private static int sizeOf(Kind kind) {
        return kind == Kind.CHAMBER ? CryptRules.CHAMBER_SIZE : CryptLayout.CELL;
    }

    private static int heightOf(Kind kind) {
        return kind == Kind.CHAMBER ? CryptRules.CHAMBER_HEIGHT : CryptRules.ROOM_HEIGHT;
    }

    private static BoundingBox box(Kind kind, int minX, int floorY, int minZ) {
        int size = sizeOf(kind);
        return new BoundingBox(minX, floorY - 1, minZ, minX + size - 1, floorY + heightOf(kind), minZ + size - 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("kind", kind.name().toLowerCase(Locale.ROOT));
        tag.putString("element", element.name().toLowerCase(Locale.ROOT));
        tag.putInt("forward", forward);
        tag.putInt("doors", doors);
    }

    public Kind kind() {
        return kind;
    }

    /** Where someone stands in this room, a block above its floor in its middle: for tools and tests. */
    public BlockPos middle() {
        return new BlockPos(boundingBox.getCenter().getX(), boundingBox.minY() + 2, boundingBox.getCenter().getZ());
    }

    public Direction wayOut() {
        return Direction.from2DDataValue(forward);
    }

    // ---- The room's own coordinates ----

    private int size() {
        return sizeOf(kind);
    }

    private int height() {
        return heightOf(kind);
    }

    private int mid() {
        return (size() - 1) / 2;
    }

    private int lx(int u, int v) {
        int c = mid();
        return c + (u - c) * CryptLayout.dx(CryptLayout.right(forward)) + (v - c) * CryptLayout.dx(forward);
    }

    private int lz(int u, int v) {
        int c = mid();
        return c + (u - c) * CryptLayout.dz(CryptLayout.right(forward)) + (v - c) * CryptLayout.dz(forward);
    }

    private void put(WorldGenLevel level, BoundingBox box, BlockState state, int u, int y, int v) {
        BlockPos pos = worldPos(u, y, v);
        placeBlock(level, state, pos.getX(), pos.getY(), pos.getZ(), box);
    }

    private BlockPos worldPos(int u, int y, int v) {
        return new BlockPos(boundingBox.minX() + lx(u, v), boundingBox.minY() + 1 + y, boundingBox.minZ() + lz(u, v));
    }

    /** A number from the block's place in the world, the same whichever chunk asks. */
    private int hash(int u, int y, int v) {
        BlockPos pos = worldPos(u, y, v);
        return (int) (Mth.getSeed(pos.getX(), pos.getY(), pos.getZ()) >>> 16);
    }

    private boolean chance(int u, int y, int v, int oneIn) {
        return Math.floorMod(hash(u, y, v), oneIn) == 0;
    }

    private Direction forwardDir() {
        return Direction.from2DDataValue(forward);
    }

    private Direction backDir() {
        return Direction.from2DDataValue(CryptLayout.opposite(forward));
    }

    private Direction rightDir() {
        return Direction.from2DDataValue(CryptLayout.right(forward));
    }

    private Direction leftDir() {
        return Direction.from2DDataValue(CryptLayout.left(forward));
    }

    private boolean door(Direction side) {
        return (doors >> side.get2DDataValue() & 1) != 0;
    }

    // ---- Building ----

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox box, ChunkPos chunkPos, BlockPos pivot) {
        CryptPalette palette = CryptPalette.of(element);
        shell(level, box);
        switch (kind) {
            case ENTRY -> entry(level, box, random, palette);
            case TOMBS -> tombs(level, box, random, palette);
            case GLYPHS -> glyphs(level, box);
            case GATE -> gate(level, box, random, palette);
            case LIBRARY -> library(level, box, random, palette);
            case STORE -> store(level, box, random);
            case CHAMBER -> chamber(level, box, random, palette);
        }
        cobwebs(level, box);
    }

    private BlockState wallBlock(int u, int y, int v) {
        if (y == height() - 1) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        return chance(u, y, v, 6) ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    }

    private BlockState floorBlock(int u, int v) {
        int roll = Math.floorMod(hash(u, 0, v), 20);
        return roll < 3 ? Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                : roll == 3 ? Blocks.COBBLED_DEEPSLATE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState();
    }

    /** Floor, walls, ceiling and the doors. */
    private void shell(WorldGenLevel level, BoundingBox box) {
        int size = size();
        int top = height();
        for (int u = 0; u < size; u++) {
            for (int v = 0; v < size; v++) {
                boolean wall = u == 0 || v == 0 || u == size - 1 || v == size - 1;
                boolean corner = (u == 0 || u == size - 1) && (v == 0 || v == size - 1);
                put(level, box, Blocks.COBBLED_DEEPSLATE.defaultBlockState(), u, -1, v);
                put(level, box, floorBlock(u, v), u, 0, v);
                for (int y = 1; y < top; y++) {
                    put(level, box, !wall ? AIR : corner ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : wallBlock(u, y, v), u, y, v);
                }
                put(level, box, Blocks.DEEPSLATE_TILES.defaultBlockState(), u, top, v);
            }
        }
        int doorHeight = kind == Kind.CHAMBER ? 4 : 3;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!door(side)) {
                continue;
            }
            boolean sealed = kind == Kind.GATE && side == forwardDir();
            for (int along = -1; along <= 1; along++) {
                int[] at = wallSpot(side, mid() + along);
                for (int y = 1; y <= doorHeight; y++) {
                    put(level, box, sealed ? ModCrypts.RUNIC_SEAL.get().defaultBlockState() : AIR, at[0], y, at[1]);
                }
                put(level, box, along == 0 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState(),
                        at[0], doorHeight + 1, at[1]);
            }
            for (int along = -2; along <= 2; along += 4) {
                int[] at = wallSpot(side, mid() + along);
                for (int y = 1; y <= doorHeight; y++) {
                    put(level, box, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), at[0], y, at[1]);
                }
            }
            if (sealed) {
                int[] at = wallSpot(side, mid());
                put(level, box, ModCrypts.RUNE_LOCK.get().defaultBlockState().setValue(RuneLockBlock.FACING, backDir()), at[0], doorHeight + 1, at[1]);
            }
        }
    }

    /** The (u, v) of the spot {@code along} blocks along the wall on {@code side} (counted from its left end, facing it from inside). */
    private int[] wallSpot(Direction side, int along) {
        int last = size() - 1;
        if (side == forwardDir()) {
            return new int[]{along, last};
        }
        if (side == backDir()) {
            return new int[]{along, 0};
        }
        if (side == rightDir()) {
            return new int[]{last, along};
        }
        return new int[]{0, along};
    }

    private void lantern(WorldGenLevel level, BoundingBox box, int u, int v) {
        put(level, box, Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true), u, height() - 1, v);
    }

    private void cobwebs(WorldGenLevel level, BoundingBox box) {
        int last = size() - 2;
        int y = height() - 1;
        for (int[] corner : new int[][]{{1, 1}, {last, 1}, {1, last}, {last, last}}) {
            if (kind == Kind.LIBRARY || chance(corner[0], y, corner[1], 2)) {
                put(level, box, Blocks.COBWEB.defaultBlockState(), corner[0], y, corner[1]);
            }
        }
    }

    private ResourceKey<LootTable> loot(String name) {
        return ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/" + name));
    }

    private ResourceKey<LootTable> elementLoot(String name) {
        return loot(name + "_" + element.name().toLowerCase(Locale.ROOT));
    }

    /** An urn (a decorated pot) holding a little, on the floor at (u, v). */
    private void urn(WorldGenLevel level, BoundingBox box, RandomSource random, int u, int v) {
        BlockPos pos = worldPos(u, 1, v);
        if (!box.isInside(pos)) {
            return;
        }
        Direction facing = Direction.from2DDataValue(Math.floorMod(hash(u, 1, v), 4));
        level.setBlock(pos, Blocks.DECORATED_POT.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, pos, elementLoot("crypt_urn"));
    }

    private void chest(WorldGenLevel level, BoundingBox box, RandomSource random, int u, int y, int v, ResourceKey<LootTable> table) {
        BlockPos pos = worldPos(u, y, v);
        if (box.isInside(pos)) {
            createChest(level, box, random, pos, table, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, backDir()));
        }
    }

    private BlockState coffin(Direction facing, DoubleBlockHalf half, boolean sealed) {
        return ModCrypts.COFFIN.get().defaultBlockState().setValue(CoffinBlock.FACING, facing).setValue(CoffinBlock.HALF, half)
                .setValue(CoffinBlock.SEALED, sealed).setValue(CoffinBlock.ELEMENT, ShrineKind.of(element));
    }

    /** An upright coffin set in the wall at (u, v), its lid toward {@code facing}, under a carved lintel. */
    private void coffinAt(WorldGenLevel level, BoundingBox box, int u, int v, Direction facing, boolean sealed) {
        put(level, box, coffin(facing, DoubleBlockHalf.LOWER, sealed), u, 1, v);
        put(level, box, coffin(facing, DoubleBlockHalf.UPPER, sealed), u, 2, v);
        put(level, box, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), u, 3, v);
    }

    private void entry(WorldGenLevel level, BoundingBox box, RandomSource random, CryptPalette palette) {
        int c = mid();
        int last = size() - 1;
        for (int u = c - 2; u <= c + 2; u++) {
            for (int v = c - 2; v <= c + 2; v++) {
                if (Math.abs(u - c) == 2 || Math.abs(v - c) == 2) {
                    put(level, box, palette.trim(), u, 0, v);
                }
            }
        }
        put(level, box, palette.light(), c, 0, c);
        for (int u : new int[]{2, last - 2}) {
            // The wardens' statues, flanking the way on.
            put(level, box, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), u, 1, last - 2);
            put(level, box, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState(), u, 2, last - 2);
            put(level, box, Blocks.CHISELED_DEEPSLATE.defaultBlockState(), u, 3, last - 2);
            put(level, box, palette.trim(), u, 4, last - 2);
        }
        urn(level, box, random, 1, 1);
        urn(level, box, random, last - 1, 1);
        lantern(level, box, c, c - 3);
        lantern(level, box, c, c + 3);
    }

    private void tombs(WorldGenLevel level, BoundingBox box, RandomSource random, CryptPalette palette) {
        int c = mid();
        int last = size() - 1;
        for (int v : new int[]{3, last - 3}) {
            coffinAt(level, box, 0, v, rightDir(), false);
            coffinAt(level, box, last, v, leftDir(), false);
        }
        // A stone bier with candles in the middle.
        for (int v = c - 1; v <= c + 1; v++) {
            put(level, box, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), c, 1, v);
        }
        put(level, box, palette.candles(3), c, 2, c - 1);
        put(level, box, palette.candles(2), c, 2, c + 1);
        for (int[] corner : new int[][]{{1, 1}, {last - 1, 1}, {1, last - 1}, {last - 1, last - 1}}) {
            if (chance(corner[0], 1, corner[1], 2)) {
                urn(level, box, random, corner[0], corner[1]);
            }
        }
        lantern(level, box, c - 3, c);
        lantern(level, box, c + 3, c);
    }

    private void glyphs(WorldGenLevel level, BoundingBox box) {
        int last = size() - 1;
        int[][] pillars = {{3, 3}, {last - 3, 3}, {3, last - 3}, {last - 3, last - 3}};
        for (int[] pillar : pillars) {
            for (int y = 1; y < height(); y++) {
                put(level, box, y == 1 || y == height() - 1 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
                        : Blocks.POLISHED_DEEPSLATE.defaultBlockState(), pillar[0], y, pillar[1]);
            }
        }
        BlockState glyph = ModCrypts.GLYPH.get().defaultBlockState().setValue(GlyphBlock.ELEMENT, ShrineKind.of(element));
        boolean[][] map = glyphMap(pillars);
        for (int u = 1; u < last; u++) {
            for (int v = 1; v < last; v++) {
                if (map[u][v]) {
                    put(level, box, glyph, u, 1, v);
                }
            }
        }
        lantern(level, box, mid() - 3, mid());
        lantern(level, box, mid() + 3, mid());
    }

    /**
     * Where the glyphs lie: a safe path, by random-weighted shortest ways, from the first door to every
     * other door, the tiles just inside each door, the pillars, and GLYPH_SHARE of the rest.
     */
    boolean[][] glyphMap(int[][] pillars) {
        int size = size();
        int last = size - 1;
        RandomSource random = RandomSource.create(Mth.getSeed(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ()));
        boolean[][] blocked = new boolean[size][size];
        for (int[] pillar : pillars) {
            blocked[pillar[0]][pillar[1]] = true;
        }
        boolean[][] safe = new boolean[size][size];
        List<int[]> doorTiles = new ArrayList<>();
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!door(side)) {
                continue;
            }
            for (int along = -1; along <= 1; along++) {
                int[] at = wallSpot(side, mid() + along);
                int u = Mth.clamp(at[0], 1, last - 1);
                int v = Mth.clamp(at[1], 1, last - 1);
                safe[u][v] = true;
                if (along == 0) {
                    doorTiles.add(new int[]{u, v});
                }
            }
        }
        int[][] weight = new int[size][size];
        for (int[] row : weight) {
            for (int i = 0; i < row.length; i++) {
                row[i] = 1 + random.nextInt(5);
            }
        }
        for (int i = 1; i < doorTiles.size(); i++) {
            for (int[] tile : path(doorTiles.get(0), doorTiles.get(i), weight, blocked)) {
                safe[tile[0]][tile[1]] = true;
            }
        }
        boolean[][] glyphs = new boolean[size][size];
        for (int u = 1; u < last; u++) {
            for (int v = 1; v < last; v++) {
                glyphs[u][v] = !safe[u][v] && !blocked[u][v] && random.nextFloat() < GLYPH_SHARE;
            }
        }
        return glyphs;
    }

    /** The cheapest way across the room's floor from one tile to another, round blocked ones. */
    private List<int[]> path(int[] from, int[] to, int[][] weight, boolean[][] blocked) {
        int size = size();
        int[][] cost = new int[size][size];
        for (int[] row : cost) {
            Arrays.fill(row, Integer.MAX_VALUE);
        }
        int[][][] previous = new int[size][size][];
        PriorityQueue<int[]> queue = new PriorityQueue<>((a, b) -> Integer.compare(a[2], b[2]));
        cost[from[0]][from[1]] = 0;
        queue.add(new int[]{from[0], from[1], 0});
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] here = queue.poll();
            if (here[2] > cost[here[0]][here[1]]) {
                continue;
            }
            if (here[0] == to[0] && here[1] == to[1]) {
                break;
            }
            for (int[] step : steps) {
                int u = here[0] + step[0];
                int v = here[1] + step[1];
                if (u < 1 || v < 1 || u > size - 2 || v > size - 2 || blocked[u][v]) {
                    continue;
                }
                int next = here[2] + weight[u][v];
                if (next < cost[u][v]) {
                    cost[u][v] = next;
                    previous[u][v] = new int[]{here[0], here[1]};
                    queue.add(new int[]{u, v, next});
                }
            }
        }
        List<int[]> tiles = new ArrayList<>();
        int[] at = to;
        while (at != null) {
            tiles.add(at);
            at = previous[at[0]][at[1]];
        }
        return tiles;
    }

    private void gate(WorldGenLevel level, BoundingBox box, RandomSource random, CryptPalette palette) {
        int c = mid();
        int last = size() - 1;
        // The gate's guardians, beside the seal: they wake when it opens.
        coffinAt(level, box, c - 3, last, backDir(), true);
        coffinAt(level, box, c + 3, last, backDir(), true);
        // Three runestones on pedestals, candles on top.
        BlockState rune = ModCrypts.RUNESTONE.get().defaultBlockState().setValue(RunestoneBlock.ELEMENT, ShrineKind.of(element));
        for (int[] spot : new int[][]{{3, 4}, {last - 3, 4}, {c, last - 3}}) {
            put(level, box, Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState(), spot[0], 1, spot[1]);
            put(level, box, rune, spot[0], 2, spot[1]);
            put(level, box, palette.candles(2), spot[0], 3, spot[1]);
        }
        urn(level, box, random, 1, 1);
        urn(level, box, random, last - 1, 1);
        lantern(level, box, c - 3, c);
        lantern(level, box, c + 3, c);
    }

    private void library(WorldGenLevel level, BoundingBox box, RandomSource random, CryptPalette palette) {
        int c = mid();
        int last = size() - 1;
        for (int v = 2; v <= last - 2; v++) {
            for (int u : new int[]{1, last - 1}) {
                put(level, box, Blocks.BOOKSHELF.defaultBlockState(), u, 1, v);
                put(level, box, Blocks.BOOKSHELF.defaultBlockState(), u, 2, v);
                if (v % 3 == 0) {
                    put(level, box, palette.candles(1 + Math.floorMod(hash(u, 3, v), 3)), u, 3, v);
                }
            }
        }
        for (int u = 2; u <= last - 2; u++) {
            if (Math.abs(u - c) <= 1) {
                continue;
            }
            put(level, box, Blocks.BOOKSHELF.defaultBlockState(), u, 1, last - 1);
            put(level, box, Blocks.BOOKSHELF.defaultBlockState(), u, 2, last - 1);
        }
        put(level, box, Blocks.LECTERN.defaultBlockState().setValue(LecternBlock.FACING, backDir()), c, 1, c);
        chest(level, box, random, c, 1, last - 1, loot("crypt_library"));
        put(level, box, palette.candles(3), c - 1, 1, last - 1);
        put(level, box, palette.candles(2), c + 1, 1, last - 1);
        lantern(level, box, c, c - 3);
    }

    private void store(WorldGenLevel level, BoundingBox box, RandomSource random) {
        int c = mid();
        int last = size() - 1;
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP);
        for (int u : new int[]{1, last - 1}) {
            for (int v = 2; v <= last - 2; v++) {
                if (Math.abs(v - c) <= 1) {
                    continue;
                }
                put(level, box, barrel, u, 1, v);
                if (chance(u, 2, v, 2)) {
                    put(level, box, barrel, u, 2, v);
                }
            }
        }
        for (int[] spot : new int[][]{{1, 2}, {last - 1, last - 2}}) {
            BlockPos pos = worldPos(spot[0], 1, spot[1]);
            if (box.isInside(pos)) {
                RandomizableContainer.setBlockEntityLootTable(level, random, pos, elementLoot("crypt_urn"));
            }
        }
        urn(level, box, random, c - 2, last - 1);
        urn(level, box, random, c + 2, last - 1);
        chest(level, box, random, c, 1, last - 1, elementLoot("crypt_store"));
        lantern(level, box, c, c);
    }

    private void chamber(WorldGenLevel level, BoundingBox box, RandomSource random, CryptPalette palette) {
        int c = mid();
        int last = size() - 1;
        int top = height();
        // The aisle from the door to the dais, edged with the crypt's trim and lights.
        for (int v = 1; v <= 14; v++) {
            for (int u = c - 2; u <= c + 2; u++) {
                put(level, box, Blocks.POLISHED_DEEPSLATE.defaultBlockState(), u, 0, v);
            }
            BlockState edge = v % 4 == 3 ? palette.light() : palette.trim();
            put(level, box, edge, c - 3, 0, v);
            put(level, box, edge, c + 3, 0, v);
        }
        for (int u : new int[]{6, last - 6}) {
            for (int v : new int[]{4, 9, 14, 19}) {
                for (int y = 1; y < top; y++) {
                    put(level, box, y == 1 || y == top - 1 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
                            : Blocks.POLISHED_DEEPSLATE.defaultBlockState(), u, y, v);
                }
            }
        }
        for (int v : new int[]{4, 8, 12, 18}) {
            put(level, box, palette.light(), c, top, v);
        }
        for (int u : new int[]{c - 3, c + 3}) {
            for (int v : new int[]{6, 12}) {
                lantern(level, box, u, v);
            }
        }
        // The dais, a step up at its front.
        for (int u = 7; u <= last - 7; u++) {
            for (int v = 15; v <= last - 1; v++) {
                put(level, box, v == 15 ? Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, forwardDir())
                        : Blocks.POLISHED_DEEPSLATE.defaultBlockState(), u, 1, v);
            }
        }
        // The Revenant's tomb, its grave flame at the head.
        for (int u = c - 1; u <= c + 1; u++) {
            for (int v = 17; v <= 19; v++) {
                put(level, box, u == c && v == 18 ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState(), u, 2, v);
                put(level, box, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM), u, 3, v);
            }
        }
        put(level, box, ModCrypts.GRAVE_FLAME.get().defaultBlockState().setValue(GraveFlameBlock.FACING, backDir())
                .setValue(GraveFlameBlock.ELEMENT, ShrineKind.of(element)), c, 2, 21);
        for (int[] spot : new int[][]{{8, 16}, {last - 8, 16}, {8, 22}, {last - 8, 22}}) {
            put(level, box, palette.candles(4), spot[0], 2, spot[1]);
        }
        chest(level, box, random, c - 3, 2, 22, elementLoot("crypt_reliquary"));
        chest(level, box, random, c + 3, 2, 22, elementLoot("crypt_reliquary"));
        // The chamber's dead, sealed in the side walls until the Revenant calls them.
        for (int v : new int[]{6, 12}) {
            coffinAt(level, box, 0, v, rightDir(), true);
            coffinAt(level, box, last, v, leftDir(), true);
        }
        urn(level, box, random, 1, 1);
        urn(level, box, random, last - 1, 1);
        urn(level, box, random, 1, last - 1);
        urn(level, box, random, last - 1, last - 1);
    }
}
