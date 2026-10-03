package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

/**
 * Skyward Leap's numbers by spell level and branch (see the Skyward Leap spec): the leap and its
 * gust, how long Skyward lasts, the glide, and the plunge's shockwave.
 */
public final class SkywardRules {
    public static final String SKYFALL = "skyfall";
    public static final String WIND_RIDER = "wind_rider";
    public static final String HEAVENS_DESCENT = "heavens_descent";
    public static final String ENDLESS_SKY = "endless_sky";

    /** Endless Sky's Skyward (it ends when you land, but never lasts past this). */
    public static final int ENDLESS_TICKS = 1200;
    /** Wind Rider's free flight, at the start of Skyward. */
    public static final int WIND_RIDER_TICKS = 120;
    /** Rising Current's updraft, upward blocks a tick (about 5 blocks). */
    public static final double RISING_CURRENT = 0.95;
    /** A plunge dives at this many blocks a tick. */
    public static final double DIVE = 1.4;
    /** Endless Sky: how much a Wind Blade thrown while gliding lifts its thrower, upward blocks a tick. */
    public static final double BLADE_LIFT = 0.45;
    /** Falls shorter than this don't shake the ground. */
    public static final double MIN_PLUNGE_HEIGHT = 2;
    /** Falls longer than this hit no harder. */
    public static final double MAX_PLUNGE_HEIGHT = 20;
    /**
     * A sinking glider must fall faster than this (blocks a tick), or a server takes it for floating
     * (vanilla's check is a 32nd of a block).
     */
    public static final double FLOATING = 1.0 / 32;

    private SkywardRules() {
    }

    /** The leap's upward speed, blocks a tick: about 9 blocks up, 13 from level 2. */
    public static double leapSpeed(int level) {
        return level >= 2 ? 1.55 : 1.3;
    }

    /** How far the leap's gust reaches: 4 blocks, 6 from level 6. */
    public static double gustReach(int level) {
        return level >= 6 ? 6.0 : 4.0;
    }

    /** How hard the gust throws creatures up, blocks a tick. */
    public static double gustLift(int level) {
        return level >= 6 ? 1.0 : 0.8;
    }

    /** How long Skyward lasts, in ticks: 8 seconds, 12 from level 2, and with Endless Sky until you land. */
    public static int skywardTicks(int level, @Nullable String branch10) {
        if (level >= 10 && ENDLESS_SKY.equals(branch10)) {
            return ENDLESS_TICKS;
        }
        return level >= 2 ? 240 : 160;
    }

    /**
     * The speed a glider is steered toward, blocks a tick. Air drag holds them below it: about 6
     * blocks a second in the end, 9 from level 7.
     */
    public static double glideSpeed(int level) {
        return level >= 7 ? 0.62 : 0.42;
    }

    /** How fast a glider sinks, blocks a tick: 1.6 a second, 1.3 from level 2. */
    public static double sinkRate(int level) {
        return level >= 2 ? 0.065 : 0.08;
    }

    /** Whether every landing is safe while Skyward (from level 7), not just the leap's own. */
    public static boolean safeLandings(int level) {
        return level >= 7;
    }

    /** What wind spells cost while gliding, as a share (Aerial Barrage, from level 8). */
    public static float barrageCost(int level) {
        return level >= 8 ? 0.7f : 1f;
    }

    /** How wide the plunge's shockwave is (its radius) after a fall of {@code height} blocks. */
    public static double plungeRadius(double height, @Nullable String branch5, @Nullable String branch10) {
        double radius = 2.5 + 0.15 * fallen(height);
        if (SKYFALL.equals(branch5)) {
            radius *= 1.5;
        }
        if (HEAVENS_DESCENT.equals(branch10)) {
            radius *= 2;
        }
        return radius;
    }

    /** How hard the plunge's shockwave hits after a fall of {@code height} blocks, before the caster's power. */
    public static float plungeDamage(double height, @Nullable String branch5) {
        float damage = (float) (3 + 0.35 * fallen(height));
        return SKYFALL.equals(branch5) ? damage * 1.5f : damage;
    }

    private static double fallen(double height) {
        return Math.clamp(height, 0, MAX_PLUNGE_HEIGHT);
    }
}
