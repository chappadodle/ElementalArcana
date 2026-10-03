package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpellChargesTest {
    private static final int COOLDOWN = 60;

    @Test
    void oneChargeIsAnOrdinaryCooldown() {
        assertEquals(1, SpellCharges.ready(1, 0, COOLDOWN));
        long clock = SpellCharges.afterUse(0, COOLDOWN);
        assertEquals(60, clock);
        assertEquals(0, SpellCharges.ready(1, clock, COOLDOWN));
        assertEquals(60, SpellCharges.untilReady(1, clock, COOLDOWN));
        assertEquals(1, SpellCharges.ready(1, 0, COOLDOWN));
    }

    @Test
    void chargesComeBackOneCooldownApart() {
        // Three charges, all used at once: the clock reads three cooldowns.
        long clock = 0;
        for (int i = 0; i < 3; i++) {
            assertEquals(3 - i, SpellCharges.ready(3, clock, COOLDOWN));
            clock = SpellCharges.afterUse(clock, COOLDOWN);
        }
        assertEquals(180, clock);
        assertEquals(0, SpellCharges.ready(3, clock, COOLDOWN));
        assertEquals(60, SpellCharges.untilReady(3, clock, COOLDOWN));
        // A cooldown later, one is back; two later, two.
        assertEquals(1, SpellCharges.ready(3, clock - 60, COOLDOWN));
        assertEquals(2, SpellCharges.ready(3, clock - 120, COOLDOWN));
        assertEquals(3, SpellCharges.ready(3, clock - 180, COOLDOWN));
    }

    @Test
    void theNextChargeIsWhatsLeftOfTheCurrentCooldown() {
        assertEquals(0, SpellCharges.untilNext(0, COOLDOWN));
        assertEquals(60, SpellCharges.untilNext(120, COOLDOWN));
        assertEquals(1, SpellCharges.untilNext(61, COOLDOWN));
        assertEquals(30, SpellCharges.untilNext(90, COOLDOWN));
    }

    @Test
    void aUseWhileRechargingQueuesBehindTheClock() {
        // Two charges, one used 20 ticks ago (40 left): using the other puts the clock at 100.
        assertEquals(1, SpellCharges.ready(2, 40, COOLDOWN));
        assertEquals(100, SpellCharges.afterUse(40, COOLDOWN));
        assertEquals(0, SpellCharges.ready(2, 100, COOLDOWN));
        assertEquals(40, SpellCharges.untilReady(2, 100, COOLDOWN));
    }
}
