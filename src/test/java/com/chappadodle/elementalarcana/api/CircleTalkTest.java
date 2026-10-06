package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CircleTalkTest {

    @Test
    void theStoryGoesByLevel() {
        assertEquals(CircleTalk.Stage.SLEEPING, CircleTalk.stage(false, 80));
        assertEquals(CircleTalk.Stage.NOVICE, CircleTalk.stage(true, 1));
        assertEquals(CircleTalk.Stage.NOVICE, CircleTalk.stage(true, 14));
        assertEquals(CircleTalk.Stage.ADEPT, CircleTalk.stage(true, 15));
        assertEquals(CircleTalk.Stage.MASTER, CircleTalk.stage(true, 35));
        assertEquals(CircleTalk.Stage.ARCHMAGE, CircleTalk.stage(true, 60));
        assertEquals("archmage", CircleTalk.Stage.ARCHMAGE.id());
    }

    @Test
    void eachMageGoesRoundItsLines() {
        assertEquals(2, CircleTalk.line(2, 0));
        assertEquals(3, CircleTalk.line(2, 1));
        assertEquals(0, CircleTalk.line(2, 2));
        assertEquals(1, CircleTalk.line(2, 3));
        assertEquals(2, CircleTalk.line(2, 4));
        assertEquals(1, CircleTalk.line(-3, 0));
    }
}
