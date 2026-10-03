package com.chappadodle.elementalarcana.api;

/**
 * Tsunami's path (see the Tsunami spec): the height a wave's foot rolls at along a straight line,
 * a step every half block, and how far it gets. It climbs a step of one block, pours down a drop of
 * up to three, and ends against anything taller or deeper; never past {@link #MAX_LENGTH} blocks.
 * The world is asked through {@link GroundPath.Ground}, so these rules don't touch it.
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

    /**
     * The foot heights (the block a wave's foot is in) at each point of its path from
     * {@code (startX, startY, startZ)} along {@code (dirX, dirZ)} (a unit vector): the first is
     * {@code startY}, and the path is {@code (length - 1) * STEP} blocks long. {@code ground} says
     * what holds the wave up or stops it.
     */
    public static int[] path(GroundPath.Ground ground, double startX, int startY, double startZ, double dirX, double dirZ) {
        return GroundPath.trace(ground, startX, startY, startZ, dirX, dirZ, STEP, (int) Math.round(MAX_LENGTH / STEP), MAX_CLIMB, MAX_DROP);
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
