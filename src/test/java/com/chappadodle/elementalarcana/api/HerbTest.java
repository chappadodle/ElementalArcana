package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class HerbTest {

    @Test
    void everyElementHasItsOwnHerb() {
        Set<Herb> seen = new HashSet<>();
        for (Element element : Element.values()) {
            Herb herb = Herb.of(element);
            assertEquals(element, herb.element());
            assertTrue(seen.add(herb), "two elements share " + herb);
        }
        assertEquals(Herb.values().length, seen.size());
    }

    @Test
    void idsRoundTrip() {
        for (Herb herb : Herb.values()) {
            assertEquals(herb, Herb.byId(herb.id()));
        }
        assertEquals("emberbloom", Herb.EMBERBLOOM.id());
        assertEquals("tonic_of_embers", Herb.EMBERBLOOM.tonicId());
        assertEquals("tonic_of_dawn", Herb.SUNPETAL.tonicId());
    }

    @Test
    void sunpetalsOpenByDayAndMoonliliesByNight() {
        assertTrue(Herb.SUNPETAL.openAt(true));
        assertFalse(Herb.SUNPETAL.openAt(false));
        assertFalse(Herb.MOONLILY.openAt(true));
        assertTrue(Herb.MOONLILY.openAt(false));
        assertTrue(Herb.EMBERBLOOM.openAt(true));
        assertTrue(Herb.EMBERBLOOM.openAt(false));
    }

    @Test
    void onlyAnOpenFlowerGlows() {
        assertEquals(8, Herb.MOONLILY.light(true));
        assertEquals(0, Herb.MOONLILY.light(false));
        assertEquals(7, Herb.EMBERBLOOM.light(true));
        assertEquals(0, Herb.SKYPLUME.light(true));
    }
}
