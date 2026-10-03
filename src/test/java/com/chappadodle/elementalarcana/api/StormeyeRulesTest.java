package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StormeyeRulesTest {

    @Test
    void widerEyeReachesFurther() {
        assertEquals(5, StormeyeRules.reach(3, null, null), 1e-9);
        assertEquals(7, StormeyeRules.reach(4, null, null), 1e-9);
    }

    @Test
    void twinStormsAreSmallerAndGreatTempestDoubles() {
        assertEquals(4.2, StormeyeRules.reach(5, StormeyeRules.TWIN, null), 1e-9);
        assertEquals(14, StormeyeRules.reach(10, StormeyeRules.WANDERING, StormeyeRules.GREAT_TEMPEST), 1e-9);
        assertEquals(8.4, StormeyeRules.reach(10, StormeyeRules.TWIN, StormeyeRules.GREAT_TEMPEST), 1e-9);
        assertEquals(14, StormeyeRules.height(StormeyeRules.GREAT_TEMPEST), 1e-9);
        assertEquals(7, StormeyeRules.height(StormeyeRules.EYE_OF_CALM), 1e-9);
    }

    @Test
    void galeForcePullsHarderAndLastsLonger() {
        assertTrue(StormeyeRules.pull(2) > StormeyeRules.pull(1));
        assertEquals(80, StormeyeRules.lifetime(1));
        assertEquals(100, StormeyeRules.lifetime(2));
    }

    @Test
    void scavengerWindLastsSevenSeconds() {
        assertEquals(100, StormeyeRules.lifetime(7));
        assertEquals(140, StormeyeRules.lifetime(8));
    }

    @Test
    void crushingWindsHitsHalfAgainAsHard() {
        assertEquals(1.5f, StormeyeRules.damage(5), 1e-6f);
        assertEquals(2.25f, StormeyeRules.damage(6), 1e-6f);
    }
}
