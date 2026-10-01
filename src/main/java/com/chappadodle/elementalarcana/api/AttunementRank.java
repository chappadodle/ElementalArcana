package com.chappadodle.elementalarcana.api;

/**
 * How strong an Attuned creature is. Plain data with no Minecraft types: how rare each rank is,
 * the bonus levels it adds on top of the creature's zone level (ranks ride the same level curve as
 * everything else; see Progression#damageLevelFactor), and how much more mana (XP) a trained caster
 * holds than an ordinary creature of its level.
 */
public enum AttunementRank {
    ADEPT(0.05, 0, 3.0),
    MAGUS(0.01, 8, 5.0),
    ARCHMAGE(0.001, 20, 12.0);

    private final double baseChance;
    private final int bonusLevels;
    private final double xpMultiplier;

    AttunementRank(double baseChance, int bonusLevels, double xpMultiplier) {
        this.baseChance = baseChance;
        this.bonusLevels = bonusLevels;
        this.xpMultiplier = xpMultiplier;
    }

    /** Chance per eligible spawn, before the distance multiplier. */
    public double baseChance() {
        return baseChance;
    }

    /** Levels added on top of the creature's zone level. */
    public int bonusLevels() {
        return bonusLevels;
    }

    /** XP multiplier over an ordinary creature of the same level. */
    public double xpMultiplier() {
        return xpMultiplier;
    }
}
