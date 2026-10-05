package com.chappadodle.elementalarcana.api;

/**
 * The numbers of the Sky Isles (docs/superpowers/specs/2026-10-05-sky-isles-design.md): how high they
 * float and the shape of their undersides. Plain Java, unit tested.
 */
public final class SkyIsleRules {
    /** Its top floats this far above the highest ground under it, never below MIN_Y. */
    public static final int MIN_ABOVE = 70;
    public static final int MAX_ABOVE = 100;
    public static final int MIN_Y = 170;
    /** Room kept under the build limit for its trees. */
    public static final int TOP_MARGIN = 24;
    public static final int RADIUS_MIN = 5;
    public static final int RADIUS_MAX = 7;
    public static final int DEPTH_MIN = 10;
    public static final int DEPTH_MAX = 14;
    /** The islet beside one isle in two: smaller, a little higher or lower, this far from it. */
    public static final int SATELLITE_RADIUS = 3;
    public static final int SATELLITE_DEPTH = 6;
    public static final double SATELLITE_MIN = 15;
    public static final double SATELLITE_MAX = 18;
    /** The piece's half-width, enough for the isle, its islet and their trees. */
    public static final int REACH = 24;

    private SkyIsleRules() {
    }

    /** Where an isle's top floats over {@code ground} (the highest ground under it), from a roll in [0, 1), below {@code ceiling}. */
    public static int topY(int ground, double roll, int ceiling) {
        int y = Math.max(MIN_Y, ground + MIN_ABOVE + (int) (roll * (MAX_ABOVE - MIN_ABOVE)));
        return Math.min(y, ceiling - TOP_MARGIN);
    }

    /** How far an isle {@code topRadius} wide at its top reaches {@code below} blocks under it: a full top tapering to a point. */
    public static double radiusAt(double topRadius, int below, int depth) {
        if (below >= depth) {
            return 0;
        }
        return topRadius * (1 - Math.pow(below / (double) depth, 1.6));
    }
}
