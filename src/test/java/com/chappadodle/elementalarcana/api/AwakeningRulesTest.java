package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.chappadodle.elementalarcana.api.AwakeningRules.anyWeights;
import static com.chappadodle.elementalarcana.api.AwakeningRules.brushChance;
import static com.chappadodle.elementalarcana.api.AwakeningRules.catalystBase;
import static com.chappadodle.elementalarcana.api.AwakeningRules.catalystChance;
import static com.chappadodle.elementalarcana.api.AwakeningRules.dailyChance;
import static com.chappadodle.elementalarcana.api.AwakeningRules.day;
import static com.chappadodle.elementalarcana.api.AwakeningRules.milestoneLevel;
import static com.chappadodle.elementalarcana.api.AwakeningRules.pick;
import static com.chappadodle.elementalarcana.api.AwakeningRules.weights;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AwakeningRulesTest {

    @Test
    void daysCountOnTheDayClockAndNeverGoBackwards() {
        assertEquals(0, day(5000, 0));
        assertEquals(3, day(3 * 24000 + 100, 0));
        assertEquals(2, day(5 * 24000, 3 * 24000));
        assertEquals(0, day(1000, 50_000));
    }

    @Test
    void theDailyChanceRampsFromDayTenAndIsCertainByDaySixteen() {
        assertEquals(0.03, dailyChance(0), 1e-9);
        assertEquals(0.03, dailyChance(9), 1e-9);
        assertEquals(0.25, dailyChance(10), 1e-9);
        assertEquals(0.55, dailyChance(12), 1e-9);
        assertEquals(1.0, dailyChance(15), 1e-9);
        assertEquals(1.0, dailyChance(16), 1e-9);
        assertEquals(1.0, dailyChance(40), 1e-9);
    }

    @Test
    void brushesAreOneInThreeAndCertainFromDayTen() {
        assertEquals(1.0 / 3, brushChance(0), 1e-9);
        assertEquals(1.0 / 3, brushChance(9), 1e-9);
        assertEquals(1.0, brushChance(10), 1e-9);
    }

    @Test
    void aDerivedElementIsAHundredthAsLikelyAsItsBase() {
        Map<Element, Double> water = weights(Element.WATER);
        assertEquals(100.0, water.get(Element.WATER), 1e-9);
        assertEquals(1.0, water.get(Element.ICE), 1e-9);
        assertEquals(Element.WATER, pick(water, 0.5));
        assertEquals(Element.WATER, pick(water, 0.989));
        assertEquals(Element.ICE, pick(water, 0.999));
        assertEquals(Map.of(Element.FIRE, 100.0), weights(Element.FIRE));
    }

    @Test
    void theBackgroundRollGivesEveryFamilyTheSameShare() {
        Map<Element, Double> any = anyWeights();
        assertEquals(100.0, any.get(Element.FIRE), 1e-9);
        assertEquals(100.0, any.get(Element.WIND), 1e-9);
        assertEquals(100.0, any.get(Element.EARTH), 1e-9);
        assertEquals(100.0, any.get(Element.WATER) + any.get(Element.ICE), 1e-9);
        assertEquals(any.get(Element.WATER) / 100, any.get(Element.ICE), 1e-9);
    }

    @Test
    void pickingFromNothingGivesNothing() {
        assertNull(pick(Map.of(), 0.3));
    }

    @Test
    void aCatalystIsAboutOnePercentBeforeItsMilestone() {
        assertEquals(10, milestoneLevel(1));
        assertEquals(20, milestoneLevel(2));
        assertEquals(0.25, catalystBase(1), 1e-9);
        assertEquals(0.0625, catalystBase(2), 1e-9);
        assertEquals(0.25 / 16, catalystBase(3), 1e-9);
        assertEquals(0.01, catalystChance(1, 5, 0), 1e-9);
        assertEquals(0.0625 * 0.04, catalystChance(2, 19, 0), 1e-9);
    }

    @Test
    void aCatalystRisesWithLevelAfterItsMilestoneUpToTripleAndFailuresHelp() {
        assertEquals(0.25, catalystChance(1, 10, 0), 1e-9);
        assertEquals(0.35, catalystChance(1, 20, 0), 1e-9);
        assertEquals(0.75, catalystChance(1, 60, 0), 1e-9);
        assertEquals(0.75, catalystChance(1, 100, 0), 1e-9);
        assertEquals(0.325, catalystChance(1, 10, 3), 1e-9);
        assertEquals(1.0, catalystChance(1, 100, 100), 1e-9);
    }
}
