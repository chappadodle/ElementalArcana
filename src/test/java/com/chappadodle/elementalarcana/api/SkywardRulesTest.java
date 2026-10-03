package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkywardRulesTest {

    @Test
    void higherGroundLeapsHigherAndLastsLonger() {
        assertTrue(SkywardRules.leapSpeed(2) > SkywardRules.leapSpeed(1));
        assertEquals(160, SkywardRules.skywardTicks(1, null));
        assertEquals(240, SkywardRules.skywardTicks(2, null));
        assertTrue(SkywardRules.sinkRate(2) < SkywardRules.sinkRate(1));
    }

    @ParameterizedTest
    @CsvSource({"1", "2", "7", "10"})
    void aGliderAlwaysSinksFastEnoughNotToFloat(int level) {
        assertTrue(SkywardRules.sinkRate(level) > SkywardRules.FLOATING);
    }

    @Test
    void liftWidensTheGust() {
        assertEquals(4.0, SkywardRules.gustReach(5), 1e-9);
        assertEquals(6.0, SkywardRules.gustReach(6), 1e-9);
        assertTrue(SkywardRules.gustLift(6) > SkywardRules.gustLift(5));
    }

    @Test
    void tailwindGlideIsFasterAndLandsSafely() {
        assertTrue(SkywardRules.glideSpeed(7) > SkywardRules.glideSpeed(6));
        assertFalse(SkywardRules.safeLandings(6));
        assertTrue(SkywardRules.safeLandings(7));
    }

    @Test
    void aerialBarrageCutsWindCosts() {
        assertEquals(1f, SkywardRules.barrageCost(7), 1e-6f);
        assertEquals(0.7f, SkywardRules.barrageCost(8), 1e-6f);
    }

    @Test
    void endlessSkyLastsUntilLanding() {
        assertEquals(SkywardRules.ENDLESS_TICKS, SkywardRules.skywardTicks(10, SkywardRules.ENDLESS_SKY));
        assertEquals(240, SkywardRules.skywardTicks(10, SkywardRules.HEAVENS_DESCENT));
    }

    @ParameterizedTest
    @CsvSource({"0, 2.5, 3.0", "10, 4.0, 6.5", "20, 5.5, 10.0", "30, 5.5, 10.0"})
    void thePlungeGrowsWithTheFallUpToTwentyBlocks(double height, double radius, float damage) {
        assertEquals(radius, SkywardRules.plungeRadius(height, null, null), 1e-9);
        assertEquals(damage, SkywardRules.plungeDamage(height, null), 1e-5f);
    }

    @Test
    void skyfallAndHeavensDescentWidenThePlunge() {
        assertEquals(8.25, SkywardRules.plungeRadius(20, SkywardRules.SKYFALL, null), 1e-9);
        assertEquals(15.0, SkywardRules.plungeDamage(20, SkywardRules.SKYFALL), 1e-5f);
        assertEquals(11.0, SkywardRules.plungeRadius(20, SkywardRules.WIND_RIDER, SkywardRules.HEAVENS_DESCENT), 1e-9);
        assertEquals(16.5, SkywardRules.plungeRadius(20, SkywardRules.SKYFALL, SkywardRules.HEAVENS_DESCENT), 1e-9);
    }
}
