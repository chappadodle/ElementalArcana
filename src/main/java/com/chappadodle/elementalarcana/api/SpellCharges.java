package com.chappadodle.elementalarcana.api;

/**
 * Spells with several charges (Gale Dash), on top of the one cooldown clock each spell already has:
 * the clock reads how long until every charge is back, each charge taking a full cooldown, and a
 * use adds a cooldown to it (from now, if it had run out). With one charge this is the ordinary
 * cooldown. See {@link Spell#charges}.
 */
public final class SpellCharges {

    private SpellCharges() {
    }

    /** How many of {@code charges} are ready, with the clock {@code remaining} ticks from full and each charge taking {@code perCharge}. */
    public static int ready(int charges, long remaining, int perCharge) {
        if (remaining <= 0 || perCharge <= 0) {
            return charges;
        }
        long missing = (remaining + perCharge - 1) / perCharge;
        return (int) Math.max(0, charges - missing);
    }

    /** Ticks until a charge is ready (0 if one is). */
    public static long untilReady(int charges, long remaining, int perCharge) {
        return ready(charges, remaining, perCharge) > 0 ? 0 : remaining - (long) (charges - 1) * perCharge;
    }

    /** Ticks until the next charge comes back (0 if they all are). */
    public static long untilNext(long remaining, int perCharge) {
        if (remaining <= 0 || perCharge <= 0) {
            return 0;
        }
        return (remaining - 1) % perCharge + 1;
    }

    /** The clock after a use: one more charge's worth. */
    public static long afterUse(long remaining, int perCharge) {
        return Math.max(0, remaining) + perCharge;
    }
}
