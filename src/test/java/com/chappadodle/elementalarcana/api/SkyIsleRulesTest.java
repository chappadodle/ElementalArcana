package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkyIsleRulesTest {
    @Test
    void islesFloatHighButUnderTheCeiling() {
        assertEquals(SkyIsleRules.MIN_Y, SkyIsleRules.topY(64, 0, 320));
        assertEquals(150 + 70 + 15, SkyIsleRules.topY(150, 0.5, 320));
        assertEquals(320 - SkyIsleRules.TOP_MARGIN, SkyIsleRules.topY(260, 0.9, 320));
    }

    @Test
    void theUndersideTapersToAPoint() {
        assertEquals(6, SkyIsleRules.radiusAt(6, 0, 12), 1e-9);
        assertEquals(0, SkyIsleRules.radiusAt(6, 12, 12), 1e-9);
        double last = 7;
        for (int below = 0; below < 12; below++) {
            double radius = SkyIsleRules.radiusAt(6, below, 12);
            assertTrue(radius <= last);
            last = radius;
        }
        // Still most of its width halfway down.
        assertTrue(SkyIsleRules.radiusAt(6, 6, 12) > 3.5);
    }
}
