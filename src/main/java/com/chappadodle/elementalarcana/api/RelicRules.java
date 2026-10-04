package com.chappadodle.elementalarcana.api;

/**
 * The numbers of the relics (docs/superpowers/specs/2026-10-04-relics-design.md). Plain Java, unit
 * tested.
 */
public final class RelicRules {
    /** Stat points a borne relic adds to its stat. */
    public static final int STAT_BONUS = 3;
    /** Ember Heart: burning foes take this much from your spells. */
    public static final float EMBER_BURNING_BONUS = 1.25f;
    /** Tidecaller's Pearl: your Water spells mend you by this share of the damage they deal. */
    public static final float PEARL_MEND_SHARE = 0.1f;
    /** Rimeheart Locket: your spells on frozen and frosted foes. */
    public static final float LOCKET_COLD_BONUS = 1.2f;
    /** Feather of the Gale: the mid-air jump's upward speed (a normal jump is 0.42). */
    public static final double FEATHER_JUMP = 0.62;
    /** Stoneheart Idol: a hit this hard hardens your skin, for so long, at most this often. */
    public static final float IDOL_HARD_HIT = 4f;
    public static final int IDOL_ABSORPTION_TICKS = 200;
    public static final int IDOL_COOLDOWN_TICKS = 300;
    /** Prism of the Deep: mana per Earth or Crystal spell hit, at most every so many ticks. */
    public static final float PRISM_MANA = 2f;
    public static final int PRISM_GAP_TICKS = 10;
    /** Storm Sigil: faster on your feet; spells in a thunderstorm. */
    public static final double SIGIL_SPEED = 0.15;
    public static final float SIGIL_STORM_BONUS = 1.2f;
    /** Sunstone: the undead this close smoulder, so much a second. */
    public static final double SUNSTONE_RADIUS = 6;
    public static final float SUNSTONE_DAMAGE = 1f;
    /** Revenant's Phylactery: once every 10 minutes. */
    public static final int PHYLACTERY_COOLDOWN_TICKS = 12000;

    private RelicRules() {
    }

    /** Whether a hit sets off the Stoneheart Idol. */
    public static boolean hardHit(float damage) {
        return damage >= IDOL_HARD_HIT;
    }

    /** Whether something on a cooldown that ends at {@code readyAt} is ready at {@code now}. */
    public static boolean ready(long readyAt, long now) {
        return now >= readyAt;
    }

    /** Whole seconds until {@code readyAt}, rounded up (0 when ready). */
    public static int secondsLeft(long readyAt, long now) {
        return (int) Math.max(0, (readyAt - now + 19) / 20);
    }

    /** The health a Tidecaller's Pearl gives back for a Water spell hit of {@code damage}. */
    public static float mend(float damage) {
        return Math.max(0f, damage) * PEARL_MEND_SHARE;
    }
}
