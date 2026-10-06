package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeekerRulesTest {

    @Test
    void kindsGoRoundInOrder() {
        String kind = SeekerRules.KINDS.get(0);
        for (int i = 1; i <= SeekerRules.KINDS.size(); i++) {
            kind = SeekerRules.next(kind);
            assertEquals(SeekerRules.KINDS.get(i % SeekerRules.KINDS.size()), kind);
        }
        assertEquals("hollowed_camps", SeekerRules.next("sky_isles"));
        assertEquals("shrines", SeekerRules.next("wisp_rings"));
        assertEquals("shrines", SeekerRules.next("nonsense"));
        assertEquals("shrines", SeekerRules.known("nonsense"));
        assertEquals("crypts", SeekerRules.known("crypts"));
    }

    @Test
    void arrivalIsOnTheGround() {
        assertTrue(SeekerRules.arrived(20, 20));
        assertTrue(SeekerRules.arrived(0, -32));
        assertFalse(SeekerRules.arrived(30, 30));
        assertFalse(SeekerRules.arrived(-40, 0));
    }

    @Test
    void distancesAreRough() {
        assertEquals(640, SeekerRules.roughDistance(0, 641));
        assertEquals(500, SeekerRules.roughDistance(300, 400));
        assertEquals(0, SeekerRules.roughDistance(2, 2));
    }
}
