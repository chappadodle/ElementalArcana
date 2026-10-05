package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CantripRulesTest {
    @Test
    void mendGivesBackAFifthButAtLeastTwentyFive() {
        // A diamond pickaxe (1561), a wooden one (59), an elytra (432).
        assertEquals(312, CantripRules.mendAmount(1561));
        assertEquals(25, CantripRules.mendAmount(59));
        assertEquals(86, CantripRules.mendAmount(432));
    }

    @Test
    void recallBreaksOnAStepOrAHurt() {
        assertFalse(CantripRules.recallBroken(0.2, false));
        assertTrue(CantripRules.recallBroken(0.3, false));
        assertTrue(CantripRules.recallBroken(0, true));
    }

    @Test
    void prospectReachesTwelveEachWay() {
        assertTrue(CantripRules.inProspect(12, -12, 0));
        assertFalse(CantripRules.inProspect(13, 0, 0));
        assertFalse(CantripRules.inProspect(0, 0, -13));
    }
}
