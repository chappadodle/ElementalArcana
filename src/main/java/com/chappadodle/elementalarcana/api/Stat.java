package com.chappadodle.elementalarcana.api;

import java.util.Locale;

/**
 * The stats every creature has, besides one Affinity per element family (see StatRules). Each
 * level gives a player a stat point to put into one of them. {@code exponent} is how hard the stat
 * leans on the shared curve: 1 = full strength, 0.5 = half (its square root).
 */
public enum Stat {
    /** Mana pool and regeneration. */
    RESERVOIR(1.0),
    /** Spell power. */
    POTENCY(0.5),
    /** Shorter cooldowns. */
    FOCUS(0.75),
    /** Less elemental damage taken. */
    WARD(0.5),
    /** Max health. */
    VITALITY(0.5),
    /** Stronger reactions and more Essence. */
    INSIGHT(0.5);

    private final double exponent;

    Stat(double exponent) {
        this.exponent = exponent;
    }

    public double exponent() {
        return exponent;
    }

    /** Its save and network key, e.g. "reservoir". */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
