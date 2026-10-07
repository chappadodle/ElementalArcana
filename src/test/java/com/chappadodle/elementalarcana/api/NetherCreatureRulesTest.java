package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetherCreatureRulesTest {

    @Test
    void packsRunTwoToFour() {
        assertEquals(2, NetherCreatureRules.packSize(0.0));
        assertEquals(3, NetherCreatureRules.packSize(0.5));
        assertEquals(4, NetherCreatureRules.packSize(0.999));
    }

    @Test
    void waterAndIceTearAnAshWraith() {
        assertEquals(10f, NetherCreatureRules.ashWraithDamage(5f, true), 1e-6);
        assertEquals(5f, NetherCreatureRules.ashWraithDamage(5f, false), 1e-6);
    }

    @Test
    void theHoundstoothIsASixthFaster() {
        assertEquals(1.0 / 6.0, NetherCreatureRules.HOUNDSTOOTH_SPEED, 1e-9);
    }

    @Test
    void theWraithsTouchWithersLongerThanItSlows() {
        assertTrue(NetherCreatureRules.WITHER_TICKS > NetherCreatureRules.SLOW_TICKS);
        assertTrue(NetherCreatureRules.ASH_TOUCH_COOLDOWN_TICKS > NetherCreatureRules.WITHER_TICKS);
    }
}
