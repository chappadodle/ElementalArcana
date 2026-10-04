package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildRulesTest {

    @Test
    void fireBurnsTreantsAndTearsWraiths() {
        assertEquals(15f, WildRules.treantDamage(10f, true));
        assertEquals(10f, WildRules.treantDamage(10f, false));
        assertEquals(15f, WildRules.wraithDamage(10f, true));
        assertEquals(10f, WildRules.wraithDamage(10f, false));
    }

    @Test
    void aWraithsTouchFreezesOnlyTheFrostedThrough() {
        assertFalse(WildRules.touchFreezes(0, 140));
        assertFalse(WildRules.touchFreezes(139, 140));
        assertTrue(WildRules.touchFreezes(140, 140));
    }

    @Test
    void cooldowns() {
        assertTrue(WildRules.ready(100, 100));
        assertFalse(WildRules.ready(101, 100));
    }

    @Test
    void theyAreBornAWaysOff() {
        assertTrue(WildRules.MIN_DISTANCE < WildRules.MAX_DISTANCE);
        assertTrue(WildRules.SPACING > WildRules.MAX_DISTANCE);
    }
}
