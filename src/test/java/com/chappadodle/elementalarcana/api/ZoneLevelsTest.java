package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static com.chappadodle.elementalarcana.api.ZoneLevels.Dimension.END;
import static com.chappadodle.elementalarcana.api.ZoneLevels.Dimension.NETHER;
import static com.chappadodle.elementalarcana.api.ZoneLevels.Dimension.OVERWORLD;
import static com.chappadodle.elementalarcana.api.ZoneLevels.level;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ZoneLevelsTest {

    @Test
    void overworldStartsLowAndGrowsWithDistance() {
        assertEquals(1, level(OVERWORLD, 0, 70, false, false, false, 0));
        assertEquals(5, level(OVERWORLD, 0, 70, false, false, false, 4));
        assertEquals(11, level(OVERWORLD, 1500, 70, false, false, false, 0));
        assertEquals(31, level(OVERWORLD, 100_000, 70, false, false, false, 0));
    }

    @Test
    void cavesStructuresAndDangerousBiomesAddUpToFifteen() {
        assertEquals(1, level(OVERWORLD, 0, 40, false, false, false, 0));   // open sky: no cave bonus
        assertEquals(3, level(OVERWORLD, 0, 40, true, false, false, 0));    // (63 - 40) / 10 = 2
        assertEquals(11, level(OVERWORLD, 0, -60, true, false, false, 0));  // cave bonus capped at 10
        assertEquals(6, level(OVERWORLD, 0, 70, false, true, false, 0));
        assertEquals(16, level(OVERWORLD, 0, -60, true, true, true, 0));    // 10 + 5 + 10 capped at 15
    }

    @Test
    void netherIsTwentyFiveToFifty() {
        assertEquals(25, level(NETHER, 0, 70, true, false, false, 0));
        assertEquals(35, level(NETHER, 500, 70, true, false, false, 0));
        assertEquals(50, level(NETHER, 100_000, 70, true, true, false, 4));
    }

    @Test
    void endIsSixtyAndUp() {
        assertEquals(60, level(END, 0, 70, false, false, false, 0));
        assertEquals(74, level(END, 1000, 70, false, false, false, 4));
        assertEquals(94, level(END, 100_000, 70, false, false, false, 4));
    }
}
