package com.chappadodle.elementalarcana.api;

/**
 * The numbers of Starfall (docs/superpowers/specs/2026-10-05-starfall-design.md): how often stars
 * fall, how far off, what they leave. Plain Java, unit tested.
 */
public final class StarfallRules {
    /** Each awakened player's chance, each night, of a star falling near them. */
    public static final float NIGHT_CHANCE = 1f / 3f;
    /** Where it lands: this far from them (in loaded land). */
    public static final double MIN_DISTANCE = 100;
    public static final double MAX_DISTANCE = 180;
    /** Who sees it fall and hears it land. */
    public static final double SEEN_WITHIN = 400;
    /** The streak: this long across the sky, from this high above the impact and this far to one side. */
    public static final int FALL_TICKS = 60;
    public static final double FALL_HEIGHT = 160;
    public static final double FALL_SIDEWAYS = 120;
    /** Dusk (when the night's roll is made) and dawn (when unmined stars cool), as day times. */
    public static final long DUSK = 12500;
    public static final long DAWN = 23500;
    /** The night's falls come between these day times. */
    public static final long EARLIEST = 13500;
    public static final long LATEST = 22000;
    public static final int SCORCH_RADIUS = 3;
    public static final int WISPS_MIN = 2;
    public static final int WISPS_MAX = 3;
    /** No hostile monster spawns this near a Starlit Lantern. */
    public static final double LANTERN_RADIUS = 24;

    private StarfallRules() {
    }

    /** The day time (within the day) at which a star falls, from a roll in [0, 1). */
    public static long fallTime(double roll) {
        return EARLIEST + (long) (roll * (LATEST - EARLIEST));
    }

    /** Whether the day time {@code from} to {@code to} (one tick on) crosses {@code mark} (all within a day). */
    public static boolean crosses(long from, long to, long mark) {
        long a = Math.floorMod(from, 24000);
        long b = Math.floorMod(to, 24000);
        return a < b ? a < mark && mark <= b : a < mark || mark <= b;
    }

    /** One of eight compass points for an offset (x east, z south): "north", "north_east", ... */
    public static String direction(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        String[] points = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
        return points[(int) Math.floorMod(Math.round(angle / 45.0), 8)];
    }

    /** Whether a spawn {@code distanceSquared} from a lantern is kept off. */
    public static boolean lanternWards(double distanceSquared) {
        return distanceSquared <= LANTERN_RADIUS * LANTERN_RADIUS;
    }
}
