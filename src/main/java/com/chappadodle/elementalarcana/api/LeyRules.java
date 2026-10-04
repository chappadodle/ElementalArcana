package com.chappadodle.elementalarcana.api;

/**
 * Ley Lines (see the Ley Lines spec): travelling between remembered shrines. How near a Shrine Core
 * a traveller must stand, what a journey costs, how long the lines take to settle after, and which
 * way a shrine lies.
 */
public final class LeyRules {
    public static final double REACH = 6;
    public static final int COOLDOWN_TICKS = 1200;
    /** The most shrines the ley menu lists. */
    public static final int MENU_SIZE = 12;
    public static final int BASE_COST = 10;
    public static final int MAX_COST = 50;
    public static final double BLOCKS_PER_MANA = 100;
    private static final String[] DIRECTIONS = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};

    private LeyRules() {
    }

    /** The mana a journey of {@code distance} blocks costs. */
    public static int manaCost(double distance) {
        return (int) Math.min(MAX_COST, BASE_COST + Math.floor(distance / BLOCKS_PER_MANA));
    }

    /** Which way something {@code (dx, dz)} away lies, as one of eight compass points (Minecraft's north is -z). */
    public static String direction(double dx, double dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        int index = (int) Math.floorMod(Math.round(degrees / 45.0), 8);
        return DIRECTIONS[index];
    }
}
