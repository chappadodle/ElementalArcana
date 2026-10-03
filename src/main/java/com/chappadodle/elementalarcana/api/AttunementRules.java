package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.DoubleSupplier;

/**
 * The dice for creature attunement, free of Minecraft types so they can be unit tested. The spawn
 * hook (Attunement) supplies the distance from world spawn, the biome's elements and a random
 * source. Every rank can appear anywhere: an Archmage near spawn is 20 levels above its zone.
 */
public final class AttunementRules {
    // Chances grow with distance from world spawn: x2 at 1000 blocks, capped at x3.
    private static final double DISTANCE_PER_STEP = 1000;
    private static final double MAX_DISTANCE_MULTIPLIER = 3;
    private static final double BIOME_ELEMENT_WEIGHT = 4;
    private static final double OTHER_ELEMENT_WEIGHT = 1;
    private static final AttunementRank[] RAREST_FIRST = {AttunementRank.ARCHMAGE, AttunementRank.MAGUS, AttunementRank.ADEPT};
    /** The elements creatures spawn Attuned to (the ones with their own creature spells). */
    public static final List<Element> ATTUNABLE = List.of(Element.FIRE, Element.WATER, Element.ICE, Element.WIND, Element.EARTH);

    private AttunementRules() {
    }

    public static double distanceMultiplier(double distanceFromSpawn) {
        return Math.min(MAX_DISTANCE_MULTIPLIER, 1 + distanceFromSpawn / DISTANCE_PER_STEP);
    }

    /**
     * Rolls a rank for a newly spawned creature, rarest first, each rank with its own roll. Returns
     * null when the creature spawns normal.
     */
    @Nullable
    public static AttunementRank rollRank(double distanceFromSpawn, DoubleSupplier random) {
        return rollRank(distanceFromSpawn, 1, random);
    }

    /** As above, with every rank's chance multiplied by {@code boost} (a mana tide doubles them). */
    public static AttunementRank rollRank(double distanceFromSpawn, double boost, DoubleSupplier random) {
        double multiplier = distanceMultiplier(distanceFromSpawn) * boost;
        for (AttunementRank rank : RAREST_FIRST) {
            if (random.getAsDouble() < rank.baseChance() * multiplier) {
                return rank;
            }
        }
        return null;
    }

    /**
     * Picks an element among those creatures can be Attuned to (ATTUNABLE): the biome's elements are
     * 4 times as likely as the others (all even if none).
     */
    public static Element pickElement(Set<Element> biomeElements, DoubleSupplier random) {
        double total = 0;
        for (Element element : ATTUNABLE) {
            total += weight(element, biomeElements);
        }
        double roll = random.getAsDouble() * total;
        for (Element element : ATTUNABLE) {
            roll -= weight(element, biomeElements);
            if (roll < 0) {
                return element;
            }
        }
        return ATTUNABLE.get(ATTUNABLE.size() - 1);
    }

    private static double weight(Element element, Set<Element> biomeElements) {
        return biomeElements.contains(element) ? BIOME_ELEMENT_WEIGHT : OTHER_ELEMENT_WEIGHT;
    }
}
