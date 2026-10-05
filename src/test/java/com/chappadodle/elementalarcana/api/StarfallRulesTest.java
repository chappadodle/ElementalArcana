package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarfallRulesTest {
    @Test
    void starsFallInTheDeepOfTheNight() {
        assertEquals(StarfallRules.EARLIEST, StarfallRules.fallTime(0));
        assertTrue(StarfallRules.fallTime(0.999) < StarfallRules.LATEST);
    }

    @Test
    void crossingAMarkWrapsAtMidnight() {
        assertTrue(StarfallRules.crosses(12499, 12500, 12500));
        assertFalse(StarfallRules.crosses(12500, 12501, 12500));
        assertTrue(StarfallRules.crosses(23999, 24000 + 1, 0));
        assertTrue(StarfallRules.crosses(36499, 36500, 12500));
    }

    @Test
    void directionsAreCompassPoints() {
        assertEquals("north", StarfallRules.direction(0, -10));
        assertEquals("east", StarfallRules.direction(10, 0));
        assertEquals("south_west", StarfallRules.direction(-10, 10));
        assertEquals("north_west", StarfallRules.direction(-10, -10));
    }

    @Test
    void lanternsWardTwentyFourBlocks() {
        assertTrue(StarfallRules.lanternWards(24 * 24));
        assertFalse(StarfallRules.lanternWards(24 * 24 + 1));
    }
}
