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
    })
    void matchupChart(Element spell, String creature, float expected) {
        Element target = creature.equals("NONE") ? null : Element.valueOf(creature);
        assertEquals(expected, spell.multiplierAgainst(target), 1e-6f);
    }
}
