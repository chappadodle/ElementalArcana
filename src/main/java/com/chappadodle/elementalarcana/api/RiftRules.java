package com.chappadodle.elementalarcana.api;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * Elemental Rifts (see the Rifts spec): how often one opens near a player whose magic has woken,
 * where, what climbs out of it wave by wave, and what it leaves when it's closed.
 */
public final class RiftRules {
    /** How often each player rolls for a rift. */
    public static final int CHECK_TICKS = 600;
    public static final double NIGHT_CHANCE = 0.015;
    public static final double DAY_CHANCE = 0.004;
    public static final double STORM_FACTOR = 2;
    /** No new rift near a player for this long after the last. */
    public static final long PLAYER_COOLDOWN_TICKS = 12_000;
    /** How far from the player a rift opens, and how far apart two rifts must be. */
    public static final double MIN_DISTANCE = 24;
    public static final double MAX_DISTANCE = 40;
    public static final double SPACING = 96;
    public static final int WAVES = 3;
    /** The tear's opening before the first wave, and how far apart its creatures climb out. */
    public static final int OPENING_TICKS = 40;
    public static final int SPAWN_GAP_TICKS = 8;
    /** A rift not closed in this long (five minutes) closes by itself, its cache lost. */
    public static final int MAX_OPEN_TICKS = 6000;
    /** The magic level from which a rift's Warden is an Archmage (below it, a Magus). */
    public static final int ARCHMAGE_WARDEN_LEVEL = 15;
    /** How near a player must be for the rift to stay open, and how long it waits for one. */
    public static final double PRESENCE = 48;
    public static final int ABANDON_TICKS = 1200;
    public static final int CLOSING_TICKS = 20;
    public static final int MIN_ESSENCE = 4;
    public static final int MAX_ESSENCE = 8;

    private RiftRules() {
    }

    /** The chance per roll of a rift opening near a player. */
    public static double chance(boolean night, boolean thunder) {
        return (night ? NIGHT_CHANCE : DAY_CHANCE) * (thunder ? STORM_FACTOR : 1);
    }

    /**
     * The ranks of the creatures that climb out in wave {@code wave} (1 to 3), wisps aside; the last
     * wave brings the Warden, of rank {@code warden}.
     */
    public static List<AttunementRank> wave(int wave, AttunementRank warden) {
        return switch (wave) {
            case 1 -> List.of(AttunementRank.ADEPT, AttunementRank.ADEPT, AttunementRank.ADEPT);
            case 2 -> List.of(AttunementRank.ADEPT, AttunementRank.ADEPT, AttunementRank.ADEPT, AttunementRank.MAGUS);
            default -> List.of(AttunementRank.ADEPT, AttunementRank.ADEPT, AttunementRank.ADEPT, AttunementRank.MAGUS, warden);
        };
    }

    /** The Warden of a rift opened near a mage of {@code magicLevel}: a Magus for the young, then an Archmage. */
    public static AttunementRank wardenRank(int magicLevel) {
        return magicLevel >= ARCHMAGE_WARDEN_LEVEL ? AttunementRank.ARCHMAGE : AttunementRank.MAGUS;
    }

    /** How many wisps of the rift's element come with wave {@code wave}. */
    public static int wisps(int wave) {
        return wave < WAVES ? 1 : 0;
    }

    /** How much Essence a closed rift throws out ({@code roll} is uniform in [0, 1)). */
    public static int essence(DoubleSupplier roll) {
        return MIN_ESSENCE + (int) (roll.getAsDouble() * (MAX_ESSENCE - MIN_ESSENCE + 1));
    }
}
