package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test
    void harpiesLetGoAfterThreeSecondsOrAHardHit() {
        assertFalse(WildRules.snatchBroken(10, 2f));
        assertTrue(WildRules.snatchBroken(WildRules.SNATCH_MAX_TICKS, 0f));
        assertTrue(WildRules.snatchBroken(5, WildRules.SNATCH_BREAK_DAMAGE));
    }

    @Test
    void lurkersLetGoAfterThreeSecondsOrOneHeavyBlow() {
        assertFalse(WildRules.holdBroken(30, 7.9f));
        assertTrue(WildRules.holdBroken(WildRules.HOLD_TICKS, 0f));
        assertTrue(WildRules.holdBroken(0, 8f));
    }

    @Test
    void theVolleyFansThreeShards() {
        assertArrayEquals(new float[]{-8f, 0f, 8f}, WildRules.volleyOffsets(), 1e-6f);
    }
}
