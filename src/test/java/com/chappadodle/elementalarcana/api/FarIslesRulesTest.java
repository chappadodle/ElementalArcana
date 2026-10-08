package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarIslesRulesTest {
    @Test
    void aLensTurnsThroughTheFourConstellations() {
        assertEquals(1, FarIslesRules.next(0));
        assertEquals(0, FarIslesRules.next(3));
    }

    @Test
    void aChartHoldsEachConstellationOnceAndComesFromItsSeed() {
        Set<String> charts = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            int[] chart = FarIslesRules.chart(seed);
            int[] sorted = chart.clone();
            Arrays.sort(sorted);
            assertArrayEquals(new int[]{0, 1, 2, 3}, sorted);
            assertArrayEquals(chart, FarIslesRules.chart(seed));
            charts.add(Arrays.toString(chart));
        }
        // Every order turns up (there are 24).
        assertEquals(24, charts.size());
    }

    @Test
    void theLensesMustShowTheChartSideForSide() {
        int[] chart = {2, 0, 3, 1};
        assertTrue(FarIslesRules.matches(new int[]{2, 0, 3, 1}, chart));
        assertFalse(FarIslesRules.matches(new int[]{2, 0, 1, 3}, chart));
        assertFalse(FarIslesRules.matches(new int[]{2, 0, 3, -1}, chart));
        assertFalse(FarIslesRules.matches(new int[]{2, 0, 3}, chart));
    }

    @Test
    void observatoriesStandOnTheOuterIslands() {
        assertFalse(FarIslesRules.farEnough(0, 0));
        assertFalse(FarIslesRules.farEnough(700, 700));
        assertTrue(FarIslesRules.farEnough(1000, 0));
        assertTrue(FarIslesRules.farEnough(-800, 800));
    }

    @Test
    void theCharmRestsFiveMinutes() {
        assertTrue(FarIslesRules.voidwalkReady(100, -1));
        assertFalse(FarIslesRules.voidwalkReady(5999, 0));
        assertTrue(FarIslesRules.voidwalkReady(6000, 0));
    }

    @Test
    void directionsAreCompassWords() {
        assertEquals("north", FarIslesRules.direction(0, -10));
        assertEquals("east", FarIslesRules.direction(10, 0));
        assertEquals("south-west", FarIslesRules.direction(-10, 10));
        assertEquals("north-west", FarIslesRules.direction(-10, -10));
    }
}
