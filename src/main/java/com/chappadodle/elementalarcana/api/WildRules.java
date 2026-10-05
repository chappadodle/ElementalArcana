package com.chappadodle.elementalarcana.api;

/**
 * The numbers of the Creatures of the Wild (docs/superpowers/specs/2026-10-04-wild-creatures-design.md):
 * the Thornwood Treant, the Frost Wraith and the Ember Salamander. Plain Java, unit tested.
 */
public final class WildRules {
    /** Each player rolls for each creature this often, and each is born 20 to 40 blocks off, never two of a kind within SPACING. */
    public static final int CHECK_TICKS = 400;
    public static final double MIN_DISTANCE = 20;
    public static final double MAX_DISTANCE = 40;
    public static final double SPACING = 48;
    public static final float TREANT_CHANCE = 0.06f;
    public static final float WRAITH_CHANCE = 0.12f;
    public static final float SALAMANDER_CHANCE = 0.08f;
    public static final float HARPY_CHANCE = 0.08f;
    public static final float CRAWLER_CHANCE = 0.10f;
    public static final float LURKER_CHANCE = 0.10f;

    // The treant.
    public static final double TREANT_WAKE = 6;
    public static final int TREANT_FORGET_TICKS = 400;
    public static final float TREANT_GRASP = 8f;
    public static final int ROOT_COOLDOWN_TICKS = 160;
    public static final int ROOT_WINDUP_TICKS = 15;
    public static final int ROOTED_TICKS = 40;
    public static final double ROOT_RANGE = 10;
    public static final float ROOT_DAMAGE = 3f;
    public static final float FIRE_WEAKNESS = 1.5f;

    // The wraith.
    public static final int WRAITH_BLINK_COOLDOWN_TICKS = 60;
    public static final double WRAITH_BLINK_MIN = 4;
    public static final double WRAITH_BLINK_MAX = 6;
    public static final float WRAITH_WEAKNESS = 1.5f;
    public static final float WRAITH_TOUCH_DAMAGE = 4f;
    public static final int WRAITH_TOUCH_COOLDOWN_TICKS = 80;

    // The salamander.
    public static final double SALAMANDER_NOTICE = 4;
    public static final double SPIT_RANGE = 8;
    public static final int SPIT_COOLDOWN_TICKS = 80;
    public static final int BITE_FIRE_TICKS = 60;

    // The harpy (docs/superpowers/specs/2026-10-05-wild-creatures-2-design.md).
    public static final int SNATCH_COOLDOWN_TICKS = 160;
    public static final double SNATCH_LIFT = 6;
    public static final int SNATCH_MAX_TICKS = 60;
    public static final float SNATCH_BREAK_DAMAGE = 6f;
    public static final int GUST_COOLDOWN_TICKS = 100;
    public static final double GUST_RANGE = 10;

    // The crawler.
    public static final int CRAWLER_MAX_Y = 40;
    public static final int VOLLEY_COOLDOWN_TICKS = 70;
    public static final double VOLLEY_RANGE = 16;
    public static final float VOLLEY_SPREAD_DEGREES = 8f;
    /** Each of its shards, against a mob's full prism bolt. */
    public static final float VOLLEY_POWER = 0.7f;
    public static final double BURST_RADIUS = 3;
    public static final float BURST_DAMAGE = 4f;

    // The lurker.
    public static final double LUNGE_RANGE = 5;
    public static final int LUNGE_COOLDOWN_TICKS = 100;
    public static final float LUNGE_BITE = 6f;
    public static final int HOLD_TICKS = 60;
    public static final float HOLD_BREAK_DAMAGE = 8f;
    public static final double SPIT_MUD_RANGE = 12;
    public static final int MUD_COOLDOWN_TICKS = 90;

    private WildRules() {
    }

    /** Whether a harpy lets go: it's carried them long enough, or it's been hurt enough meanwhile. */
    public static boolean snatchBroken(int carriedTicks, float damageWhileCarrying) {
        return carriedTicks >= SNATCH_MAX_TICKS || damageWhileCarrying >= SNATCH_BREAK_DAMAGE;
    }

    /** Whether a lurker lets go: it's held on long enough, or one hit was hard enough. */
    public static boolean holdBroken(int heldTicks, float hit) {
        return heldTicks >= HOLD_TICKS || hit >= HOLD_BREAK_DAMAGE;
    }

    /** The crawler's three shards, as turns (degrees) from straight at its target. */
    public static float[] volleyOffsets() {
        return new float[]{-VOLLEY_SPREAD_DEGREES, 0, VOLLEY_SPREAD_DEGREES};
    }

    /** How hard a hit lands on a treant: fire burns it worse. */
    public static float treantDamage(float amount, boolean fire) {
        return fire ? amount * FIRE_WEAKNESS : amount;
    }

    /** How hard a hit lands on a wraith: fire and Radiance tear it. */
    public static float wraithDamage(float amount, boolean fireOrRadiance) {
        return fireOrRadiance ? amount * WRAITH_WEAKNESS : amount;
    }

    /** Whether a wraith's touch freezes: only someone already frosted through. */
    public static boolean touchFreezes(int ticksFrozen, int ticksToFreeze) {
        return ticksFrozen >= ticksToFreeze;
    }

    /** Whether a cooldown ending at {@code readyAt} is over at {@code now}. */
    public static boolean ready(long readyAt, long now) {
        return now >= readyAt;
    }
}
