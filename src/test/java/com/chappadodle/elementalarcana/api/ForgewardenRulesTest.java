package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgewardenRulesTest {

    @Test
    void itTurnsMoltenAtHalfItsHealth() {
        assertFalse(ForgewardenRules.molten(101f, 200f));
        assertTrue(ForgewardenRules.molten(100f, 200f));
        assertTrue(ForgewardenRules.molten(1f, 200f));
    }

    @Test
    void itHurlsMagmaOnlyAtFoesFarOffButInReach() {
        assertFalse(ForgewardenRules.hurls(3));
        assertTrue(ForgewardenRules.hurls(6));
        assertTrue(ForgewardenRules.hurls(24));
        assertFalse(ForgewardenRules.hurls(30));
    }

    @Test
    void itLeavesOneCoreAndSometimesTwoOnHard() {
        assertEquals(1, ForgewardenRules.cores(false, 0.1));
        assertEquals(2, ForgewardenRules.cores(true, 0.1));
        assertEquals(1, ForgewardenRules.cores(true, 0.9));
    }

    @Test
    void itsSlamReachesFartherThanAGolems() {
        assertTrue(ForgewardenRules.REACH > GolemRules.REACH);
        assertTrue(ForgewardenRules.SLAM_RADIUS > GolemRules.SLAM_RADIUS);
    }
}
