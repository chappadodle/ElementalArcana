package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static com.chappadodle.elementalarcana.api.AttunementRank.ADEPT;
import static com.chappadodle.elementalarcana.api.AttunementRank.ARCHMAGE;
import static com.chappadodle.elementalarcana.api.AttunementRank.MAGUS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AttunementRankTest {

    @Test
    void ranksAreBonusLevelsWithMoreMana() {
        assertEquals(0, ADEPT.bonusLevels());
        assertEquals(8, MAGUS.bonusLevels());
        assertEquals(20, ARCHMAGE.bonusLevels());
        assertEquals(3.0, ADEPT.xpMultiplier(), 1e-9);
        assertEquals(5.0, MAGUS.xpMultiplier(), 1e-9);
        assertEquals(12.0, ARCHMAGE.xpMultiplier(), 1e-9);
    }
}
