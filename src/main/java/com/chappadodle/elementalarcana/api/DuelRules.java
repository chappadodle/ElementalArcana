package com.chappadodle.elementalarcana.api;

/**
 * The Circle's duels (see the Circle spec, part 3): when a duellist yields, how long the parts of a
 * bout last, how far the ring reaches, what a win pays and how many wins the Archmagister asks
 * for. Plain Java, unit tested; the bouts themselves are content code's.
 */
public final class DuelRules {
    /** A duellist yields when a blow would leave them below this share of their health. */
    public static final float YIELD_SHARE = 0.2f;
    /** How long the mage waits for the player to step into the ring (thirty seconds), in ticks. */
    public static final int GATHER_TICKS = 600;
    /** How long a mage that hasn't reached the ring walks before it steps there by magic. */
    public static final int WALK_TICKS = 200;
    /** The count before a bout (three seconds). */
    public static final int COUNT_TICKS = 60;
    /** How long a bout lasts before it's a draw (two minutes). */
    public static final int BOUT_TICKS = 2400;
    /** The duels a player must have won before the Archmagister will fight them. */
    public static final int ARCHMAGISTER_WINS = 3;
    /** The ring's reach from its middle: inside it to begin; past the rim, a duellist has left it. */
    public static final double RING_RADIUS = 5.5;
    public static final double RING_LEAVE = 6.5;

    private DuelRules() {
    }

    /** The marks a win pays besides the wager back: 2 from a mage, 5 from the Archmagister. */
    public static int prize(boolean archmagister) {
        return archmagister ? 5 : 2;
    }

    /** Whether a blow of {@code damage} would leave a duellist at {@code health} (of {@code maxHealth}) below the yield. */
    public static boolean yields(float health, float maxHealth, float damage) {
        return health - damage < maxHealth * YIELD_SHARE;
    }

    /** The damage a blow deals in a bout: as much as it would, but never past the yield. */
    public static float capped(float health, float maxHealth, float damage) {
        return yields(health, maxHealth, damage) ? Math.max(0f, health - maxHealth * YIELD_SHARE) : damage;
    }

    /** Whether (dx, dz) from the ring's middle is in the ring: inside it, or ({@code leaving}) not yet past its rim. */
    public static boolean inRing(double dx, double dz, boolean leaving) {
        double reach = leaving ? RING_LEAVE : RING_RADIUS;
        return dx * dx + dz * dz <= reach * reach;
    }

    /** The number the count shows {@code ticks} into it (3, 2, 1), or 0 once it's done. */
    public static int count(long ticks) {
        return ticks >= COUNT_TICKS ? 0 : 3 - (int) (ticks / 20);
    }
}
