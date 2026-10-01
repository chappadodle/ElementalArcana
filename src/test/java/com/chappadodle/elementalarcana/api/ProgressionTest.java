package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static com.chappadodle.elementalarcana.api.Progression.condenseCost;
import static com.chappadodle.elementalarcana.api.Progression.cooldownTicks;
import static com.chappadodle.elementalarcana.api.Progression.essenceBarFraction;
import static com.chappadodle.elementalarcana.api.Progression.essenceMastery;
import static com.chappadodle.elementalarcana.api.Progression.essenceToFill;
import static com.chappadodle.elementalarcana.api.Progression.creatureXp;
import static com.chappadodle.elementalarcana.api.Progression.damageLevelFactor;
import static com.chappadodle.elementalarcana.api.Progression.killXp;
import static com.chappadodle.elementalarcana.api.Progression.sizeFactor;
import static com.chappadodle.elementalarcana.api.Progression.xpGapFactor;
import static com.chappadodle.elementalarcana.api.Progression.xpToNextLevel;
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
    void xpCurveGrowsNinePercentPerLevel() {
        assertEquals(55, xpToNextLevel(1));
        assertEquals(669, xpToNextLevel(30));   // 55 x 1.09^29 = 669.5
        assertEquals(255_953, xpToNextLevel(99));
        assertEquals(0, xpToNextLevel(100));
    }

    @Test
    void sameLevelKillsPerLevelRiseFromEightToForty() {
        assertEquals(7, creatureXp(1));          // 55 / 8 = 6.9
        assertEquals(6399, creatureXp(100));     // 255,953 / 40
    }

    @Test
    void lowerCreaturesPayLessAndHigherOnesMore() {
        assertEquals(1.0, xpGapFactor(10, 10), 1e-9);
        assertEquals(0.5, xpGapFactor(10, 5), 1e-9);
        assertEquals(0.0, xpGapFactor(20, 10), 1e-9);
        assertEquals(0.0, xpGapFactor(30, 10), 1e-9);
        assertEquals(1.25, xpGapFactor(10, 15), 1e-9);
        assertEquals(1.5, xpGapFactor(10, 40), 1e-9);
    }

    @Test
    void killXpScalesBySizeAndRank() {
        assertEquals(7, killXp(1, 1, 1.0, 1.0));
        assertEquals(1, killXp(1, 1, 0.2, 1.0));     // a chicken: 1.4
        assertEquals(84, killXp(1, 1, 1.0, 12.0));
        assertEquals(0, killXp(20, 1, 5.0, 12.0));
        assertEquals(0.1, sizeFactor(1), 1e-9);
        assertEquals(1.0, sizeFactor(20), 1e-9);
        assertEquals(5.0, sizeFactor(300), 1e-9);
    }

    @Test
    void levelGapScalesDamageBothWays() {
        assertEquals(1f, damageLevelFactor(10, 10), 1e-6f);
        assertEquals((float) Math.pow(1.045, 20), damageLevelFactor(30, 10), 1e-4f);
        assertEquals((float) Math.pow(1.045, -20), damageLevelFactor(10, 30), 1e-6f);
        assertEquals(damageLevelFactor(41, 1), damageLevelFactor(100, 1), 1e-6f);
    }

    @Test
    void focusFactorShortensCooldowns() {
        assertEquals(60, cooldownTicks(60, 1, 1f));
        assertEquals(30, cooldownTicks(60, 1, 0.5f));
        assertEquals(14, cooldownTicks(60, 10, 0.5f));  // 60 x 0.46 x 0.5 = 13.8
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
