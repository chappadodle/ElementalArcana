package com.chappadodle.elementalarcana.api;

import java.util.List;

/**
 * The Sovereigns' numbers (docs/superpowers/specs/2026-10-03-sanctums-and-sovereigns-design.md).
 * Plain Java, unit tested.
 */
public final class SovereignRules {
    /** One Sovereign for each base element. */
    public static final List<Element> ELEMENTS = List.of(Element.FIRE, Element.WATER, Element.WIND, Element.EARTH);
    /** A Sovereign's zone level never counts for less than this (before its Archmage bonus). */
    public static final int MIN_BASE_LEVEL = 30;
    /** At or below this share of its health, a Sovereign fights in its second phase. */
    public static final float SECOND_PHASE = 0.5f;
    /** Its element's creature spells come round this much faster. */
    public static final float COOLDOWN_SCALE = 0.6f;
    private static final int MIN_COOLDOWN_TICKS = 20;
    /** Its wind-up and the least gap between two casts (creatures: 10 and 40). */
    public static final int WINDUP_TICKS = 8;
    public static final int MIN_GAP_TICKS = 25;
    /** How far it may stray from its seal. */
    public static final double LEASH = 16;
    /** How long without anyone to fight before it goes back to its seal and heals. */
    public static final int CALM_TICKS = 400;
    /** Its own spells' power (a player's Lv 1 spell is 1; creatures cast at 0.6). 0.8 since the balance pass (was 1). */
    public static final float POWER = 0.8f;
    /** The share of damage it still takes while its Bulwark stands (Orvald). */
    public static final float BULWARK_DAMAGE = 0.25f;

    private SovereignRules() {
    }

    /** The Sovereign's base level where it stands: the zone's, but never under 30. */
    public static int baseLevel(int zoneLevel) {
        return Math.max(MIN_BASE_LEVEL, zoneLevel);
    }

    public static boolean secondPhase(float health, float maxHealth) {
        return maxHealth > 0 && health <= maxHealth * SECOND_PHASE;
    }

    /** One of its element's creature spells' cooldown, quickened (never under a second). */
    public static int cooldown(int creatureTicks) {
        return Math.max(MIN_COOLDOWN_TICKS, Math.round(creatureTicks * COOLDOWN_SCALE));
    }

    /** Whether {@code element} has a Sovereign. */
    public static boolean hasSovereign(Element element) {
        return ELEMENTS.contains(element);
    }
}
