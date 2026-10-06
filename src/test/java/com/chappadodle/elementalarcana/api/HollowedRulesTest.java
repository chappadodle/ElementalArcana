package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HollowedRulesTest {

    @Test
    void theyEatWhatThereIs() {
        assertEquals(15f, HollowedRules.manaEaten(100f, 15f, false), 1e-6);
        assertEquals(6f, HollowedRules.manaEaten(6f, 15f, false), 1e-6);
        assertEquals(0f, HollowedRules.manaEaten(0f, 15f, false), 1e-6);
        assertEquals(0f, HollowedRules.manaEaten(-3f, 15f, false), 1e-6);
    }

    @Test
    void aHungerwardHalvesIt() {
        assertEquals(7.5f, HollowedRules.manaEaten(100f, 15f, true), 1e-6);
        assertEquals(4f, HollowedRules.manaEaten(4f, 15f, true), 1e-6);
        assertEquals(3f, HollowedRules.damage(4f, true), 1e-6);
        assertEquals(4f, HollowedRules.damage(4f, false), 1e-6);
    }

    @Test
    void withNothingToEatTheyBiteDeeper() {
        assertTrue(HollowedRules.starving(5f, 10f));
        assertFalse(HollowedRules.starving(10f, 10f));
    }

    @Test
    void patrolsComeFromLevelFifteen() {
        assertEquals(0, HollowedRules.patrolChance(14), 1e-9);
        assertEquals(0.08, HollowedRules.patrolChance(15), 1e-9);
        assertEquals(0.11, HollowedRules.patrolChance(25), 1e-9);
        assertEquals(0.2, HollowedRules.patrolChance(60), 1e-9);
        assertEquals(1, HollowedRules.patrolAcolytes(29));
        assertEquals(2, HollowedRules.patrolAcolytes(30));
        assertEquals(1, HollowedRules.patrolDevourers(44));
        assertEquals(2, HollowedRules.patrolDevourers(45));
    }
}
