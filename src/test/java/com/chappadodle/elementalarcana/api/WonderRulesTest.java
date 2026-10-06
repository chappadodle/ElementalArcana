package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WonderRulesTest {

    @Test
    void mothsComeOutAtNight() {
        assertFalse(WonderRules.mothTime(6000));
        assertFalse(WonderRules.mothTime(12499));
        assertTrue(WonderRules.mothTime(12500));
        assertTrue(WonderRules.mothTime(18000));
        assertFalse(WonderRules.mothTime(23500));
        assertTrue(WonderRules.mothTime(24000 * 3 + 13000));
    }

    @Test
    void twoOrThreeComeButNeverPastSix() {
        assertEquals(2, WonderRules.mothsToBring(0, 0.1));
        assertEquals(3, WonderRules.mothsToBring(0, 0.9));
        assertEquals(1, WonderRules.mothsToBring(5, 0.9));
        assertEquals(0, WonderRules.mothsToBring(6, 0.1));
        assertEquals(0, WonderRules.mothsToBring(9, 0.9));
    }

    @Test
    void theWakeLiftsGentlyButNeverDrops() {
        assertEquals(-0.15, WonderRules.wakeRise(-0.2), 1e-9);
        assertEquals(0.12, WonderRules.wakeRise(0.1), 1e-9);
        // Already rising faster than the wake lifts: left as it is.
        assertEquals(0.5, WonderRules.wakeRise(0.5), 1e-9);
    }
}
