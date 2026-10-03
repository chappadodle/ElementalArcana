package com.chappadodle.elementalarcana.api;

/**
 * Gale Dash's numbers by spell level and branch (see the Gale Dash spec): charges, the dash itself,
 * what it leaves along its way, Blink Storm's blink and Hurricane Rush's rush.
 */
public final class GaleDashRules {
    public static final String PHANTOM_STEP = "phantom_step";
    public static final String GALE_STRIKE = "gale_strike";
    public static final String BLINK_STORM = "blink_storm";
    public static final String HURRICANE_RUSH = "hurricane_rush";

    /** How long a dash lasts: its trail, what it strikes and passes through, in ticks. */
    public static final int DASH_TICKS = 8;
    /** Air Step (level 3): no harm comes to the dasher for this long. */
    public static final int GUARD_TICKS = 8;
    /** The dash's speed, blocks a tick, before the caster's power. */
    public static final double SPEED = 1.5;
    /** How close to the dasher things are passed through (struck, Swirled, pushed aside). */
    public static final double REACH = 1.8;
    public static final float STRIKE_DAMAGE = 4f;
    /** Phantom Step: how long the dasher is unseen. */
    public static final int PHANTOM_TICKS = 20;
    /** Blink Storm: how far the blink goes, and how far its gusts reach. */
    public static final double BLINK_RANGE = 8;
    public static final double BLINK_GUST = 3;
    public static final float BLINK_DAMAGE = 3f;
    /** Hurricane Rush: how long it can be held, how fast it goes, what it hits for. */
    public static final int RUSH_TICKS = 40;
    public static final double RUSH_SPEED = 0.9;
    public static final float RUSH_DAMAGE = 3f;

    private GaleDashRules() {
    }

    /** Dashes held at once: 1, 2 from level 2 (Second Wind), 3 from level 7 (Triple Charge). */
    public static int charges(int level) {
        return level >= 7 ? 3 : level >= 2 ? 2 : 1;
    }

    /** The dash's speed for a caster of {@code power}: a little more with power, at most 30% more. */
    public static double speed(float power) {
        return SPEED * Math.clamp(Math.sqrt(power), 0.8, 1.3);
    }

    /** Air Step (level 3): mid-air dashes go any way, and the dasher is unharmed while dashing. */
    public static boolean airStep(int level) {
        return level >= 3;
    }

    /** Slipstream Trail (level 4): the dash shoves foes aside and quickens allies it passes. */
    public static boolean slipstream(int level) {
        return level >= 4;
    }

    /** Tailwind (level 6): speed after a dash, and a quarter off the cooldown. */
    public static boolean tailwind(int level) {
        return level >= 6;
    }

    public static float cooldownFactor(int level) {
        return tailwind(level) ? 0.75f : 1f;
    }

    /** Featherfall (level 8): a slow drift down after a dash. */
    public static boolean featherfall(int level) {
        return level >= 8;
    }

    /** Swirling Rush (level 9): dashing through a foe that carries an element Swirls it. */
    public static boolean swirls(int level) {
        return level >= 9;
    }
}
