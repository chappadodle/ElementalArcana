package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaleDashRulesTest {

    @ParameterizedTest
    @CsvSource({"1, 1", "2, 2", "6, 2", "7, 3", "10, 3"})
    void secondWindAndTripleChargeAddCharges(int level, int charges) {
        assertEquals(charges, GaleDashRules.charges(level));
    }

    @Test
    void powerSpeedsTheDashALittle() {
        assertEquals(1.5, GaleDashRules.speed(1f), 1e-9);
        assertEquals(1.5 * 1.3, GaleDashRules.speed(4f), 1e-9);
        assertEquals(1.5 * 0.8, GaleDashRules.speed(0.25f), 1e-9);
    }

    @Test
    void tailwindCutsTheCooldown() {
        assertEquals(1f, GaleDashRules.cooldownFactor(5), 1e-6f);
        assertEquals(0.75f, GaleDashRules.cooldownFactor(6), 1e-6f);
    }

    @Test
    void tiersComeInOrder() {
        assertFalse(GaleDashRules.airStep(2));
        assertTrue(GaleDashRules.airStep(3));
        assertTrue(GaleDashRules.slipstream(4));
        assertFalse(GaleDashRules.featherfall(7));
        assertTrue(GaleDashRules.featherfall(8));
        assertTrue(GaleDashRules.swirls(9));
    }
}
