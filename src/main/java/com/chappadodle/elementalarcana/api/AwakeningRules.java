package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * The dice for awakening (docs/superpowers/specs/2026-10-01-awakening-design.md). Plain Java, no
 * Minecraft types (unit tested). Your first element wakes by itself: a small chance each Minecraft
 * day, a much bigger one from day 10, certain by day 16, and a one-in-three chance each time you
 * survive a brush with an element (certain from day 10). A derived element (Ice) is a hundredth as
 * likely as its base element. Extra elements come from Catalysts: each family you already hold makes
 * the next about 4 times harder, and a level milestone (10, 20, 30...) makes it realistic.
 */
public final class AwakeningRules {
    public static final int DAY_TICKS = 24000;
    public static final int RAMP_DAY = 10;
    public static final int GUARANTEE_DAY = 16;
    /** A derived element's weight against its base element's 1. */
    public static final double DERIVED_WEIGHT = 0.01;
    private static final double BASE_DAILY_CHANCE = 0.03;
    private static final double RAMP_START_CHANCE = 0.25;
    private static final double RAMP_PER_DAY = 0.15;
    private static final double BRUSH_CHANCE = 1.0 / 3;
    private static final double BRUSH_RAMP_FACTOR = 3;
    private static final double CATALYST_FIRST_CHANCE = 0.25;
    private static final double CATALYST_FALLOFF = 4;
    private static final double BEFORE_MILESTONE_SHARE = 0.04;
    private static final double PER_LEVEL_AFTER_MILESTONE = 0.04;
    private static final double MAX_LEVEL_FACTOR = 3;
    private static final double FAILURE_BONUS = 0.10;
    private static final int MILESTONE_LEVELS = 10;

    private AwakeningRules() {
    }

    /** Whole days since {@code startDayTime} on the world's day clock; never below 0 (so `/time set` can't undo days). */
    public static int day(long dayTime, long startDayTime) {
        return (int) Math.max(0, (dayTime - startDayTime) / DAY_TICKS);
    }

    /** The chance that a new day wakes your magic by itself. */
    public static double dailyChance(int day) {
        if (day >= GUARANTEE_DAY) {
            return 1;
        }
        if (day < RAMP_DAY) {
            return BASE_DAILY_CHANCE;
        }
        return Math.min(1, RAMP_START_CHANCE + RAMP_PER_DAY * (day - RAMP_DAY));
    }

    /** The chance that surviving a brush with an element wakes your magic. */
    public static double brushChance(int day) {
        return Math.min(1, day >= RAMP_DAY ? BRUSH_CHANCE * BRUSH_RAMP_FACTOR : BRUSH_CHANCE);
    }

    /** Weights for one family: its base element 100, a derived element 1. */
    public static Map<Element, Double> weights(Element family) {
        Map<Element, Double> weights = new EnumMap<>(Element.class);
        for (Element element : Element.values()) {
            if (element.family() == family.family()) {
                weights.put(element, element.derived() ? 100 * DERIVED_WEIGHT : 100.0);
            }
        }
        return weights;
    }

    /** Weights across every family equally (each family totals 100), then base against derived within it. */
    public static Map<Element, Double> anyWeights() {
        Map<Element, Double> weights = new EnumMap<>(Element.class);
        for (Element family : Element.values()) {
            if (family.family() != family) {
                continue;
            }
            Map<Element, Double> inside = weights(family);
            double total = inside.values().stream().mapToDouble(Double::doubleValue).sum();
            inside.forEach((element, weight) -> weights.put(element, weight / total * 100));
        }
        return weights;
    }

    /** The element at {@code roll} (0 to 1) in the weighted map, or null for an empty map. */
    @Nullable
    public static Element pick(Map<Element, Double> weights, double roll) {
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            return null;
        }
        double left = roll * total;
        Element last = null;
        for (Map.Entry<Element, Double> entry : weights.entrySet()) {
            last = entry.getKey();
            left -= entry.getValue();
            if (left < 0) {
                return last;
            }
        }
        return last;
    }

    /** The level from which a Catalyst is a realistic try when you hold {@code familiesHeld} families: 10, 20, 30... */
    public static int milestoneLevel(int familiesHeld) {
        return MILESTONE_LEVELS * Math.max(1, familiesHeld);
    }

    /** A Catalyst's base chance when you hold {@code familiesHeld} families: 25%, 6.25%, 1.56%... */
    public static double catalystBase(int familiesHeld) {
        return CATALYST_FIRST_CHANCE / Math.pow(CATALYST_FALLOFF, Math.max(1, familiesHeld) - 1);
    }

    /**
     * A Catalyst's chance: 4% of the base before the milestone level, the base at it, rising 4% of the
     * base per level after (up to 3 times), plus 10% of the base for each failure since the last success.
     */
    public static double catalystChance(int familiesHeld, int level, int failures) {
        double base = catalystBase(familiesHeld);
        int milestone = milestoneLevel(familiesHeld);
        double levelFactor = level < milestone ? BEFORE_MILESTONE_SHARE
                : Math.min(MAX_LEVEL_FACTOR, 1 + PER_LEVEL_AFTER_MILESTONE * (level - milestone));
        return Math.min(1, base * levelFactor + FAILURE_BONUS * base * Math.max(0, failures));
    }
}
