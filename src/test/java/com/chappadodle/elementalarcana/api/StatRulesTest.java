package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatRulesTest {

    @Test
    void curveSoftCapsAtTwentyAndForty() {
        assertEquals(1.0, StatRules.curve(0), 1e-9);
        assertEquals(Math.pow(1.04, 20), StatRules.curve(20), 1e-9);
        assertEquals(Math.pow(1.04, 20) * Math.pow(1.02, 20), StatRules.curve(40), 1e-9);
        assertEquals(5.92, StatRules.curve(100), 0.01);
    }

    @Test
    void manaAndRegenGrowWithLevelAndReservoir() {
        assertEquals(100f, StatRules.maxMana(1, 0), 1e-4f);
        assertEquals(245f, StatRules.maxMana(30, 0), 1e-4f);
        assertEquals(245f * (float) Math.pow(1.04, 10), StatRules.maxMana(30, 10), 1e-2f);
        assertEquals(2.5f, StatRules.regenPerSecond(1, 0), 1e-4f);
        assertEquals(5.4f, StatRules.regenPerSecond(30, 0), 1e-4f);
    }

    @Test
    void halfStrengthStatsUseTheSquareRoot() {
        assertEquals(Math.pow(1.04, 10), StatRules.spellPower(10, 10), 1e-5);
        assertEquals(Math.pow(1.04, 5), StatRules.spellPower(10, 0), 1e-5);
        assertEquals(1 / Math.pow(1.04, 5), StatRules.wardFactor(10), 1e-5);
        assertEquals(Math.pow(1.04, 5), StatRules.healthMultiplier(10), 1e-5);
        assertEquals(Math.pow(1.04, 5), StatRules.insightFactor(10), 1e-5);
    }

    @Test
    void focusShortensCooldowns() {
        assertEquals(1f, StatRules.cooldownFactor(0), 1e-6f);
        assertEquals(1 / Math.pow(Math.pow(1.04, 20), 0.75), StatRules.cooldownFactor(20), 1e-5);
    }

    @Test
    void creaturesSplitTheirPointsInThree() {
        assertEquals(0, StatRules.creaturePoints(1));
        assertEquals(1, StatRules.creaturePoints(4));
        assertEquals(13, StatRules.creaturePoints(40));
        assertEquals(33, StatRules.creaturePoints(100));
    }
}
