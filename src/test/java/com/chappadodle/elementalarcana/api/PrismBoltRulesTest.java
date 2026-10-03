package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrismBoltRulesTest {

    @Test
    void levelOneIsTheOldSpell() {
        assertEquals(1f, PrismBoltRules.damageFactor(1), 1e-6f);
        assertEquals(3, PrismBoltRules.shards(1, null));
        assertEquals(25, PrismBoltRules.spreadDegrees(1, null), 1e-9);
        assertEquals(0, PrismBoltRules.pierce(1, null));
        assertEquals(2.0, PrismBoltRules.speed(null), 1e-9);
        assertEquals(1, PrismBoltRules.bolts(1));
    }

    @Test
    void keenEdgeRefractionAndPiercingLight() {
        assertEquals(1.2f, PrismBoltRules.damageFactor(2), 1e-6f);
        assertEquals(5, PrismBoltRules.shards(3, null));
        assertEquals(18, PrismBoltRules.spreadDegrees(3, null), 1e-9);
        assertEquals(1, PrismBoltRules.pierce(4, null));
    }

    @Test
    void theForks() {
        assertEquals(PrismBoltRules.LANCE_PIERCE, PrismBoltRules.pierce(5, PrismBoltRules.LANCE));
        assertEquals(3.0, PrismBoltRules.speed(PrismBoltRules.LANCE), 1e-9);
        assertFalse(PrismBoltRules.burstsOnCreatures(PrismBoltRules.LANCE));
        assertTrue(PrismBoltRules.burstsOnCreatures(null));
        assertEquals(0, PrismBoltRules.pierce(9, PrismBoltRules.GEODE));
        assertEquals(10, PrismBoltRules.shards(5, PrismBoltRules.GEODE));
        assertEquals(36, PrismBoltRules.spreadDegrees(5, PrismBoltRules.GEODE), 1e-9);
        assertEquals(1.5f, PrismBoltRules.shardFactor(PrismBoltRules.GEODE), 1e-6f);
    }

    @Test
    void theShellBuildsToFourHeartsAndNeverTakesAway() {
        assertEquals(2f, PrismBoltRules.shellAfterHit(0f), 1e-6f);
        assertEquals(8f, PrismBoltRules.shellAfterHit(7f), 1e-6f);
        assertEquals(16f, PrismBoltRules.shellAfterHit(16f), 1e-6f);
    }

    @Test
    void tiersComeInOrder() {
        assertFalse(PrismBoltRules.shell(5));
        assertTrue(PrismBoltRules.shell(6));
        assertEquals(2, PrismBoltRules.bolts(7));
        assertFalse(PrismBoltRules.resonance(7));
        assertTrue(PrismBoltRules.resonance(8));
        assertTrue(PrismBoltRules.splits(9));
    }
}
