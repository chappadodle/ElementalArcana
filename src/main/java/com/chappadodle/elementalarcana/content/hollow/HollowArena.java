package com.chappadodle.elementalarcana.content.hollow;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.HollowRules;
import com.chappadodle.elementalarcana.content.sanctum.ModSanctums;
import com.chappadodle.elementalarcana.content.sanctum.SanctumSealBlock;
import com.chappadodle.elementalarcana.content.sanctum.SealState;
import com.chappadodle.elementalarcana.content.world.ShrineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * The Ruin in the Hollow (see the Hollow spec), and what the Hollow remembers: an island of
 * blackstone and obsidian 49 blocks across hanging in the void, built the first time anyone
 * arrives, with veins of crying obsidian, four broken pillars on the diagonals each holding a
 * restored Seal of one element, and the maw in the middle (a ring of obsidian, then one of crying
 * obsidian, around a pit three deep, floored with crying obsidian) that the Hollow rises from. Saved with the level: which build
 * of the Ruin stands (an older one is cleared and built again), and when the Hollow next lets
 * everyone go or may rise again.
 */
public class HollowArena extends SavedData {
    public static final BlockPos CENTER = new BlockPos(0, 64, 0);
    /** Where arrivals land: the island's south edge, facing the maw. */
    public static final Vec3 ARRIVAL = new Vec3(0.5, 65, 19.5);
    /** Where the Hollow rises, in the maw. */
    public static final Vec3 MAW = new Vec3(0.5, 62, 0.5);
    private static final int RADIUS = 24;
    private static final int PIT = 4;
    /** The pillars stand on the diagonals, 17 blocks out: arrivals look past them at the maw. */
    private static final int PILLAR = 12;
    private static final String NAME = "elementalarcana_hollow";
    /** Which build of the Ruin is current: raise it when the Ruin changes. */
    private static final int VERSION = 4;
    /** How deep the maw's pit goes. */
    private static final int PIT_DEPTH = 3;

    private int version;
    private long releaseAt;
    private long nextRiseAt;

    public static HollowArena get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(HollowArena::new, HollowArena::load, null), NAME);
    }

    private static HollowArena load(CompoundTag tag, HolderLookup.Provider registries) {
        HollowArena arena = new HollowArena();
        // The first Ruins saved only that they were built.
        arena.version = tag.contains("version") ? tag.getInt("version") : tag.getBoolean("built") ? 1 : 0;
        arena.releaseAt = tag.getLong("release_at");
        arena.nextRiseAt = tag.getLong("next_rise_at");
        return arena;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", version);
        tag.putLong("release_at", releaseAt);
        tag.putLong("next_rise_at", nextRiseAt);
        return tag;
    }

    public long releaseAt() {
        return releaseAt;
    }

    public void setReleaseAt(long time) {
        releaseAt = time;
        setDirty();
    }

    public long nextRiseAt() {
        return nextRiseAt;
    }

    public void setNextRiseAt(long time) {
        nextRiseAt = time;
        setDirty();
    }

    public void ensureBuilt(ServerLevel level) {
        if (version < VERSION) {
            if (version > 0) {
                clear(level);
            }
            build(level);
            version = VERSION;
            setDirty();
        }
    }

    /** Clears an older Ruin away, so the current one can be built in its place. */
    private static void clear(ServerLevel level) {
        for (int x = -RADIUS - 3; x <= RADIUS + 3; x++) {
            for (int z = -RADIUS - 3; z <= RADIUS + 3; z++) {
                for (int y = -30; y <= 20; y++) {
                    BlockPos pos = CENTER.offset(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
    }

    private static void build(ServerLevel level) {
        RandomSource random = RandomSource.create(4242);
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                double r = Math.hypot(x, z);
                // A ragged rim, and the maw's pit: a floor three down, nothing above it.
                if (r > RADIUS - random.nextInt(3) + 0.5) {
                    continue;
                }
                if (r < PIT) {
                    set(level, x, -PIT_DEPTH, z, Blocks.CRYING_OBSIDIAN.defaultBlockState());
                    for (int y = -PIT_DEPTH - 1; y >= -PIT_DEPTH - 4; y--) {
                        set(level, x, y, z, Blocks.OBSIDIAN.defaultBlockState());
                    }
                    continue;
                }
                BlockState top;
                if (r < PIT + 2) {
                    top = Blocks.OBSIDIAN.defaultBlockState();
                    set(level, x, 1, z, Blocks.OBSIDIAN.defaultBlockState());
                } else if (r < PIT + 3) {
                    top = Blocks.CRYING_OBSIDIAN.defaultBlockState();
                } else {
                    float roll = random.nextFloat();
                    top = roll < 0.06f ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                            : roll < 0.4f ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                            : roll < 0.55f ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                            : Blocks.BLACKSTONE.defaultBlockState();
                }
                set(level, x, 0, z, top);
                // The underside tapers away into the void.
                int depth = (int) ((RADIUS - r) * 0.6) + random.nextInt(3);
                for (int y = -1; y >= -depth; y--) {
                    set(level, x, y, z, random.nextFloat() < 0.3f ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState());
                }
            }
        }
        // The four seals that bind it, on broken pillars at the compass points.
        int[][] spots = {{-PILLAR, -PILLAR}, {PILLAR, -PILLAR}, {PILLAR, PILLAR}, {-PILLAR, PILLAR}};
        Element[] elements = {Element.FIRE, Element.WATER, Element.WIND, Element.EARTH};
        for (int i = 0; i < spots.length; i++) {
            int height = 4 + random.nextInt(3);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int top = height - (Math.abs(dx) + Math.abs(dz) == 2 ? 1 + random.nextInt(2) : 0);
                    for (int y = 1; y <= top; y++) {
                        set(level, spots[i][0] + dx, y, spots[i][1] + dz, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
                    }
                }
            }
            set(level, spots[i][0], height + 1, spots[i][1], ModSanctums.SANCTUM_SEAL.get().defaultBlockState()
                    .setValue(SanctumSealBlock.KIND, ShrineKind.of(elements[i])).setValue(SanctumSealBlock.STATE, SealState.RESTORED));
        }
    }

    private static void set(ServerLevel level, int x, int y, int z, BlockState state) {
        level.setBlock(CENTER.offset(x, y, z), state, Block.UPDATE_CLIENTS);
    }

    /** Whether {@code pos} has fallen well below the island (off its edge, toward the void). */
    public static boolean fallen(Vec3 pos) {
        return pos.y < CENTER.getY() - 24;
    }

    /** Whether {@code pos} is on or over the island (within reach of the fight). */
    public static boolean near(Vec3 pos) {
        return pos.distanceTo(Vec3.atCenterOf(CENTER)) <= RADIUS + HollowRules.LEASH;
    }
}
