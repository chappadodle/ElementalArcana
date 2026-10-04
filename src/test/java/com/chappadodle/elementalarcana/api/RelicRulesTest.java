package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelicRulesTest {

    @Test
    void everyElementHasItsOwnRelic() {
        Set<Relic> seen = new HashSet<>();
        for (Element element : Element.values()) {
            Relic relic = Relic.of(element);
            assertEquals(element, relic.element());
            assertTrue(seen.add(relic));
        }
        assertEquals(Relic.values().length - 1, seen.size());
        assertNull(Relic.REVENANTS_PHYLACTERY.element());
    }

    @Test
    void theIdolWantsAHardHit() {
        assertFalse(RelicRules.hardHit(3.9f));
        assertTrue(RelicRules.hardHit(4f));
    }

    @Test
    void cooldownsCountDownInWholeSeconds() {
        assertTrue(RelicRules.ready(0, 100));
        assertFalse(RelicRules.ready(12100, 100));
        assertEquals(600, RelicRules.secondsLeft(12100, 100));
        assertEquals(1, RelicRules.secondsLeft(101, 100));
        assertEquals(0, RelicRules.secondsLeft(100, 100));
        assertEquals(RelicRules.PHYLACTERY_COOLDOWN_TICKS / 20, RelicRules.secondsLeft(RelicRules.PHYLACTERY_COOLDOWN_TICKS, 0));
    }

    @Test
    void thePearlMendsATenth() {
        assertEquals(1f, RelicRules.mend(10f), 1e-6);
        assertEquals(0f, RelicRules.mend(-3f), 1e-6);
    }
}
