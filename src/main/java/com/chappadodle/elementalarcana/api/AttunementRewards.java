package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleSupplier;

/**
 * The Elemental Essence elemental creatures drop for the player who kills them (see
 * CreatureRewards; their XP is Progression#killXp). Plain Java, no Minecraft types (unit tested).
 * A null rank means a plain innate creature, like a normal blaze. {@code chanceMultiplier} is the
 * killer's Insight (StatRules#insightFactor): it raises every chance, each capped at certain.
 */
public final class AttunementRewards {
    private static final double PLAIN_ESSENCE_CHANCE = 0.05;
    private static final double ADEPT_ESSENCE_CHANCE = 0.5;
    private static final double MAGUS_SECOND_CHANCE = 0.5;

    private AttunementRewards() {
    }

    /** How many Essence the creature drops; {@code random} returns values in [0, 1). */
    public static int essenceDrops(@Nullable AttunementRank rank, double chanceMultiplier, DoubleSupplier random) {
        if (rank == null) {
            return random.getAsDouble() < Math.min(1, PLAIN_ESSENCE_CHANCE * chanceMultiplier) ? 1 : 0;
        }
        return switch (rank) {
            case ADEPT -> random.getAsDouble() < Math.min(1, ADEPT_ESSENCE_CHANCE * chanceMultiplier) ? 1 : 0;
            case MAGUS -> random.getAsDouble() < Math.min(1, MAGUS_SECOND_CHANCE * chanceMultiplier) ? 2 : 1;
            case ARCHMAGE -> 3 + Math.min(2, (int) (random.getAsDouble() * 3));
        };
    }
}
