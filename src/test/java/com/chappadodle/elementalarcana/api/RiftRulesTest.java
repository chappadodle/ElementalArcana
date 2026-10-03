package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiftRulesTest {

    @Test
    void nightsAndStormsBringMoreRifts() {
        assertEquals(0.004, RiftRules.chance(false, false), 1e-9);
        assertEquals(0.015, RiftRules.chance(true, false), 1e-9);
        assertEquals(0.03, RiftRules.chance(true, true), 1e-9);
        // About one night in four: 20 rolls a night.
        double perNight = 1 - Math.pow(1 - RiftRules.chance(true, false), 20);
        assertTrue(perNight > 0.2 && perNight < 0.3, "per night: " + perNight);
    }

    @Test
    void theWavesGrow() {
        assertEquals(3, RiftRules.wave(1, AttunementRank.ARCHMAGE).size());
        assertEquals(AttunementRank.MAGUS, RiftRules.wave(2, AttunementRank.ARCHMAGE).get(3));
        assertEquals(AttunementRank.ARCHMAGE, RiftRules.wave(3, AttunementRank.ARCHMAGE).get(4));
        assertEquals(1, RiftRules.wisps(1));
        assertEquals(0, RiftRules.wisps(RiftRules.WAVES));
    }

    @Test
    void youngMagesFaceAMagusWarden() {
        assertEquals(AttunementRank.MAGUS, RiftRules.wardenRank(1));
        assertEquals(AttunementRank.MAGUS, RiftRules.wardenRank(14));
        assertEquals(AttunementRank.ARCHMAGE, RiftRules.wardenRank(15));
    }

    @Test
    void theCacheHoldsFourToEightEssence() {
        assertEquals(4, RiftRules.essence(() -> 0.0));
        assertEquals(8, RiftRules.essence(() -> 0.999));
    }
}
