package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmiteRulesTest {

    @Test
    void levelOneIsTheOldSpell() {
        assertEquals(2.5, SmiteRules.radius(1), 1e-9);
        assertEquals(15, SmiteRules.delay(1));
        assertEquals(1.5f, SmiteRules.undead(1), 1e-6f);
        assertEquals(3, SmiteRules.fireSeconds(1));
        assertFalse(SmiteRules.consecrates(1));
    }

    @Test
    void wideJudgmentAndSwiftVerdict() {
        assertEquals(3.5, SmiteRules.radius(2), 1e-9);
        assertEquals(10, SmiteRules.delay(3));
    }

    @Test
    void holyFireJudgesTheUndeadHarder() {
        assertEquals(2f, SmiteRules.undead(7), 1e-6f);
        assertEquals(6, SmiteRules.fireSeconds(7));
    }

    @Test
    void wrathRainsOverTwoSeconds() {
        assertEquals(SmiteRules.SMALL_DELAY, SmiteRules.wrathDelay(0));
        assertTrue(SmiteRules.wrathDelay(SmiteRules.WRATH_PILLARS - 1) <= SmiteRules.SMALL_DELAY + SmiteRules.WRATH_TICKS);
    }

    @Test
    void tiersComeInOrder() {
        assertTrue(SmiteRules.consecrates(4));
        assertFalse(SmiteRules.purges(5));
        assertTrue(SmiteRules.purges(6));
        assertFalse(SmiteRules.halo(7));
        assertTrue(SmiteRules.halo(8));
        assertTrue(SmiteRules.bursts(9));
    }
}
