package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BountyRulesTest {

    @Test
    void harderTasksAtHigherLevels() {
        assertEquals(BountyRules.Task.ATTUNED, BountyRules.taskFor(1));
        assertEquals(BountyRules.Task.WISPS, BountyRules.taskFor(2));
        assertEquals(BountyRules.Task.MAGUS, BountyRules.taskFor(3));
        assertEquals(BountyRules.Task.RIFT, BountyRules.taskFor(4));
        assertEquals(BountyRules.Task.ARCHMAGE, BountyRules.taskFor(5));
    }

    @Test
    void countsAndRewardsStayInRange() {
        assertEquals(5, BountyRules.count(BountyRules.Task.ATTUNED, () -> 0.0));
        assertEquals(8, BountyRules.count(BountyRules.Task.ATTUNED, () -> 0.999));
        assertEquals(3, BountyRules.count(BountyRules.Task.WISPS, () -> 0.0));
        assertEquals(5, BountyRules.count(BountyRules.Task.WISPS, () -> 0.999));
        assertEquals(1, BountyRules.count(BountyRules.Task.RIFT, () -> 0.5));
        assertEquals(8, BountyRules.emeralds(BountyRules.Task.ATTUNED, () -> 0.0));
        assertEquals(12, BountyRules.emeralds(BountyRules.Task.WISPS, () -> 0.999));
        assertEquals(32, BountyRules.emeralds(BountyRules.Task.ARCHMAGE, () -> 0.5));
        assertTrue(BountyRules.essence(BountyRules.Task.ARCHMAGE) > BountyRules.essence(BountyRules.Task.ATTUNED));
    }

    @Test
    void tasksRoundTripByIdAndOnlyOneNamesAnElement() {
        for (BountyRules.Task task : BountyRules.Task.values()) {
            assertEquals(task, BountyRules.Task.byId(task.id()));
        }
        assertTrue(BountyRules.Task.ATTUNED.hasElement());
        assertFalse(BountyRules.Task.RIFT.hasElement());
    }
}
