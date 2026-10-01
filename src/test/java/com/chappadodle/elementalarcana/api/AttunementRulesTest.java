package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.DoubleSupplier;

import static com.chappadodle.elementalarcana.api.AttunementRank.ADEPT;
import static com.chappadodle.elementalarcana.api.AttunementRank.ARCHMAGE;
import static com.chappadodle.elementalarcana.api.AttunementRank.MAGUS;
import static com.chappadodle.elementalarcana.api.AttunementRules.distanceMultiplier;
import static com.chappadodle.elementalarcana.api.AttunementRules.pickElement;
import static com.chappadodle.elementalarcana.api.AttunementRules.rollRank;
import static com.chappadodle.elementalarcana.api.Element.FIRE;
import static com.chappadodle.elementalarcana.api.Element.ICE;
import static com.chappadodle.elementalarcana.api.Element.WATER;
import static com.chappadodle.elementalarcana.api.Element.WIND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AttunementRulesTest {

    private static DoubleSupplier always(double value) {
        return () -> value;
    }

    @Test
    void rarestRankWinsWithoutALevelGate() {
        // A roll of 0 always hits, so the result is the rarest rank.
        assertEquals(ARCHMAGE, rollRank(0, always(0)));
    }

    @Test
    void baseChancesAtWorldSpawn() {
        assertEquals(ADEPT, rollRank(0, always(0.049)));
        assertNull(rollRank(0, always(0.05)));
        assertEquals(MAGUS, rollRank(0, always(0.0099)));
        assertEquals(ARCHMAGE, rollRank(0, always(0.00099)));
    }

    @Test
    void chancesGrowWithDistanceUpToTriple() {
        assertEquals(1.0, distanceMultiplier(0), 1e-9);
        assertEquals(2.0, distanceMultiplier(1000), 1e-9);
        assertEquals(3.0, distanceMultiplier(2000), 1e-9);
        assertEquals(3.0, distanceMultiplier(50_000), 1e-9);
        assertEquals(ADEPT, rollRank(1000, always(0.099)));
        assertNull(rollRank(1000, always(0.1)));
        assertEquals(ADEPT, rollRank(50_000, always(0.149)));
        assertNull(rollRank(50_000, always(0.151)));
    }

    @Test
    void everyAllowedRankGetsItsOwnRoll() {
        // Archmage and Magus rolls miss, then the Adept roll hits: three rolls in total.
        double[] rolls = {0.5, 0.5, 0.01};
        int[] used = {0};
        assertEquals(ADEPT, rollRank(0, () -> rolls[used[0]++]));
        assertEquals(3, used[0]);
    }

    @Test
    void elementsAreEvenWithoutABiomeElement() {
        assertEquals(FIRE, pickElement(Set.of(), always(0.0)));
        assertEquals(FIRE, pickElement(Set.of(), always(0.24)));
        assertEquals(WATER, pickElement(Set.of(), always(0.26)));
        assertEquals(ICE, pickElement(Set.of(), always(0.51)));
        assertEquals(WIND, pickElement(Set.of(), always(0.99)));
    }

    @Test
    void biomeElementIsFourTimesAsLikely() {
        // With Ice as the biome element the weights are Fire 1, Water 1, Ice 4, Wind 1 (total 7).
        assertEquals(FIRE, pickElement(Set.of(ICE), always(0.5 / 7)));
        assertEquals(WATER, pickElement(Set.of(ICE), always(1.5 / 7)));
        assertEquals(ICE, pickElement(Set.of(ICE), always(2.1 / 7)));
        assertEquals(ICE, pickElement(Set.of(ICE), always(5.9 / 7)));
        assertEquals(WIND, pickElement(Set.of(ICE), always(6.5 / 7)));
    }
}
