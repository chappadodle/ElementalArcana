package com.chappadodle.elementalarcana.api;

import java.util.Set;

/**
 * The skill tree's keystones (docs/superpowers/specs/2026-10-02-notables-and-keystones-design.md):
 * each changes a rule of magic, for better and for worse, so a build has to choose. The numbers
 * live here; MagicData, PlayerStats, CastingService, Conjuring, ElementalMatchups and
 * KeystoneEvents apply them.
 */
public final class Keystones {
    /** Spells hit 30% harder; 30% less max health. */
    public static final String GLASS_CANNON = "glass_cannon";
    /** Mana regenerates 60% faster; 25% less max mana. */
    public static final String WELLSPRING = "wellspring";
    /** 15% faster and no fall damage; 15% more damage from every element. */
    public static final String GALE_STEP = "gale_step";
    /** 40% more max health and no knockback; 15% slower. */
    public static final String MOUNTAIN_HEART = "mountain_heart";
    /** Spells hit frozen or frosted creatures 30% harder (ice spells frost as they hit); 40% more fire damage taken. */
    public static final String WINTERS_GRASP = "winters_grasp";
    /** Spells (and conjured upkeep) cost health instead of mana, 1 heart per 20, and never cause Mana Sickness. */
    public static final String BLOOD_MAGIC = "blood_magic";
    /** Every third projectile that would hit you turns back at its shooter; melee blows hurt you 20% more. */
    public static final String REFRACTION = "refraction";
    /** Your spell hits arc on to the nearest other foe within 5 blocks for 35% of their damage; 20% less max mana. */
    public static final String CONDUCTOR = "conductor";
    /** In bright light your spells hit 25% harder and you mend; in dim light they hit 20% weaker. */
    public static final String SUNBORN = "sunborn";

    public static final float GLASS_CANNON_POWER = 1.3f;
    public static final double GLASS_CANNON_HEALTH = -0.3;
    public static final float WELLSPRING_REGEN = 1.6f;
    public static final float WELLSPRING_MANA = 0.75f;
    public static final double GALE_STEP_SPEED = 0.15;
    public static final float GALE_STEP_DAMAGE_TAKEN = 1.15f;
    public static final double MOUNTAIN_HEART_HEALTH = 0.4;
    public static final double MOUNTAIN_HEART_SPEED = -0.15;
    public static final float WINTERS_GRASP_DAMAGE = 1.3f;
    public static final float WINTERS_GRASP_FIRE_TAKEN = 1.4f;
    public static final int REFRACTION_EVERY = 3;
    public static final float REFRACTION_MELEE_TAKEN = 1.2f;
    public static final float CONDUCTOR_SHARE = 0.35f;
    public static final double CONDUCTOR_REACH = 5;
    public static final float CONDUCTOR_MANA = 0.8f;
    /** Sunborn's bright light (this light level or more) and dim light (less than this). */
    public static final int SUNBORN_BRIGHT = 12;
    public static final int SUNBORN_DIM = 8;
    public static final float SUNBORN_BRIGHT_POWER = 1.25f;
    public static final float SUNBORN_DIM_POWER = 0.8f;
    public static final int SUNBORN_MEND_TICKS = 60;
    public static final float SUNBORN_MEND = 1f;

    private Keystones() {
    }

    /** Multiplier on spell power. */
    public static float powerFactor(Set<String> keystones) {
        return keystones.contains(GLASS_CANNON) ? GLASS_CANNON_POWER : 1f;
    }

    /** Multiplier on max mana. */
    public static float maxManaFactor(Set<String> keystones) {
        float factor = keystones.contains(WELLSPRING) ? WELLSPRING_MANA : 1f;
        return keystones.contains(CONDUCTOR) ? factor * CONDUCTOR_MANA : factor;
    }

    /** Sunborn's multiplier on spell power at a light level (0 to 15): stronger in bright light, weaker in dim. */
    public static float lightFactor(Set<String> keystones, int light) {
        if (!keystones.contains(SUNBORN)) {
            return 1f;
        }
        return light >= SUNBORN_BRIGHT ? SUNBORN_BRIGHT_POWER : light < SUNBORN_DIM ? SUNBORN_DIM_POWER : 1f;
    }

    /** Whether Refraction turns back the {@code count}th projectile to come at its holder (counting from 1). */
    public static boolean refracts(Set<String> keystones, int count) {
        return keystones.contains(REFRACTION) && count % REFRACTION_EVERY == 0;
    }

    /** Multiplier on melee damage this player takes. */
    public static float meleeTakenFactor(Set<String> keystones) {
        return keystones.contains(REFRACTION) ? REFRACTION_MELEE_TAKEN : 1f;
    }

    /** Multiplier on mana regen. */
    public static float regenFactor(Set<String> keystones) {
        return keystones.contains(WELLSPRING) ? WELLSPRING_REGEN : 1f;
    }

    /** Max health change, as a fraction of the total (-0.3 is 30% less). */
    public static double healthBonus(Set<String> keystones) {
        double bonus = 0;
        if (keystones.contains(GLASS_CANNON)) {
            bonus += GLASS_CANNON_HEALTH;
        }
        if (keystones.contains(MOUNTAIN_HEART)) {
            bonus += MOUNTAIN_HEART_HEALTH;
        }
        return bonus;
    }

    /** Movement speed change, as a fraction of the total. */
    public static double speedBonus(Set<String> keystones) {
        double bonus = 0;
        if (keystones.contains(GALE_STEP)) {
            bonus += GALE_STEP_SPEED;
        }
        if (keystones.contains(MOUNTAIN_HEART)) {
            bonus += MOUNTAIN_HEART_SPEED;
        }
        return bonus;
    }

    /** Multiplier on elemental damage this player takes from {@code element}. */
    public static float damageTakenFactor(Set<String> keystones, Element element) {
        float factor = 1f;
        if (keystones.contains(GALE_STEP)) {
            factor *= GALE_STEP_DAMAGE_TAKEN;
        }
        if (keystones.contains(WINTERS_GRASP) && element == Element.FIRE) {
            factor *= WINTERS_GRASP_FIRE_TAKEN;
        }
        return factor;
    }

    /** Multiplier on this player's spell damage to a creature that is ({@code chilled}) frozen or frosted. */
    public static float damageDealtFactor(Set<String> keystones, boolean chilled) {
        return chilled && keystones.contains(WINTERS_GRASP) ? WINTERS_GRASP_DAMAGE : 1f;
    }
}
