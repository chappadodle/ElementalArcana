package com.chappadodle.elementalarcana.api;

import java.util.Arrays;

/**
 * Tsunami's path (see the Tsunami spec): the height a wave's foot rolls at along a straight line,
 * a step every half block, and how far it gets. It climbs a step of one block, pours down a drop of
 * up to three, and ends against anything taller or deeper; never past {@link #MAX_LENGTH} blocks.
 * The world is asked through {@link Ground}, so these rules don't touch it.
 */
public final class TsunamiRules {
    /** Blocks between two points of the path (the wave rolls one a tick). */
    public static final double STEP = 0.5;
    public static final double MAX_LENGTH = 16;
    /** The least way the wave needs to rise at all. */
    public static final double MIN_LENGTH = 2;
    public static final int MAX_CLIMB = 1;
    public static final int MAX_DROP = 3;

    private TsunamiRules() {
    }

    /** What the wave rolls on and breaks against: whether the block at (x, y, z) holds it up (ground, a wall, or water to ride). */
    @FunctionalInterface
    public interface Ground {
        boolean solid(int x, int y, int z);
    }

    /**
     * The foot heights (the block a wave's foot is in) at each point of its path from
     * {@code (startX, startY, startZ)} along {@code (dirX, dirZ)} (a unit vector): the first is
     * {@code startY}, and the path is {@code (length - 1) * STEP} blocks long.
     */
    public static int[] path(Ground ground, double startX, int startY, double startZ, double dirX, double dirZ) {
        int steps = (int) Math.round(MAX_LENGTH / STEP);
        int[] heights = new int[steps + 1];
        heights[0] = startY;
        int y = startY;
        for (int i = 1; i <= steps; i++) {
            int x = (int) Math.floor(startX + dirX * i * STEP);
            int z = (int) Math.floor(startZ + dirZ * i * STEP);
            Integer next = nextHeight(ground, x, y, z);
            if (next == null) {
                return Arrays.copyOf(heights, i);
            }
            y = next;
            heights[i] = y;
        }
        return heights;
    }

    /** Where the foot goes in column (x, z), coming from height {@code y}; null if the wave can't go on. */
    private static Integer nextHeight(Ground ground, int x, int y, int z) {
        if (ground.solid(x, y, z)) {
            // Something in the way: climb it if it's a single block.
            for (int climb = 1; climb <= MAX_CLIMB; climb++) {
                if (!ground.solid(x, y + climb, z)) {
                    return y + climb;
                }
            }
            return null;
        }
        // Open: fall onto whatever is below, if it isn't too far down.
        for (int drop = 0; drop <= MAX_DROP; drop++) {
            if (ground.solid(x, y - 1 - drop, z)) {
                return y - drop;
            }
        }
        return null;
    }

    /** How far a path of these heights runs, in blocks. */
    public static double length(int[] heights) {
        return (heights.length - 1) * STEP;
    }

    /** Whether a path is long enough for the wave to rise at all. */
    public static boolean hasRoom(int[] heights) {
        return length(heights) >= MIN_LENGTH;
    }
}
