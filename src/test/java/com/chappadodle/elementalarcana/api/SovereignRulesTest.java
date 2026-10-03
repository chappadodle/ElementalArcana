package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SovereignRulesTest {

    @ParameterizedTest
    @CsvSource({"1, 30", "16, 30", "30, 30", "31, 31", "45, 45"})
    void baseLevelIsTheZonesButNeverUnderThirty(int zone, int expected) {
        assertEquals(expected, SovereignRules.baseLevel(zone));
    }

    @Test
    void theWeakestSovereignIsLevelFifty() {
        assertEquals(50, SovereignRules.baseLevel(1) + AttunementRank.ARCHMAGE.bonusLevels());
    }

    @Test
    void secondPhaseFromHalfHealth() {
        assertFalse(SovereignRules.secondPhase(321, 640));
        assertTrue(SovereignRules.secondPhase(320, 640));
        assertTrue(SovereignRules.secondPhase(1, 640));
        assertFalse(SovereignRules.secondPhase(0, 0));
    }

    @ParameterizedTest
    @CsvSource({"240, 144", "60, 36", "30, 20", "10, 20"})
    void creatureSpellsComeRoundFaster(int creature, int expected) {
        assertEquals(expected, SovereignRules.cooldown(creature));
    }

    @Test
    void oneSovereignPerBaseElement() {
        for (Element element : Element.values()) {
            assertEquals(!element.derived(), SovereignRules.hasSovereign(element), element.name());
        }
    }
}
