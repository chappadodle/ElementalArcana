package com.chappadodle.elementalarcana.api;

/**
 * The Hollowed's numbers (docs/superpowers/specs/2026-10-06-the-hollowed-design.md): what their
 * attacks eat, what a Hungerward saves, and how often and how many come for a player at dusk.
 * Plain Java, unit tested.
 */
public final class HollowedRules {
    /** A Hunger Bolt: its harm, the mana it eats from a player, what that heals its thrower. */
    public static final float BOLT_DAMAGE = 4f;
    public static final float BOLT_MANA = 15f;
    public static final float BOLT_HEAL = 2f;
    /** A Devourer's claws: the mana a hit eats, what that heals it. */
    public static final float CLAW_MANA = 10f;
    public static final float CLAW_HEAL = 3f;
    /** A Herald's hunger: the mana it eats a second from players near it, and how near. */
    public static final float AURA_MANA = 2f;
    public static final double AURA_RADIUS = 12;
    /** The Herald's Pull: how far it reaches, its burst's harm. */
    public static final double PULL_RADIUS = 10;
    public static final float PULL_DAMAGE = 5f;
    /** The harm added when there was no mana left to eat. */
    public static final float STARVING_DAMAGE = 2f;
    /** A Hungerward: the share of eaten mana it saves, and the share of hunger's harm. */
    public static final float WARD_MANA_FACTOR = 0.5f;
    public static final float WARD_DAMAGE_FACTOR = 0.75f;
    /** How near the Hunger Obelisk keeps its people strong, and how near its shattering weakens them. */
    public static final double OBELISK_RADIUS = 16;
    /** The level from which the Hollowed come for a player at dusk. */
    public static final int PATROL_LEVEL = 15;

    private HollowedRules() {
    }

    /** The mana an attack that wants {@code wanted} actually eats from someone with {@code mana} (a Hungerward halves it). */
    public static float manaEaten(float mana, float wanted, boolean warded) {
        return Math.min(Math.max(0, mana), wanted * (warded ? WARD_MANA_FACTOR : 1f));
    }

    /** Whether someone with {@code mana} had too little to feed an attack that wants {@code wanted}: it bites deeper. */
    public static boolean starving(float mana, float wanted) {
        return mana < wanted;
    }

    /** Hunger's harm, a quarter less for one who carries a Hungerward. */
    public static float damage(float damage, boolean warded) {
        return warded ? damage * WARD_DAMAGE_FACTOR : damage;
    }

    /** The chance at dusk that a band comes for a player of {@code level}: none before 15, 8% then, 0.3% more a level, at most 20%. */
    public static double patrolChance(int level) {
        if (level < PATROL_LEVEL) {
            return 0;
        }
        return Math.min(0.2, 0.08 + 0.003 * (level - PATROL_LEVEL));
    }

    /** How many Acolytes come for a player of {@code level}: one, two from level 30. */
    public static int patrolAcolytes(int level) {
        return level >= 30 ? 2 : 1;
    }

    /** How many Devourers: one, two from level 45. */
    public static int patrolDevourers(int level) {
        return level >= 45 ? 2 : 1;
    }
}
