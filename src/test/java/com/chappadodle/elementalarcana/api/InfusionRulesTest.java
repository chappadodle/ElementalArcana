package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InfusionRulesTest {
    @Test
    void eachPieceOfTheFamilyWardsATenth() {
        assertEquals(1f, InfusionRules.wardFactor(List.of(), Element.FIRE), 1e-6);
        assertEquals(0.9f, InfusionRules.wardFactor(List.of(Element.FIRE, Element.WATER), Element.FIRE), 1e-6);
        // Radiance is of fire's family.
        assertEquals(0.8f, InfusionRules.wardFactor(List.of(Element.FIRE, Element.RADIANCE), Element.FIRE), 1e-6);
        assertEquals(0.6f, InfusionRules.wardFactor(List.of(Element.ICE, Element.ICE, Element.ICE, Element.WATER), Element.WATER), 1e-6);
        assertEquals(1f, InfusionRules.wardFactor(Arrays.asList(null, Element.EARTH), Element.WIND), 1e-6);
    }

    @Test
    void aSetIsFourOfOneElement() {
        assertEquals(Element.WIND, InfusionRules.setElement(List.of(Element.WIND, Element.WIND, Element.WIND, Element.WIND)));
        assertNull(InfusionRules.setElement(List.of(Element.WIND, Element.WIND, Element.WIND)));
        assertNull(InfusionRules.setElement(List.of(Element.WIND, Element.WIND, Element.WIND, Element.LIGHTNING)));
        assertNull(InfusionRules.setElement(Arrays.asList(null, null, null, null)));
    }

    @Test
    void theBasinTakesOneElementUpToEight() {
        assertTrue(InfusionRules.canPour(null, 0, Element.FIRE));
        assertTrue(InfusionRules.canPour(Element.FIRE, 3, Element.FIRE));
        assertFalse(InfusionRules.canPour(Element.FIRE, 3, Element.ICE));
        assertTrue(InfusionRules.canPour(Element.FIRE, 0, Element.ICE));
        assertFalse(InfusionRules.canPour(Element.FIRE, InfusionRules.ESSENCE_NEEDED, Element.FIRE));
    }
}
