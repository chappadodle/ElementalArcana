package com.chappadodle.elementalarcana.api;

/**
 * The Creatures of the Nether (see their spec): the Ash Wraith and the Cinder Hound, how often they
 * come, their numbers, and their trophies'. Plain Java, unit tested.
 */
public final class NetherCreatureRules {
    /** Their chances at each of the wild spawner's checks round a player in the Nether. */
    public static final float ASH_WRAITH_CHANCE = 0.12f;
    public static final float HOUND_CHANCE = 0.10f;
    /** A pack of hounds: two to four. */
    public static final int PACK_MIN = 2;
    public static final int PACK_MAX = 4;

    /** The Ash Wraith's touch, once its prey burns: a little harm, Wither and Slowness, and how long before the next. */
    public static final float ASH_TOUCH_DAMAGE = 3f;
    public static final int WITHER_TICKS = 80;
    public static final int SLOW_TICKS = 40;
    public static final int ASH_TOUCH_COOLDOWN_TICKS = 160;
    /** Water and ice tear an Ash Wraith: their harm is doubled. */
    public static final float ASH_WEAKNESS = 2f;

    /** The Cinder Hound's bite sets its prey alight this long; water and rain hurt it this much a second. */
    public static final int HOUND_BURN_SECONDS = 3;
    public static final float HOUND_WATER_DAMAGE = 1f;
    /** How far a hurt hound's howl carries to its pack. */
    public static final double HOWL_RADIUS = 16;

    /** The Houndstooth Charm: how much faster its bearer runs in the Nether (a share of their speed). */
    public static final double HOUNDSTOOTH_SPEED = 1.0 / 6.0;

    private NetherCreatureRules() {
    }

    /** A pack's size for {@code roll} uniform in [0, 1): two to four. */
    public static int packSize(double roll) {
        return PACK_MIN + Math.min(PACK_MAX - PACK_MIN, Math.max(0, (int) (roll * (PACK_MAX - PACK_MIN + 1))));
    }

    /** The harm an Ash Wraith takes from a blow of {@code amount}: doubled if water or ice ({@code tears}). */
    public static float ashWraithDamage(float amount, boolean tears) {
        return tears ? amount * ASH_WEAKNESS : amount;
    }
}
