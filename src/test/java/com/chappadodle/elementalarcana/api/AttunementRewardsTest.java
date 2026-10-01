package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static com.chappadodle.elementalarcana.api.AttunementRank.ADEPT;
import static com.chappadodle.elementalarcana.api.AttunementRank.ARCHMAGE;
import static com.chappadodle.elementalarcana.api.AttunementRank.MAGUS;
import static com.chappadodle.elementalarcana.api.AttunementRewards.essenceDrops;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttunementRewardsTest {

    private static DoubleSupplier always(double value) {
        return () -> value;
    }

    @Test
    void plainInnateCreaturesRarelyDropEssence() {
        assertEquals(1, essenceDrops(null, 1.0, always(0.049)));
        assertEquals(0, essenceDrops(null, 1.0, always(0.05)));
    }

    @Test
    void insightRaisesTheChances() {
        assertEquals(1, essenceDrops(null, 2.0, always(0.099)));
        assertEquals(0, essenceDrops(null, 2.0, always(0.1)));
        assertEquals(1, essenceDrops(ADEPT, 2.0, always(0.99)));
    }

    @Test
    void adeptsDropOneHalfTheTime() {
        assertEquals(1, essenceDrops(ADEPT, 1.0, always(0.49)));
        assertEquals(0, essenceDrops(ADEPT, 1.0, always(0.5)));
    }

    @Test
    void magiDropOneOrTwo() {
        assertEquals(2, essenceDrops(MAGUS, 1.0, always(0.49)));
        assertEquals(1, essenceDrops(MAGUS, 1.0, always(0.5)));
    }

    @Test
    void archmagesDropThreeToFive() {
        assertEquals(3, essenceDrops(ARCHMAGE, 1.0, always(0.0)));
        assertEquals(3, essenceDrops(ARCHMAGE, 1.0, always(0.33)));
        assertEquals(4, essenceDrops(ARCHMAGE, 1.0, always(0.34)));
        assertEquals(5, essenceDrops(ARCHMAGE, 1.0, always(0.67)));
        assertEquals(5, essenceDrops(ARCHMAGE, 1.0, always(0.9999)));
    }
}
