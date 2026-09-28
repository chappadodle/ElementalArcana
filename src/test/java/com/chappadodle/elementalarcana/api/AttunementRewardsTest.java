package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.function.DoubleSupplier;

import static com.chappadodle.elementalarcana.api.AttunementRank.ADEPT;
import static com.chappadodle.elementalarcana.api.AttunementRank.ARCHMAGE;
import static com.chappadodle.elementalarcana.api.AttunementRank.MAGUS;
import static com.chappadodle.elementalarcana.api.AttunementRewards.essenceDrops;
import static com.chappadodle.elementalarcana.api.AttunementRewards.magicXp;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttunementRewardsTest {

    private static DoubleSupplier always(double value) {
        return () -> value;
    }

    @Test
    void magicXpByRank() {
        assertEquals(0, magicXp(null));
        assertEquals(20, magicXp(ADEPT));
        assertEquals(60, magicXp(MAGUS));
        assertEquals(250, magicXp(ARCHMAGE));
    }

    @Test
    void plainInnateCreaturesRarelyDropEssence() {
        assertEquals(1, essenceDrops(null, always(0.049)));
        assertEquals(0, essenceDrops(null, always(0.05)));
    }

    @Test
    void adeptsDropOneHalfTheTime() {
        assertEquals(1, essenceDrops(ADEPT, always(0.49)));
        assertEquals(0, essenceDrops(ADEPT, always(0.5)));
    }

    @Test
    void magiDropOneOrTwo() {
        assertEquals(2, essenceDrops(MAGUS, always(0.49)));
        assertEquals(1, essenceDrops(MAGUS, always(0.5)));
    }

    @Test
    void archmagesDropThreeToFive() {
        assertEquals(3, essenceDrops(ARCHMAGE, always(0.0)));
        assertEquals(3, essenceDrops(ARCHMAGE, always(0.33)));
        assertEquals(4, essenceDrops(ARCHMAGE, always(0.34)));
        assertEquals(5, essenceDrops(ARCHMAGE, always(0.67)));
        assertEquals(5, essenceDrops(ARCHMAGE, always(0.9999)));
    }
}
