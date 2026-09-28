package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static com.chappadodle.elementalarcana.api.Progression.condenseCost;
import static com.chappadodle.elementalarcana.api.Progression.cooldownTicks;
import static com.chappadodle.elementalarcana.api.Progression.essenceBarFraction;
import static com.chappadodle.elementalarcana.api.Progression.essenceMastery;
import static com.chappadodle.elementalarcana.api.Progression.essenceToFill;
import static com.chappadodle.elementalarcana.api.Progression.regenPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProgressionTest {

    @Test
    void cooldownsShrinkSixPercentPerSpellLevel() {
        assertEquals(60, cooldownTicks(60, 1));
        assertEquals(46, cooldownTicks(60, 5));   // 60 x 0.76 = 45.6
        assertEquals(28, cooldownTicks(60, 10));  // 60 x 0.46 = 27.6
        assertEquals(23, cooldownTicks(50, 10));
        assertEquals(18, cooldownTicks(40, 10));
        assertEquals(37, cooldownTicks(80, 10));
        assertEquals(184, cooldownTicks(400, 10));
    }

    @Test
    void levelBelowOneCountsAsOne() {
        assertEquals(60, cooldownTicks(60, 0));
    }

    @Test
    void regenGrowsWithMagicLevel() {
        assertEquals(2.5f, regenPerSecond(1), 1e-6f);
        assertEquals(4.75f, regenPerSecond(10), 1e-6f);
        assertEquals(9.75f, regenPerSecond(30), 1e-6f);
    }

    @Test
    void essenceFillsLessOfTheBarEachLevel() {
        assertEquals(0.5f, essenceBarFraction(1), 1e-6f);
        assertEquals(0.4f, essenceBarFraction(2), 1e-6f);
        assertEquals(0.32f, essenceBarFraction(3), 1e-6f);
        assertEquals(0.2048f, essenceBarFraction(5), 1e-5f);
    }

    @Test
    void essenceMasteryPerEssence() {
        // Bars are 60 x level mastery.
        assertEquals(30, essenceMastery(1, 60));
        assertEquals(58, essenceMastery(3, 180));  // 57.6
        assertEquals(61, essenceMastery(5, 300));  // 61.44
        assertEquals(45, essenceMastery(9, 540));  // 45.3
        assertEquals(1, essenceMastery(9, 1));     // never 0
    }

    @Test
    void essenceNeededToFillTheBar() {
        assertEquals(2, essenceToFill(1, 60, 0));
        assertEquals(1, essenceToFill(1, 60, 30));
        assertEquals(0, essenceToFill(1, 60, 60));
        assertEquals(12, essenceToFill(9, 540, 0));
        int total = 0;
        for (int level = 1; level <= 9; level++) {
            total += essenceToFill(level, 60 * level, 0);
        }
        assertEquals(55, total);
    }

    @Test
    void condensingCostsMoreEachTime() {
        assertEquals(8, condenseCost(0));
        assertEquals(12, condenseCost(1));
        assertEquals(16, condenseCost(2));
        int total = 0;
        for (int i = 0; i < 16; i++) {
            total += condenseCost(i);
        }
        assertEquals(608, total);
    }
}
