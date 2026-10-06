package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuelRulesTest {

    @Test
    void aBlowThatLeavesAFifthOrMoreIsDealtInFull() {
        assertFalse(DuelRules.yields(20f, 20f, 16f));
        assertEquals(16f, DuelRules.capped(20f, 20f, 16f), 1e-6);
    }

    @Test
    void aBlowPastTheFifthStopsAtIt() {
        assertTrue(DuelRules.yields(20f, 20f, 17f));
        assertEquals(16f, DuelRules.capped(20f, 20f, 17f), 1e-6);
        assertEquals(6f, DuelRules.capped(10f, 20f, 100f), 1e-6);
    }

    @Test
    void someoneAlreadyBelowTheFifthTakesNothingMore() {
        assertTrue(DuelRules.yields(3f, 20f, 1f));
        assertEquals(0f, DuelRules.capped(3f, 20f, 1f), 1e-6);
    }

    @Test
    void theRingHasARimToStepOnto() {
        assertTrue(DuelRules.inRing(3, 4, false));
        assertFalse(DuelRules.inRing(4, 4, false));
        assertTrue(DuelRules.inRing(4, 4, true));
        assertFalse(DuelRules.inRing(5, 5, true));
    }

    @Test
    void theCountGoesThreeTwoOne() {
        assertEquals(3, DuelRules.count(0));
        assertEquals(3, DuelRules.count(19));
        assertEquals(2, DuelRules.count(20));
        assertEquals(1, DuelRules.count(59));
        assertEquals(0, DuelRules.count(60));
    }

    @Test
    void theArchmagisterPaysMore() {
        assertEquals(2, DuelRules.prize(false));
        assertEquals(5, DuelRules.prize(true));
    }
}
