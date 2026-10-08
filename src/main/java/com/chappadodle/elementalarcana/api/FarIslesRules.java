package com.chappadodle.elementalarcana.api;

import java.util.List;

/**
 * The Far Isles' numbers (docs/superpowers/specs/2026-10-08-the-far-isles-design.md): the
 * observatories' star chart, the Stargazers, the Voidwalker's Charm and the Astral Chart. Plain
 * Java, unit tested.
 */
public final class FarIslesRules {
    /** The four constellations a Star Lens can show, in the order a use turns it. */
    public static final List<String> CONSTELLATIONS = List.of("flame", "wave", "gale", "stone");
    /** Observatories stand at least this far from the End's centre (on the outer islands). */
    public static final int MIN_DISTANCE = 1000;
    /** The terrace's radius; the four pillars (and the lenses at their feet) stand this far out. */
    public static final int TERRACE = 9;
    public static final int LENS_DISTANCE = 6;
    /** A Stargazer's chance per player check (as the wild creatures', at the server's rate). */
    public static final float STARGAZER_CHANCE = 0.25f;
    /** Within this many blocks, a Stargazer notices a player looking it in the face. */
    public static final double STARE_RANGE = 24;
    /** Its touch: Levitation, this long. */
    public static final int TOUCH_LEVITATION_TICKS = 20;
    public static final int TOUCH_COOLDOWN_TICKS = 60;
    /** The Voidwalker's Charm: once every five minutes, and Slow Falling this long after. */
    public static final int VOIDWALK_COOLDOWN_TICKS = 6000;
    public static final int VOIDWALK_SLOW_FALL_TICKS = 100;
    /** The Astral Chart's trail of starlight: how far it runs toward home, and for how long it's shown. */
    public static final int TRAIL_LENGTH = 24;
    public static final int CHART_COOLDOWN_TICKS = 40;

    private FarIslesRules() {
    }

    /** The constellation after {@code index} (a lens turned once). */
    public static int next(int index) {
        return Math.floorMod(index + 1, CONSTELLATIONS.size());
    }

    /**
     * An observatory's chart from its seed: the four constellations in an order of their own, one
     * for each side (north, east, south, west).
     */
    public static int[] chart(long seed) {
        int[] chart = {0, 1, 2, 3};
        long state = seed;
        for (int i = chart.length - 1; i > 0; i--) {
            state = state * 6364136223846793005L + 1442695040888963407L;
            int j = (int) Math.floorMod(state >>> 33, (long) (i + 1));
            int swap = chart[i];
            chart[i] = chart[j];
            chart[j] = swap;
        }
        return chart;
    }

    /** Whether the lenses (north, east, south, west; -1 for a missing one) show the chart. */
    public static boolean matches(int[] lenses, int[] chart) {
        if (lenses.length != chart.length) {
            return false;
        }
        for (int i = 0; i < chart.length; i++) {
            if (lenses[i] != chart[i]) {
                return false;
            }
        }
        return true;
    }

    /** Whether a place {@code x}, {@code z} blocks from the End's centre is far enough out for an observatory. */
    public static boolean farEnough(int x, int z) {
        return (long) x * x + (long) z * z >= (long) MIN_DISTANCE * MIN_DISTANCE;
    }

    /** Whether the Voidwalker's Charm is ready, last used at {@code lastUsed} (-1: never). */
    public static boolean voidwalkReady(long now, long lastUsed) {
        return lastUsed < 0 || now - lastUsed >= VOIDWALK_COOLDOWN_TICKS;
    }

    /** A compass word for a direction on the ground (dx east, dz south): "north", "south-east"... */
    public static String direction(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        int octant = (int) Math.floorMod(Math.round(angle / 45), 8);
        return List.of("north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west").get(octant);
    }
}
