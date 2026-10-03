package com.chappadodle.elementalarcana.api;

/**
 * Tremor's shockwave (see the Earth spec): its front runs along the ground a block a tick, up to
 * {@link #LENGTH} blocks, climbing steps of one block and following drops of up to two
 * ({@link GroundPath}). Whatever stands where the front passes is caught once; anything with its
 * feet more than {@link #MAX_ABOVE} over the ground there (a creature in mid-jump) escapes it.
 */
public final class TremorRules {
    public static final int LENGTH = 10;
    public static final int MAX_CLIMB = 1;
    public static final int MAX_DROP = 2;
    /** How far to either side of its line the front reaches. */
    public static final double HALF_WIDTH = 1.6;
    /** How far before and behind the front, along its way, a creature is still caught. */
    public static final double REACH = 0.6;
    public static final double MAX_ABOVE = 1.0;

    private TremorRules() {
    }

    /** The foot heights at each block of the path from {@code (startX, startY, startZ)} along {@code (dirX, dirZ)} (a unit vector). */
    public static int[] path(GroundPath.Ground ground, double startX, int startY, double startZ, double dirX, double dirZ) {
        return GroundPath.trace(ground, startX, startY, startZ, dirX, dirZ, 1.0, LENGTH, MAX_CLIMB, MAX_DROP);
    }

    /** Whether the shockwave can run at all: at least a block. */
    public static boolean hasRoom(int[] heights) {
        return heights.length > 1;
    }

    /**
     * Whether the front catches a creature this far from it ({@code along} its way and
     * {@code across} it, and with its feet {@code above} the front's foot), {@code halfWidth} being
     * half the creature's width.
     */
    public static boolean catches(double along, double across, double above, double halfWidth) {
        return Math.abs(along) <= REACH + halfWidth && Math.abs(across) <= HALF_WIDTH + halfWidth
                && above >= -1.0 && above <= MAX_ABOVE;
    }
}
