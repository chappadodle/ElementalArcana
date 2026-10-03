package com.chappadodle.elementalarcana.api;

import java.util.Arrays;

/**
 * A path along the ground in a straight line (Tsunami's wave, Tremor's shockwave): the height its
 * foot is at, point by point. It climbs a step of up to {@code maxClimb} blocks, follows a drop of
 * up to {@code maxDrop}, and ends against anything taller or deeper. The world is asked through
 * {@link Ground}, so these rules don't touch it.
 */
public final class GroundPath {
    private GroundPath() {
    }

    /** What the path runs on and ends against: whether the block at (x, y, z) holds it up. */
    @FunctionalInterface
    public interface Ground {
        boolean solid(int x, int y, int z);
    }

    /**
     * The foot heights (the block the path's foot is in) at each point of a path from
     * {@code (startX, startY, startZ)} along {@code (dirX, dirZ)} (a unit vector), {@code step}
     * blocks apart: the first is {@code startY}, and there are at most {@code steps + 1}.
     */
    public static int[] trace(Ground ground, double startX, int startY, double startZ, double dirX, double dirZ,
                              double step, int steps, int maxClimb, int maxDrop) {
        int[] heights = new int[steps + 1];
        heights[0] = startY;
        int y = startY;
        for (int i = 1; i <= steps; i++) {
            int x = (int) Math.floor(startX + dirX * i * step);
            int z = (int) Math.floor(startZ + dirZ * i * step);
            Integer next = nextHeight(ground, x, y, z, maxClimb, maxDrop);
            if (next == null) {
                return Arrays.copyOf(heights, i);
            }
            y = next;
            heights[i] = y;
        }
        return heights;
    }

    /** Where the foot goes in column (x, z), coming from height {@code y}; null if the path can't go on. */
    private static Integer nextHeight(Ground ground, int x, int y, int z, int maxClimb, int maxDrop) {
        if (ground.solid(x, y, z)) {
            // Something in the way: climb it if it's low enough.
            for (int climb = 1; climb <= maxClimb; climb++) {
                if (!ground.solid(x, y + climb, z)) {
                    return y + climb;
                }
            }
            return null;
        }
        // Open: fall onto whatever is below, if it isn't too far down.
        for (int drop = 0; drop <= maxDrop; drop++) {
            if (ground.solid(x, y - 1 - drop, z)) {
                return y - drop;
            }
        }
        return null;
    }
}
