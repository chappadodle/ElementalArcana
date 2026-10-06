package com.chappadodle.elementalarcana.api;

import java.util.List;

/**
 * The Seeker's Compass's rules (docs/superpowers/specs/2026-10-05-seekers-compass-design.md): what
 * it seeks and in what order, how far it looks, when you've arrived.
 */
public final class SeekerRules {
    /** What it can seek, in the order sneak-use goes through them (each is the structure tag elementalarcana:seekable/<kind>). */
    public static final List<String> KINDS = List.of("shrines", "ruins", "crypts", "mage_towers", "sanctums", "drake_nests", "sky_isles",
            "hollowed_camps", "wisp_rings");
    /** How far it looks: rings of each structure's spread grid, as /locate and explorer maps count them (thousands of blocks). */
    public static final int SEARCH_RINGS = 100;
    /** Within this many blocks of what it found, on the ground (any height), you've arrived. */
    public static final double ARRIVAL_DISTANCE = 32;
    /** Ticks between seeks (each is a search of the world). */
    public static final int COOLDOWN_TICKS = 60;

    private SeekerRules() {
    }

    /** The kind after {@code kind} (the first for an unknown one). */
    public static String next(String kind) {
        return KINDS.get((KINDS.indexOf(kind) + 1) % KINDS.size());
    }

    /** {@code kind} if it's one the compass knows, the first kind otherwise. */
    public static String known(String kind) {
        return KINDS.contains(kind) ? kind : KINDS.get(0);
    }

    /** Whether an offset on the ground (x and z) is close enough to have arrived. */
    public static boolean arrived(double dx, double dz) {
        return dx * dx + dz * dz <= ARRIVAL_DISTANCE * ARRIVAL_DISTANCE;
    }

    /** A distance on the ground, to the nearest ten blocks (for "about 640 blocks away"). */
    public static int roughDistance(double dx, double dz) {
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz) / 10) * 10;
    }
}
