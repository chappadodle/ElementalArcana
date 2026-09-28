package com.chappadodle.elementalarcana.api;

/**
 * How strong an Attuned creature is. Plain data with no Minecraft types: how rare each rank is,
 * the Magic Level a nearby player needs before it can appear, and its bonus max health (a share of
 * the base value: 0.5 = +50%, 2.0 = three times the health).
 */
public enum AttunementRank {
    ADEPT(0.05, 1, 0.0),
    MAGUS(0.01, 5, 0.5),
    ARCHMAGE(0.001, 10, 2.0);

    private final double baseChance;
    private final int requiredMagicLevel;
    private final double healthBonus;

    AttunementRank(double baseChance, int requiredMagicLevel, double healthBonus) {
        this.baseChance = baseChance;
        this.requiredMagicLevel = requiredMagicLevel;
        this.healthBonus = healthBonus;
    }

    /** Chance per eligible spawn, before the distance multiplier. */
    public double baseChance() {
        return baseChance;
    }

    public int requiredMagicLevel() {
        return requiredMagicLevel;
    }

    public double healthBonus() {
        return healthBonus;
    }
}
