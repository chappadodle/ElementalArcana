package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrakeRulesTest {

    @Test
    void theBreathIsAConeTenBlocksLong() {
        // Straight ahead, near and at the end of its reach.
        assertTrue(DrakeRules.inBreath(0, 0, 3, 0, 0, 1));
        assertTrue(DrakeRules.inBreath(0, 0, 10, 0, 0, 1));
        assertFalse(DrakeRules.inBreath(0, 0, 10.5, 0, 0, 1));
        // Off to the side: within 30 degrees, not beyond.
        assertTrue(DrakeRules.inBreath(2.5, 0, 5, 0, 0, 1));
        assertFalse(DrakeRules.inBreath(4, 0, 5, 0, 0, 1));
        // Behind it, never.
        assertFalse(DrakeRules.inBreath(0, 0, -3, 0, 0, 1));
    }

    @Test
    void aQuarterHealthGroundsIt() {
        assertFalse(DrakeRules.grounded(36, 140));
        assertTrue(DrakeRules.grounded(35, 140));
    }

    @Test
    void itSoarsEighteenToTwentyEightBlocksUp() {
        assertEquals(18, DrakeRules.soarHeight(0), 1e-9);
        assertEquals(28, DrakeRules.soarHeight(1), 1e-9);
        assertEquals(28, DrakeRules.soarHeight(3), 1e-9);
    }

    @Test
    void fiveDrakes() {
        assertEquals(5, DrakeRules.ELEMENTS.size());
        assertFalse(DrakeRules.ELEMENTS.contains(Element.EARTH));
    }
}
