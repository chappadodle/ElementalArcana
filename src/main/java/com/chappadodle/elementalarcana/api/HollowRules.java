package com.chappadodle.elementalarcana.api;

import java.util.List;

/**
 * The Hollow's numbers (docs/superpowers/specs/2026-10-03-the-hollow-design.md). Plain Java, unit
 * tested.
 */
public final class HollowRules {
    /** The forms it fights in, a quarter of its health each, in this order. */
    public static final List<Element> FORMS = List.of(Element.FIRE, Element.WATER, Element.WIND, Element.EARTH);
    /** Its level before its story-boss bonus: it is level 60. */
    public static final int BASE_LEVEL = 50;
    /** How far its hunger reaches, and the mana it eats from each a second (health, once there's none). */
    public static final double HUNGER_RADIUS = 24;
    public static final float MANA_EATEN = 5f;
    public static final float STARVING_DAMAGE = 1f;
    /** A Devouring: how far it pulls, and how hard it bursts. */
    public static final double DEVOUR_RADIUS = 16;
    public static final float DEVOUR_DAMAGE = 6f;
    /** How far it may drift from its maw. */
    public static final double LEASH = 20;
    /** Ticks from its fall until the Hollow lets everyone go. */
    public static final int RELEASE_TICKS = 200;
    /** The tree points the Heart of the Prime gives, besides every element. */
    public static final int PRIME_TREE_POINTS = 5;

    private HollowRules() {
    }

    /** Its form at {@code health} of {@code maxHealth}: Fire above 75%, Water above 50%, Wind above 25%, then Earth. */
    public static Element formAt(float health, float maxHealth) {
        float share = maxHealth <= 0 ? 0 : health / maxHealth;
        int index = share > 0.75f ? 0 : share > 0.5f ? 1 : share > 0.25f ? 2 : 3;
        return FORMS.get(index);
    }

    /** The mana its hunger takes this second from someone who has {@code mana}. */
    public static float manaEaten(float mana) {
        return Math.min(Math.max(0, mana), MANA_EATEN);
    }

    /** Whether someone with {@code mana} has too little left to feed it, so it eats their health. */
    public static boolean starving(float mana) {
        return mana < MANA_EATEN;
    }
}
