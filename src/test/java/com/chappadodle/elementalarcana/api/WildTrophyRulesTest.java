package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildTrophyRulesTest {
    @Test
    void restsOnlyByDayInTheOpenOnTheEarthWhenLeftAlone() {
        assertTrue(WildTrophyRules.canRest(true, true, true, 1000, 1000 - WildTrophyRules.REST_CALM_TICKS));
        assertFalse(WildTrophyRules.canRest(true, true, true, 1000, 1000 - WildTrophyRules.REST_CALM_TICKS + 1));
        assertFalse(WildTrophyRules.canRest(false, true, true, 1000, 0));
        assertFalse(WildTrophyRules.canRest(true, false, true, 1000, 0));
        assertFalse(WildTrophyRules.canRest(true, true, false, 1000, 0));
    }

    @Test
    void aFlickerHalvesTheHit() {
        assertEquals(3f, WildTrophyRules.flickerDamage(6f), 1e-6);
        assertTrue(WildTrophyRules.FLICKER_MIN < WildTrophyRules.FLICKER_MAX);
    }

    @Test
    void theCrustDiscReachesTwoBlocksButNotItsCorners() {
        assertTrue(WildTrophyRules.inCrustDisc(0, 0));
        assertTrue(WildTrophyRules.inCrustDisc(1.5, 0.5));
        assertFalse(WildTrophyRules.inCrustDisc(1.5, 1.5));
        assertFalse(WildTrophyRules.inCrustDisc(2.5, 0));
    }

    @Test
    void theCrustAgesThenMelts() {
        assertEquals(1, WildTrophyRules.nextCrustAge(0));
        assertEquals(3, WildTrophyRules.nextCrustAge(2));
        assertEquals(-1, WildTrophyRules.nextCrustAge(WildTrophyRules.CRUST_MAX_AGE));
    }

    @Test
    void thePlumeWardsFallsOfUpToEightBlocks() {
        assertTrue(WildTrophyRules.fallWarded(8f));
        assertFalse(WildTrophyRules.fallWarded(8.5f));
        assertTrue(WildTrophyRules.LEAP_BOOST > 0 && WildTrophyRules.CLIMB_SPEED > 0 && WildTrophyRules.SWIM_BOOST > 0);
    }
}
