package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * The dice for creature attunement, free of Minecraft types so they can be unit tested. The spawn
 * hook (Attunement) supplies the nearby player's Magic Level, the distance from world spawn, the
 * biome's elements and a random source.
 */
public final class AttunementRules {
    // Chances grow with distance from world spawn: x2 at 1000 blocks, capped at x3.
    private static final double DISTANCE_PER_STEP = 1000;
    private static final double MAX_DISTANCE_MULTIPLIER = 3;
    private static final double BIOME_ELEMENT_WEIGHT = 4;
    private static final double OTHER_ELEMENT_WEIGHT = 1;
    private static final AttunementRank[] RAREST_FIRST = {AttunementRank.ARCHMAGE, AttunementRank.MAGUS, AttunementRank.ADEPT};

    private AttunementRules() {
    }

    public static double distanceMultiplier(double distanceFromSpawn) {
        return Math.min(MAX_DISTANCE_MULTIPLIER, 1 + distanceFromSpawn / DISTANCE_PER_STEP);
    }

    /**
     * Rolls a rank for a newly spawned creature, rarest first. Every rank the player's Magic Level
     * allows gets its own roll. Returns null when the creature spawns normal.
     */
    @Nullable
    public static AttunementRank rollRank(int magicLevel, double distanceFromSpawn, DoubleSupplier random) {
        double multiplier = distanceMultiplier(distanceFromSpawn);
        for (AttunementRank rank : RAREST_FIRST) {
            if (magicLevel >= rank.requiredMagicLevel() && random.getAsDouble() < rank.baseChance() * multiplier) {
                return rank;
            }
        }
        return null;
    }

    /** Picks an element: the biome's elements are 4 times as likely as the others (all even if none). */
    public static Element pickElement(Set<Element> biomeElements, DoubleSupplier random) {
        double total = 0;
        for (Element element : Element.values()) {
            total += weight(element, biomeElements);
        }
        double roll = random.getAsDouble() * total;
        for (Element element : Element.values()) {
            roll -= weight(element, biomeElements);
            if (roll < 0) {
                return element;
            }
        }
        return Element.values()[Element.values().length - 1];
    }

    private static double weight(Element element, Set<Element> biomeElements) {
        return biomeElements.contains(element) ? BIOME_ELEMENT_WEIGHT : OTHER_ELEMENT_WEIGHT;
    }
}
