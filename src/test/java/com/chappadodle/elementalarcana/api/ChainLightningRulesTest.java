package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChainLightningRulesTest {

    @Test
    void levelOneIsTheOldSpell() {
        assertEquals(20, ChainLightningRules.range(1));
        assertEquals(6, ChainLightningRules.jumpRange(1));
        assertEquals(3, ChainLightningRules.jumps(1, null));
        assertEquals(1.5f, ChainLightningRules.conducted(1), 1e-6f);
        assertEquals(2, ChainLightningRules.wetJumps(1));
        assertEquals(7, ChainLightningRules.maxStrikes(1, null));
        assertEquals(0, ChainLightningRules.stunTicks(1));
        assertEquals(5f, ChainLightningRules.strike(1, 0), 1e-5f);
        assertEquals(4f, ChainLightningRules.strike(1, 1), 1e-5f);
        assertEquals(3.2f, ChainLightningRules.strike(1, 2), 1e-5f);
    }

    @ParameterizedTest
    @CsvSource({"1, 3", "3, 3", "4, 4", "7, 4", "8, 5", "10, 5"})
    void forkedChainAndLiveWireAddJumps(int level, int jumps) {
        assertEquals(jumps, ChainLightningRules.jumps(level, null));
    }

    @Test
    void theFalloffShrinksThenGoes() {
        assertEquals(4.5f, ChainLightningRules.strike(4, 1), 1e-5f);
        assertEquals(4.05f, ChainLightningRules.strike(4, 2), 1e-5f);
        assertEquals(5f, ChainLightningRules.strike(9, 4), 1e-5f);
    }

    @Test
    void longArcReachesFurther() {
        assertEquals(26, ChainLightningRules.range(2));
        assertEquals(8, ChainLightningRules.jumpRange(2));
    }

    @Test
    void conductorDoublesWetStrikes() {
        assertEquals(2f, ChainLightningRules.conducted(6), 1e-6f);
        assertEquals(3, ChainLightningRules.wetJumps(6));
        assertEquals(9, ChainLightningRules.maxStrikes(6, null));
    }

    @Test
    void theForks() {
        assertEquals(2, ChainLightningRules.fanOut(ChainLightningRules.STORM_FORK));
        assertEquals(1, ChainLightningRules.fanOut(ChainLightningRules.OVERLOAD));
        assertEquals(ChainLightningRules.FORK_STRIKES, ChainLightningRules.maxStrikes(5, ChainLightningRules.STORM_FORK));
        assertEquals(2, ChainLightningRules.jumps(9, ChainLightningRules.OVERLOAD));
    }

    @Test
    void tiersComeInOrder() {
        assertTrue(ChainLightningRules.stunTicks(3) > 0);
        assertFalse(ChainLightningRules.skyBolt(6));
        assertTrue(ChainLightningRules.skyBolt(7));
        assertFalse(ChainLightningRules.liveWire(7));
        assertTrue(ChainLightningRules.liveWire(8));
    }
}
