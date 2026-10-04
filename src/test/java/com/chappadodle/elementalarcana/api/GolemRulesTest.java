package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GolemRulesTest {

    @Test
    void nightsBringMoreGolems() {
        assertTrue(GolemRules.chance(true) > GolemRules.chance(false));
    }

    @Test
    void theSlamIsWarnedOfAndReachesFurtherThanItHits() {
        assertTrue(GolemRules.WINDUP_TICKS >= 10, "players need time to step away");
        assertTrue(GolemRules.SLAM_COOLDOWN_TICKS > GolemRules.WINDUP_TICKS);
        assertTrue(GolemRules.SLAM_AHEAD + GolemRules.SLAM_RADIUS >= GolemRules.REACH);
    }

    @Test
    void theSwingRisesThenLands() {
        assertEquals(0f, GolemRules.swing(-1), 1e-6f);
        assertEquals(0.5f, GolemRules.swing(GolemRules.WINDUP_TICKS / 2f), 1e-6f);
        assertEquals(1f, GolemRules.swing(GolemRules.WINDUP_TICKS + 5), 1e-6f);
    }
}
