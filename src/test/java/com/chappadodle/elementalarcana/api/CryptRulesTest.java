package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptRulesTest {

    @Test
    void glyphsHitHarderInHigherLands() {
        assertEquals(4f, CryptRules.glyphDamage(0));
        assertEquals(7f, CryptRules.glyphDamage(12));
        assertEquals(11.5f, CryptRules.glyphDamage(30));
    }

    @Test
    void theStairReachesFloorsSixteenToTwentyEightBlocksDown() {
        assertFalse(CryptRules.stairReaches(80, 65));
        assertTrue(CryptRules.stairReaches(80, 64));
        assertTrue(CryptRules.stairReaches(80, 52));
        assertFalse(CryptRules.stairReaches(80, 51));
    }

    @Test
    void stepsGoDownOneABlockToTheFloor() {
        // The first stair is set in the mausoleum's floor, a block behind its middle; then one down per block.
        assertEquals(80, CryptRules.stepY(80, 60, -1));
        assertEquals(79, CryptRules.stepY(80, 60, 0));
        assertEquals(61, CryptRules.stepY(80, 60, 18));
        assertEquals(60, CryptRules.stepY(80, 60, 19));
        assertEquals(60, CryptRules.stepY(80, 60, 27));
        // The deepest stair is level with the floor by the last block before the entry hall's back wall.
        assertEquals(52, CryptRules.stepY(80, 52, CryptRules.STAIR_RUN - 1));
    }

    @Test
    void kinCrypts() {
        assertEquals(Element.FIRE, CryptRules.element(Element.FIRE, false));
        assertEquals(Element.RADIANCE, CryptRules.element(Element.FIRE, true));
        assertEquals(Element.LIGHTNING, CryptRules.element(Element.WIND, true));
        assertEquals(Element.CRYSTAL, CryptRules.element(Element.EARTH, true));
        assertEquals(Element.ICE, CryptRules.element(Element.WATER, true));
        assertEquals(Element.ICE, CryptRules.element(Element.ICE, true));
    }

    @Test
    void oneCoffinInFourHoldsAMagus() {
        assertEquals(AttunementRank.MAGUS, CryptRules.coffinRank(0.1f));
        assertEquals(AttunementRank.ADEPT, CryptRules.coffinRank(0.25f));
        assertEquals(AttunementRank.ADEPT, CryptRules.coffinRank(0.9f));
    }
}
