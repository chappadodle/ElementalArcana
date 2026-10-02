package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementTest {

    @Test
    void fireOpposesTheCold() {
        assertTrue(Element.FIRE.opposes(Element.WATER));
        assertTrue(Element.FIRE.opposes(Element.ICE));
        assertTrue(Element.WATER.opposes(Element.FIRE));
        assertTrue(Element.ICE.opposes(Element.FIRE));
    }

    @Test
    void iceBelongsToWatersFamily() {
        assertEquals(Element.WATER, Element.ICE.family());
        assertEquals(Element.WATER, Element.WATER.family());
        assertEquals(Element.FIRE, Element.FIRE.family());
        assertEquals(Element.WIND, Element.WIND.family());
        assertEquals(Element.EARTH, Element.EARTH.family());
    }

    @Test
    void derivedElementsBelongToTheirKin() {
        assertEquals(Element.EARTH, Element.CRYSTAL.family());
        assertEquals(Element.WIND, Element.LIGHTNING.family());
        assertEquals(Element.FIRE, Element.RADIANCE.family());
        for (Element element : Element.values()) {
            boolean derived = element == Element.ICE || element == Element.CRYSTAL || element == Element.LIGHTNING
                    || element == Element.RADIANCE;
            assertEquals(derived, element.derived(), element.name());
            assertFalse(element.family().derived(), element.name());
        }
    }

    @Test
    void radianceTakesFiresSide() {
        assertTrue(Element.RADIANCE.opposes(Element.WATER));
        assertTrue(Element.RADIANCE.opposes(Element.ICE));
        assertTrue(Element.WATER.opposes(Element.RADIANCE));
        assertFalse(Element.RADIANCE.opposes(Element.FIRE));
        assertFalse(Element.CRYSTAL.opposes(Element.LIGHTNING));
    }

    @Test
    void everythingElseIsCompatible() {
        assertFalse(Element.WATER.opposes(Element.ICE));
        assertFalse(Element.ICE.opposes(Element.WATER));
        for (Element element : Element.values()) {
            assertFalse(Element.WIND.opposes(element));
            assertFalse(element.opposes(Element.WIND));
            assertFalse(element.opposes(element));
        }
    }

    // The full chart from the spec: spell element, creature element (NONE = no element), multiplier.
    @ParameterizedTest
    @CsvSource({
            "FIRE, FIRE, 0.5", "FIRE, WATER, 0.5", "FIRE, ICE, 1.5", "FIRE, WIND, 1.0", "FIRE, NONE, 1.0",
            "WATER, FIRE, 1.5", "WATER, WATER, 0.5", "WATER, ICE, 1.0", "WATER, WIND, 1.0", "WATER, NONE, 1.0",
            "ICE, FIRE, 0.5", "ICE, WATER, 1.5", "ICE, ICE, 0.5", "ICE, WIND, 1.0", "ICE, NONE, 1.0",
            "WIND, FIRE, 1.0", "WIND, WATER, 1.0", "WIND, ICE, 1.0", "WIND, WIND, 0.5", "WIND, NONE, 1.0",
            "WATER, EARTH, 1.5", "EARTH, WIND, 1.5", "EARTH, EARTH, 0.5", "EARTH, NONE, 1.0",
            "EARTH, FIRE, 1.0", "EARTH, WATER, 1.0", "EARTH, ICE, 1.0", "FIRE, EARTH, 1.0", "ICE, EARTH, 1.0", "WIND, EARTH, 1.0",
            // The derived elements (docs/superpowers/specs/2026-10-03-derived-elements-design.md).
            "CRYSTAL, WIND, 1.5", "CRYSTAL, LIGHTNING, 1.5", "CRYSTAL, CRYSTAL, 0.5", "CRYSTAL, EARTH, 1.0", "CRYSTAL, WATER, 1.0",
            "CRYSTAL, NONE, 1.0", "EARTH, LIGHTNING, 1.5", "EARTH, CRYSTAL, 1.0", "WATER, CRYSTAL, 1.0",
            "LIGHTNING, WATER, 1.5", "LIGHTNING, EARTH, 0.5", "LIGHTNING, CRYSTAL, 0.5", "LIGHTNING, LIGHTNING, 0.5",
            "LIGHTNING, WIND, 1.0", "LIGHTNING, FIRE, 1.0", "LIGHTNING, ICE, 1.0", "LIGHTNING, NONE, 1.0", "WIND, LIGHTNING, 1.0",
            "WATER, LIGHTNING, 1.0",
            "RADIANCE, ICE, 1.5", "RADIANCE, WATER, 0.5", "RADIANCE, RADIANCE, 0.5", "RADIANCE, FIRE, 1.0", "RADIANCE, EARTH, 1.0",
            "RADIANCE, NONE, 1.0", "FIRE, RADIANCE, 1.0", "WATER, RADIANCE, 1.0", "ICE, RADIANCE, 1.0",
    })
    void matchupChart(Element spell, String creature, float expected) {
        Element target = creature.equals("NONE") ? null : Element.valueOf(creature);
        assertEquals(expected, spell.multiplierAgainst(target), 1e-6f);
    }
}
