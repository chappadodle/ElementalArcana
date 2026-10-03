package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManaWeatherRulesTest {

    @ParameterizedTest
    @CsvSource({"1, 1.0", "10, 1.0", "20, 1.1", "35, 1.25", "50, 1.4", "90, 1.4"})
    void richLandsQuickenManaUpToFortyPercent(int zone, float expected) {
        assertEquals(expected, ManaWeatherRules.rich(zone), 1e-5f);
    }

    @Test
    void theWeatherCombines() {
        assertEquals(1f, ManaWeatherRules.regenFactor(5, false, 0, false), 1e-5f);
        assertEquals(0.25f, ManaWeatherRules.regenFactor(40, true, 0, false), 1e-5f);
        assertEquals(1.25f, ManaWeatherRules.regenFactor(5, false, 1, false), 1e-5f);
        assertEquals(0.8f, ManaWeatherRules.regenFactor(5, false, -1, false), 1e-5f);
        assertEquals(1.5f, ManaWeatherRules.regenFactor(5, false, 0, true), 1e-5f);
        // Rich land, home ground and a tide: 1.2 x 1.25 x 1.5.
        assertEquals(2.25f, ManaWeatherRules.regenFactor(30, false, 1, true), 1e-5f);
    }

    @ParameterizedTest
    @CsvSource({"14, 0, 0", "15, 0, 1", "24, 0, 1", "25, 0, 2", "35, 0, 3", "60, 0, 3", "18, 12, 1", "18, 16, 0", "25, 20, 1"})
    void pressureGrowsWithTheGapAndWardPushesBack(int gap, int ward, int expected) {
        assertEquals(expected, ManaWeatherRules.pressureRanks(gap, ward));
    }

    @Test
    void eachRankOfPressureCosts() {
        assertEquals(0.88f, ManaWeatherRules.pressurePower(1), 1e-5f);
        assertEquals(0.64f, ManaWeatherRules.pressurePower(3), 1e-5f);
        assertEquals(0.4f, ManaWeatherRules.pressureRegen(3), 1e-5f);
        assertEquals(1f, ManaWeatherRules.pressurePower(0), 1e-5f);
    }

    @ParameterizedTest
    @CsvSource({"-20, WEAK", "-5, WEAK", "-4, EVEN", "4, EVEN", "5, STRONG", "9, STRONG", "10, DEADLY"})
    void dangerColoursTheGap(int gap, ManaSenseRules.Danger expected) {
        assertEquals(expected, ManaSenseRules.danger(gap));
    }

    @ParameterizedTest
    @CsvSource({"0, DANGER", "4, DANGER", "5, LEVEL", "14, LEVEL", "15, ELEMENT", "60, ELEMENT"})
    void insightReadsMore(int insight, ManaSenseRules.Detail expected) {
        assertEquals(expected, ManaSenseRules.detail(insight));
    }
}
