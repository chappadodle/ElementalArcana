package com.chappadodle.elementalarcana.api;

/**
 * What stat points do (see docs/superpowers/specs/2026-10-01-progression-system-design.md). Plain
 * Java, unit tested. Every stat rides one soft-capped curve: each point is worth x1.04 up to 20
 * points, x1.02 up to 40 and x1.01 after that (about x6 at 100). A stat uses the curve to its
 * exponent (see Stat). Creatures use the same rules, with their points spread evenly over Vitality,
 * Ward and Potency.
 */
public final class StatRules {
    public static final double AFFINITY_EXPONENT = 0.5;
    private static final float BASE_MANA = 100f;
    private static final float MANA_PER_LEVEL = 5f;
    private static final float BASE_REGEN = 2.5f;
    private static final float REGEN_PER_LEVEL = 0.1f;

    private StatRules() {
    }

    /** The shared curve: x1.04 a point up to 20, x1.02 up to 40, x1.01 after. */
    public static double curve(int points) {
        int p = Math.max(0, points);
        return Math.pow(1.04, Math.min(p, 20)) * Math.pow(1.02, Math.clamp(p - 20, 0, 20)) * Math.pow(1.01, Math.max(0, p - 40));
    }

    /** The curve to {@code exponent}. */
    public static double effect(int points, double exponent) {
        return Math.pow(curve(points), exponent);
    }

    public static float maxMana(int level, int reservoir) {
        return (float) ((BASE_MANA + MANA_PER_LEVEL * (Math.max(1, level) - 1)) * effect(reservoir, Stat.RESERVOIR.exponent()));
    }

    public static float regenPerSecond(int level, int reservoir) {
        return (float) ((BASE_REGEN + REGEN_PER_LEVEL * (Math.max(1, level) - 1)) * effect(reservoir, Stat.RESERVOIR.exponent()));
    }

    /** Spell power: Potency for every spell, times the Affinity of the spell's element family. */
    public static float spellPower(int potency, int affinity) {
        return (float) (effect(potency, Stat.POTENCY.exponent()) * effect(affinity, AFFINITY_EXPONENT));
    }

    /** Multiplier on cooldowns, from Focus. */
    public static float cooldownFactor(int focus) {
        return (float) (1 / effect(focus, Stat.FOCUS.exponent()));
    }

    /** Multiplier on elemental damage taken, from Ward. */
    public static float wardFactor(int ward) {
        return (float) (1 / effect(ward, Stat.WARD.exponent()));
    }

    /** Multiplier on max health, from Vitality. */
    public static float healthMultiplier(int vitality) {
        return (float) effect(vitality, Stat.VITALITY.exponent());
    }

    /** Multiplier on reaction hits and Essence chances, from Insight. */
    public static float insightFactor(int insight) {
        return (float) effect(insight, Stat.INSIGHT.exponent());
    }

    /** A creature's points in each of Vitality, Ward and Potency: a third of its level's points. */
    public static int creaturePoints(int level) {
        return (Math.max(1, level) - 1) / 3;
    }
}
