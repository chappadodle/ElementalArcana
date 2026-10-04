package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamiliarRulesTest {

    @Test
    void aWispMustBeWornDownToBeBound() {
        assertFalse(FamiliarRules.canBind(20f, 20f));
        assertFalse(FamiliarRules.canBind(5.1f, 20f));
        assertTrue(FamiliarRules.canBind(5f, 20f));
        assertTrue(FamiliarRules.canBind(1f, 20f));
    }

    @Test
    void itGrowsWithItsOwner() {
        assertEquals(AttunementRank.ADEPT, FamiliarRules.rankFor(1));
        assertEquals(AttunementRank.ADEPT, FamiliarRules.rankFor(19));
        assertEquals(AttunementRank.MAGUS, FamiliarRules.rankFor(20));
        assertEquals(AttunementRank.ARCHMAGE, FamiliarRules.rankFor(40));
    }

    @Test
    void itFollowsBeforeItTeleports() {
        assertTrue(FamiliarRules.FOLLOW_STOP < FamiliarRules.FOLLOW_START);
        assertTrue(FamiliarRules.FOLLOW_START < FamiliarRules.TELEPORT);
    }
}
