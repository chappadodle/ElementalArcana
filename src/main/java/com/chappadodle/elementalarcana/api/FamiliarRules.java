package com.chappadodle.elementalarcana.api;

/**
 * Wisp Familiars (see the Familiars spec): when a wild wisp can be bound, what rank a familiar
 * casts at for its owner's level, and how it keeps up with them.
 */
public final class FamiliarRules {
    /** A wisp can be bound once it's down to this share of its health. */
    public static final float BIND_HEALTH = 0.25f;
    /** The owner's magic level from which their familiar is a Magus, and an Archmage. */
    public static final int MAGUS_LEVEL = 20;
    public static final int ARCHMAGE_LEVEL = 40;
    /** It starts following when this far from its owner, stops when this close, and teleports to them beyond the last. */
    public static final float FOLLOW_START = 6f;
    public static final float FOLLOW_STOP = 2.5f;
    public static final float TELEPORT = 24f;

    private FamiliarRules() {
    }

    public static boolean canBind(float health, float maxHealth) {
        return maxHealth > 0 && health <= maxHealth * BIND_HEALTH;
    }

    /** The rank a familiar casts at for an owner of magic level {@code level}. */
    public static AttunementRank rankFor(int level) {
        if (level >= ARCHMAGE_LEVEL) {
            return AttunementRank.ARCHMAGE;
        }
        return level >= MAGUS_LEVEL ? AttunementRank.MAGUS : AttunementRank.ADEPT;
    }
}
