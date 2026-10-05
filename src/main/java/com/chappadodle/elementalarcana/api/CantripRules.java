package com.chappadodle.elementalarcana.api;

/**
 * The numbers of the cantrips (docs/superpowers/specs/2026-10-04-cantrips-design.md): everyday
 * magic of the Arcane school, learned from scrolls. Plain Java, unit tested.
 */
public final class CantripRules {
    /** How far away a Mage Light can be set. */
    public static final double LIGHT_RANGE = 24;
    /** Prospect: ores this near (a cube's half-width), seen for this long, at most this many. */
    public static final int PROSPECT_RADIUS = 12;
    public static final int PROSPECT_TICKS = 160;
    public static final int PROSPECT_MAX_ORES = 512;
    /** Recall: this long standing still; moving further than this breaks it. */
    public static final int RECALL_CHANNEL_TICKS = 80;
    public static final double RECALL_STILL = 0.5;
    /** Mend: a fifth of the item's durability, at least 25. */
    public static final float MEND_SHARE = 0.2f;
    public static final int MEND_MIN = 25;
    public static final int WATER_BREATHING_TICKS = 1800;
    public static final int FEATHERFALL_TICKS = 400;
    public static final int NIGHT_EYE_TICKS = 3600;

    private CantripRules() {
    }

    /** How much durability Mend gives back to an item that has {@code maxDamage} in all. */
    public static int mendAmount(int maxDamage) {
        return Math.max(MEND_MIN, Math.round(maxDamage * MEND_SHARE));
    }

    /** Whether a Recall breaks: its caster was hurt, or moved (squared distance from where they began). */
    public static boolean recallBroken(double movedSquared, boolean hurt) {
        return hurt || movedSquared > RECALL_STILL * RECALL_STILL;
    }

    /** Whether a block {@code dx}, {@code dy}, {@code dz} from the caster is within Prospect's reach. */
    public static boolean inProspect(int dx, int dy, int dz) {
        return Math.abs(dx) <= PROSPECT_RADIUS && Math.abs(dy) <= PROSPECT_RADIUS && Math.abs(dz) <= PROSPECT_RADIUS;
    }
}
