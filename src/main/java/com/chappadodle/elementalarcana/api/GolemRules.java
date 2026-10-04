package com.chappadodle.elementalarcana.api;

/**
 * Elemental Golems (see the Golems spec): their stats, the slam's timing and reach, and how often
 * one comes near a player.
 */
public final class GolemRules {
    public static final double HEALTH = 60;
    public static final double ARMOR = 6;
    public static final double SPEED = 0.22;
    public static final double SLAM_DAMAGE = 8;
    public static final double KNOCKBACK_RESISTANCE = 0.8;
    /** How close it must be to slam, how wide the slam lands, and how far in front of it. */
    public static final double REACH = 3.0;
    public static final double SLAM_RADIUS = 2.5;
    public static final double SLAM_AHEAD = 1.2;
    /** The warning (arms raised) before the slam lands, and the least time between slams. */
    public static final int WINDUP_TICKS = 15;
    public static final int SLAM_COOLDOWN_TICKS = 40;
    /** How near a mage of another element must come to be a trespasser. */
    public static final double TERRITORY = 12;
    /** Spawning: how often each player rolls, the chances, how far off, and how sparse golems are. */
    public static final int CHECK_TICKS = 600;
    public static final double DAY_CHANCE = 0.03;
    public static final double NIGHT_CHANCE = 0.06;
    public static final double MIN_DISTANCE = 24;
    public static final double MAX_DISTANCE = 40;
    public static final double SPACING = 64;
    public static final int MIN_ESSENCE = 2;
    public static final int MAX_ESSENCE = 4;
    public static final int EXPERIENCE = 25;

    private GolemRules() {
    }

    public static double chance(boolean night) {
        return night ? NIGHT_CHANCE : DAY_CHANCE;
    }

    /** Where the slam lands, as a progress through its swing ({@code ticks} since the arms went up): 0 rising, 1 the blow. */
    public static float swing(float ticks) {
        if (ticks < 0) {
            return 0f;
        }
        return Math.min(1f, ticks / WINDUP_TICKS);
    }
}
