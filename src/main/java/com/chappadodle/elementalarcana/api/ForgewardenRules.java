package com.chappadodle.elementalarcana.api;

/**
 * The Forgewarden, keeper of a Cinder Forge (see the Cinder Forges spec): its numbers, when it turns
 * molten, and what it leaves. Plain Java, unit tested.
 */
public final class ForgewardenRules {
    public static final double HEALTH = 200;
    public static final double ARMOR = 10;
    public static final double TOUGHNESS = 2;
    public static final double SPEED = 0.22;
    /** How much faster it walks molten (a share of its speed). */
    public static final double MOLTEN_SPEED = 0.4;
    public static final double SLAM_DAMAGE = 9;
    /** Its slam's reach and the ring of cinders it raises (a golem's are 3 and 2.5). */
    public static final double REACH = 4.0;
    public static final double SLAM_RADIUS = 4.0;
    /** It hurls magma (a Meteor) at a foe this far off, in sight, every so often. */
    public static final double HURL_MIN = 6;
    public static final double HURL_MAX = 24;
    public static final int HURL_COOLDOWN_TICKS = 100;
    /** Fire bursts from its seams all round it, now and then, if anyone is this near. */
    public static final double VENT_RADIUS = 5;
    public static final float VENT_DAMAGE = 4f;
    public static final int VENT_COOLDOWN_TICKS = 220;
    public static final int VENT_BURN_SECONDS = 3;
    /** Molten, it leaves burning ground this often. */
    public static final int TRAIL_TICKS = 10;
    /** How near a player comes to the forge's heart before it wakes, and how far it chases from it. */
    public static final double WAKE_RADIUS = 24;
    public static final int GUARD_RADIUS = 20;
    public static final int EXPERIENCE = 120;

    private ForgewardenRules() {
    }

    /** Whether it has turned molten: at half its health or below. */
    public static boolean molten(float health, float maxHealth) {
        return health <= maxHealth * 0.5f;
    }

    /** Whether a foe at {@code distance} is one to hurl magma at. */
    public static boolean hurls(double distance) {
        return distance >= HURL_MIN && distance <= HURL_MAX;
    }

    /** The Ember Cores it leaves: one, and a second on Hard half the time ({@code roll} uniform in [0, 1)). */
    public static int cores(boolean hard, double roll) {
        return hard && roll < 0.5 ? 2 : 1;
    }
}
