package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DrakeRidingRulesTest {

    @Test
    void drakesGrowUpOverThreeDays() {
        assertEquals(0, DrakeRidingRules.stage(0));
        assertEquals(0, DrakeRidingRules.stage(23999));
        assertEquals(1, DrakeRidingRules.stage(24000));
        assertEquals(2, DrakeRidingRules.stage(72000));
        assertEquals(0.25f, DrakeRidingRules.scale(0));
        assertEquals(1f, DrakeRidingRules.scale(2));
        assertEquals(140, DrakeRidingRules.health(2));
    }

    @Test
    void eggsWarmAndCrack() {
        assertEquals(0, DrakeRidingRules.warmth(false, true));
        assertEquals(1, DrakeRidingRules.warmth(true, false));
        assertEquals(2, DrakeRidingRules.warmth(true, true));
        assertEquals(0, DrakeRidingRules.cracks(0));
        assertEquals(1, DrakeRidingRules.cracks(DrakeRidingRules.HATCH_TICKS / 3));
        assertEquals(2, DrakeRidingRules.cracks(DrakeRidingRules.HATCH_TICKS * 2 / 3));
        assertEquals(2, DrakeRidingRules.cracks(DrakeRidingRules.HATCH_TICKS));
    }

    @Test
    void flyingSpendsStaminaAndLandingRestoresIt() {
        assertEquals(599, DrakeRidingRules.stamina(600, true, false));
        assertEquals(0, DrakeRidingRules.stamina(0, true, false));
        assertEquals(103, DrakeRidingRules.stamina(100, false, true));
        assertEquals(600, DrakeRidingRules.stamina(599, false, true));
        // Falling without flying: neither.
        assertEquals(100, DrakeRidingRules.stamina(100, false, false));
    }
}
