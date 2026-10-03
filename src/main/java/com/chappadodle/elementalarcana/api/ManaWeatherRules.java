package com.chappadodle.elementalarcana.api;

/**
 * Mana weather and Pressure (docs/superpowers/specs/2026-10-03-mana-sense-and-weather-design.md).
 * Plain Java, unit tested.
 */
public final class ManaWeatherRules {
    /** Regeneration in a dead zone, in your elements' lands, in an opposed element's, and in a tide. */
    public static final float DEAD_ZONE = 0.25f;
    public static final float COMFORT = 1.25f;
    public static final float DISCOMFORT = 0.8f;
    public static final float TIDE = 1.5f;
    /** A hidden aura: regeneration, and how far away creatures notice you. */
    public static final float HIDDEN_REGEN = 0.5f;
    public static final double HIDDEN_VISIBILITY = 0.5;
    private static final int RICH_FROM = 10;
    private static final float RICH_PER_LEVEL = 0.01f;
    private static final float RICH_MAX = 0.4f;
    /** Pressure: the gap it starts at, the gap per further rank, the most ranks, and what each rank costs. */
    public static final int PRESSURE_GAP = 15;
    private static final int PRESSURE_STEP = 10;
    public static final int PRESSURE_MAX_RANKS = 3;
    private static final float PRESSURE_POWER = 0.12f;
    private static final float PRESSURE_REGEN = 0.2f;
    private static final int WARD_PER_LEVEL = 4;
    /** The tide: creatures' extra levels, and how much likelier awakening, Attunement and wisps are. */
    public static final int TIDE_LEVELS = 5;
    public static final double TIDE_CHANCES = 2;

    private ManaWeatherRules() {
    }

    /** Rich land: +1% per zone level above 10, at most +40%. */
    public static float rich(int zoneLevel) {
        return 1 + Math.min(RICH_MAX, Math.max(0, zoneLevel - RICH_FROM) * RICH_PER_LEVEL);
    }

    /**
     * The mana in the air: rich land (or a dead zone), comfort (positive: one of your elements'
     * lands; negative: an opposed element's; zero: neither) and the tide.
     */
    public static float regenFactor(int zoneLevel, boolean deadZone, int comfort, boolean tide) {
        float factor = deadZone ? DEAD_ZONE : rich(zoneLevel);
        if (comfort > 0) {
            factor *= COMFORT;
        } else if (comfort < 0) {
            factor *= DISCOMFORT;
        }
        return tide ? factor * TIDE : factor;
    }

    /** Pressure's ranks (0: none) from a creature {@code gap} levels above you, against {@code ward} Ward. */
    public static int pressureRanks(int gap, int ward) {
        int felt = gap - Math.max(0, ward) / WARD_PER_LEVEL;
        if (felt < PRESSURE_GAP) {
            return 0;
        }
        return Math.min(PRESSURE_MAX_RANKS, 1 + (felt - PRESSURE_GAP) / PRESSURE_STEP);
    }

    public static float pressurePower(int ranks) {
        return 1 - PRESSURE_POWER * Math.max(0, ranks);
    }

    public static float pressureRegen(int ranks) {
        return 1 - PRESSURE_REGEN * Math.max(0, ranks);
    }
}
