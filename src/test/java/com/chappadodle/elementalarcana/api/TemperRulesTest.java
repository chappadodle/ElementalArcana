package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemperRulesTest {
    private static final Map<String, Integer> ADEPT_STAFF = Map.of("potency", 4, "focus", 2, "affinity/fire", 6);

    @Test
    void aPieceTakesThreeTempers() {
        assertTrue(TemperRules.canTemper(0));
        assertTrue(TemperRules.canTemper(2));
        assertFalse(TemperRules.canTemper(3));
    }

    @Test
    void eachTemperAddsOneToEachStat() {
        assertEquals(Map.of("potency", 5, "focus", 3, "affinity/fire", 7), TemperRules.tempered(ADEPT_STAFF, 1));
        assertEquals(Map.of("potency", 7, "focus", 5, "affinity/fire", 9), TemperRules.tempered(ADEPT_STAFF, 3));
    }

    @Test
    void anUntemperedPieceIsAsItWas() {
        assertEquals(ADEPT_STAFF, TemperRules.tempered(ADEPT_STAFF, 0));
    }

    @Test
    void noMoreThanTheMostTempersCount() {
        assertEquals(TemperRules.tempered(ADEPT_STAFF, 3), TemperRules.tempered(ADEPT_STAFF, 9));
    }
}
