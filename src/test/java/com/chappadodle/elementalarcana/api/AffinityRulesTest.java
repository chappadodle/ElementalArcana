package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.chappadodle.elementalarcana.api.AffinityRules.blockingOpposite;
import static com.chappadodle.elementalarcana.api.AffinityRules.damageTaken;
import static com.chappadodle.elementalarcana.api.Element.FIRE;
import static com.chappadodle.elementalarcana.api.Element.ICE;
import static com.chappadodle.elementalarcana.api.Element.WATER;
import static com.chappadodle.elementalarcana.api.Element.WIND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AffinityRulesTest {

    @Test
    void fireBlocksWaterAndIceUntilLevel30() {
        assertEquals(FIRE, blockingOpposite(Set.of(FIRE), WATER, 29));
        assertEquals(FIRE, blockingOpposite(Set.of(FIRE), ICE, 29));
        assertNull(blockingOpposite(Set.of(FIRE), WATER, 30));
        assertNull(blockingOpposite(Set.of(FIRE), ICE, 30));
    }

    @Test
    void compatibleElementsAreNeverBlocked() {
        assertNull(blockingOpposite(Set.of(FIRE), WIND, 10));
        assertNull(blockingOpposite(Set.of(WATER), ICE, 10));
        assertNull(blockingOpposite(Set.of(WATER, ICE), WIND, 20));
        assertNull(blockingOpposite(Set.of(), FIRE, 1));
    }

    @Test
    void waterOrIceBlockFire() {
        assertEquals(WATER, blockingOpposite(Set.of(WATER), FIRE, 20));
        assertEquals(ICE, blockingOpposite(Set.of(WIND, ICE), FIRE, 20));
    }

    @Test
    void ownElementsHitForLess() {
        assertEquals(0.75f, damageTaken(Set.of(FIRE), FIRE), 1e-6f);
        assertEquals(0.75f, damageTaken(Set.of(FIRE, WIND), WIND), 1e-6f);
        assertEquals(1f, damageTaken(Set.of(FIRE), WATER), 1e-6f);
        assertEquals(1f, damageTaken(Set.of(), ICE), 1e-6f);
    }
}
