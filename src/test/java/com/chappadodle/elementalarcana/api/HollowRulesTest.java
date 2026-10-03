package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HollowRulesTest {

    @ParameterizedTest
    @CsvSource({"1000, FIRE", "751, FIRE", "750, WATER", "501, WATER", "500, WIND", "251, WIND", "250, EARTH", "1, EARTH"})
    void itWearsFourFormsAQuarterEach(float health, Element expected) {
        assertEquals(expected, HollowRules.formAt(health, 1000));
    }

    @Test
    void theFormsAreTheFourSovereignsInOrder() {
        assertEquals(SovereignRules.ELEMENTS, HollowRules.FORMS);
    }

    @Test
    void itIsLevelSeventy() {
        assertEquals(70, HollowRules.BASE_LEVEL + AttunementRank.ARCHMAGE.bonusLevels());
    }

    @Test
    void itEatsManaThenHealth() {
        assertEquals(5f, HollowRules.manaEaten(120f));
        assertEquals(3f, HollowRules.manaEaten(3f));
        assertEquals(0f, HollowRules.manaEaten(0f));
        assertFalse(HollowRules.starving(5f));
        assertTrue(HollowRules.starving(4.9f));
    }
}
