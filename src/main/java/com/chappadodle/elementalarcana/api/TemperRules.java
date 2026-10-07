package com.chappadodle.elementalarcana.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Tempering at an Ember Anvil (see the Ember Anvil spec): how many tempers a piece of gear takes,
 * what each costs, and a piece's stats tempered. Plain Java, unit tested.
 */
public final class TemperRules {
    /** The most tempers a piece takes. */
    public static final int MAX = 3;
    /** What a temper costs: Ember Cores, and experience levels. */
    public static final int CORES = 1;
    public static final int LEVELS = 5;

    private TemperRules() {
    }

    /** Whether a piece tempered {@code times} takes another. */
    public static boolean canTemper(int times) {
        return times < MAX;
    }

    /** A piece's stats tempered {@code times}: each of them one more a temper (never more than the most tempers). */
    public static Map<String, Integer> tempered(Map<String, Integer> stats, int times) {
        int extra = Math.clamp(times, 0, MAX);
        if (extra == 0) {
            return stats;
        }
        Map<String, Integer> out = new HashMap<>();
        stats.forEach((key, value) -> out.put(key, value + extra));
        return out;
    }
}
