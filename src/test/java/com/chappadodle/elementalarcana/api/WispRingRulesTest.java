package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WispRingRulesTest {

    @Test
    void wispsDanceAtNight() {
        assertFalse(WispRingRules.isNight(6000));
        assertFalse(WispRingRules.isNight(12000));
        assertTrue(WispRingRules.isNight(13000));
        assertTrue(WispRingRules.isNight(18000));
        assertFalse(WispRingRules.isNight(23000));
        assertTrue(WispRingRules.isNight(24000 * 5 + 20000));
    }

    @Test
    void aNightRunsFromDuskToTheNextNoon() {
        long first = WispRingRules.night(13000);
        assertEquals(first, WispRingRules.night(22999));
        assertEquals(first, WispRingRules.night(24000 + 6000));
        assertEquals(first + 1, WispRingRules.night(24000 + 13000));
        assertEquals(first + 1, WispRingRules.night(24000 * 2 + 1000));
    }

    @Test
    void twoBoonsInThree() {
        assertTrue(WispRingRules.pick(0.0, 0.0).boon());
        assertTrue(WispRingRules.pick(0.66, 0.99).boon());
        assertFalse(WispRingRules.pick(0.67, 0.0).boon());
        assertFalse(WispRingRules.pick(0.99, 0.99).boon());
        assertEquals(WispRingRules.Outcome.FEY_LUCK, WispRingRules.pick(0.1, 0.0));
        assertEquals(WispRingRules.Outcome.FULL_MOON, WispRingRules.pick(0.1, 0.999));
        assertEquals(WispRingRules.Outcome.WILL_O_THE_WISP, WispRingRules.pick(0.9, 0.999));
    }

    @Test
    void theRingIsSevenAcross() {
        assertTrue(WispRingRules.inside(0, 0));
        assertTrue(WispRingRules.inside(3, 0));
        assertTrue(WispRingRules.inside(2, 2));
        assertFalse(WispRingRules.inside(4, 0));
        assertFalse(WispRingRules.inside(3, 3));
    }
}
