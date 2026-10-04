package com.chappadodle.elementalarcana.api;

/**
 * The numbers of Drake Riding (docs/superpowers/specs/2026-10-04-drake-riding-design.md): eggs,
 * growing up, flying and its stamina. Plain Java, unit tested.
 */
public final class DrakeRidingRules {
    /** An egg hatches after this much warmth (20 minutes; a storm egg warms twice as fast in a thunderstorm). */
    public static final int HATCH_TICKS = 24000;
    /** Growth for each stage: a juvenile after a day, an adult after three. */
    public static final int JUVENILE_AT = 24000;
    public static final int ADULT_AT = 72000;
    /** Feeding: its element's Essence, or meat at half as much. */
    public static final int ESSENCE_GROWTH = 6000;
    public static final int MEAT_GROWTH = 3000;
    public static final float FEED_HEAL = 10f;
    /** Size and health by stage: hatchling, juvenile, adult. */
    private static final float[] SCALE = {0.25f, 0.5f, 1f};
    private static final double[] HEALTH = {30, 70, 140};
    /** Flight: 30 seconds of stamina, back three times as fast on the ground. */
    public static final int MAX_STAMINA = 600;
    public static final int STAMINA_REGEN = 3;
    /** A take-off needs this much stamina. */
    public static final int TAKE_OFF_STAMINA = 60;
    public static final double FLY_SPEED = 0.9;
    public static final double CRUISE_SPEED = 0.5;
    public static final double HOVER_SPEED = 0.15;
    public static final double CLIMB = 0.35;
    public static final double SINK = 0.12;
    public static final double TAKE_OFF = 0.7;
    /** A rider's breath: the warning, the breath, and the wait before the next. */
    public static final int RIDER_BREATH_WINDUP = 12;
    public static final int RIDER_BREATH_TICKS = 30;
    public static final int RIDER_BREATH_COOLDOWN = 160;
    /** A tamed drake breathes at its foes this often at most (juveniles and adults). */
    public static final int FIGHT_BREATH_COOLDOWN = 200;

    private DrakeRidingRules() {
    }

    /** The stage for {@code growth}: 0 hatchling, 1 juvenile, 2 adult. */
    public static int stage(int growth) {
        return growth >= ADULT_AT ? 2 : growth >= JUVENILE_AT ? 1 : 0;
    }

    public static float scale(int stage) {
        return SCALE[Math.clamp(stage, 0, 2)];
    }

    public static double health(int stage) {
        return HEALTH[Math.clamp(stage, 0, 2)];
    }

    /** How much warmth an egg gains this tick. */
    public static int warmth(boolean warm, boolean storming) {
        return warm ? storming ? 2 : 1 : 0;
    }

    /** How cracked an egg looks, 0 to 2, by how far along it is. */
    public static int cracks(int warmth) {
        return Math.clamp(warmth * 3L / HATCH_TICKS, 0, 2);
    }

    /** Stamina after a tick: flying spends one, standing on the ground gives STAMINA_REGEN back. */
    public static int stamina(int stamina, boolean flying, boolean onGround) {
        if (flying) {
            return Math.max(0, stamina - 1);
        }
        return onGround ? Math.min(MAX_STAMINA, stamina + STAMINA_REGEN) : stamina;
    }
}
