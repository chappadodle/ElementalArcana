package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * What elemental creatures give the player who kills them (see CreatureRewards): Magic XP for
 * Attuned ones, and Elemental Essence. Plain Java, no Minecraft types (unit tested). A null rank
 * means a plain innate creature, like a normal blaze.
 */
public final class AttunementRewards {
    private static final double PLAIN_ESSENCE_CHANCE = 0.05;
    private static final double ADEPT_ESSENCE_CHANCE = 0.5;

    private AttunementRewards() {
    }

    public static int magicXp(@Nullable AttunementRank rank) {
        if (rank == null) {
            return 0;
        }
        return switch (rank) {
            case ADEPT -> 20;
            case MAGUS -> 60;
            case ARCHMAGE -> 250;
        };
    }

    /** How many Essence the creature drops; {@code random} returns values in [0, 1). */
    public static int essenceDrops(@Nullable AttunementRank rank, DoubleSupplier random) {
        if (rank == null) {
            return random.getAsDouble() < PLAIN_ESSENCE_CHANCE ? 1 : 0;
        }
        return switch (rank) {
            case ADEPT -> random.getAsDouble() < ADEPT_ESSENCE_CHANCE ? 1 : 0;
            case MAGUS -> random.getAsDouble() < 0.5 ? 2 : 1;
            case ARCHMAGE -> 3 + Math.min(2, (int) (random.getAsDouble() * 3));
        };
    }
}
