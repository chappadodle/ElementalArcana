package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static com.chappadodle.elementalarcana.api.Progression.cooldownTicks;
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
}
