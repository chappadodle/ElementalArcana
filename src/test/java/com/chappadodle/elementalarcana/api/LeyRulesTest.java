package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeyRulesTest {

    @Test
    void longerJourneysCostMoreUpToACap() {
        assertEquals(10, LeyRules.manaCost(50));
        assertEquals(22, LeyRules.manaCost(1240));
        assertEquals(50, LeyRules.manaCost(100_000));
    }

    @Test
    void compassPoints() {
        assertEquals("north", LeyRules.direction(0, -100));
        assertEquals("east", LeyRules.direction(100, 0));
        assertEquals("south", LeyRules.direction(0, 100));
        assertEquals("west", LeyRules.direction(-100, 0));
        assertEquals("north_east", LeyRules.direction(100, -100));
        assertEquals("south_west", LeyRules.direction(-80, 90));
    }
}
