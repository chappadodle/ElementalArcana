package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TsunamiRulesTest {
    private static final int FLOOR = 64;

    /** A wave rolling east from (0.5, 64, 0.5), its point i at x = 0.5 + 0.5 i. */
    private static int[] eastward(TsunamiRules.Ground ground) {
        return TsunamiRules.path(ground, 0.5, FLOOR, 0.5, 1, 0);
    }

    @Test
    void flatGroundRunsTheWholeWay() {
        int[] path = eastward((x, y, z) -> y < FLOOR);
        assertEquals(TsunamiRules.MAX_LENGTH, TsunamiRules.length(path), 1e-9);
        for (int height : path) {
            assertEquals(FLOOR, height);
        }
    }

    @Test
    void aStepOfOneIsClimbed() {
        int[] path = eastward((x, y, z) -> y < (x >= 5 ? FLOOR + 1 : FLOOR));
        assertEquals(TsunamiRules.MAX_LENGTH, TsunamiRules.length(path), 1e-9);
        // x = 5.0 is point 9.
        assertEquals(FLOOR, path[8]);
        assertEquals(FLOOR + 1, path[9]);
        assertEquals(FLOOR + 1, path[path.length - 1]);
    }

    @Test
    void aDropOfThreeIsFollowed() {
        int[] path = eastward((x, y, z) -> y < (x >= 5 ? FLOOR - 3 : FLOOR));
        assertEquals(TsunamiRules.MAX_LENGTH, TsunamiRules.length(path), 1e-9);
        assertEquals(FLOOR - 3, path[9]);
    }

    @Test
    void aDropOfFourEndsIt() {
        int[] path = eastward((x, y, z) -> y < (x >= 5 ? FLOOR - 4 : FLOOR));
        assertEquals(9, path.length);
        assertEquals(4.0, TsunamiRules.length(path), 1e-9);
    }

    @Test
    void aTwoBlockWallEndsIt() {
        int[] path = eastward((x, y, z) -> y < FLOOR || x >= 6 && y < FLOOR + 2);
        // The last point before x = 6.0 (point 11) is x = 5.5.
        assertEquals(11, path.length);
        assertTrue(TsunamiRules.hasRoom(path));
    }

    @Test
    void aWallRightInFrontLeavesNoRoom() {
        int[] path = eastward((x, y, z) -> y < FLOOR || x >= 2 && y < FLOOR + 3);
        assertEquals(3, path.length);
        assertFalse(TsunamiRules.hasRoom(path));
    }

    @Test
    void waterIsRidden() {
        // A lake: water (solid to the wave) down to 50 from x = 3, its surface level with the land.
        int[] path = eastward((x, y, z) -> y < FLOOR && (x < 3 || y >= 50));
        assertEquals(TsunamiRules.MAX_LENGTH, TsunamiRules.length(path), 1e-9);
        assertEquals(FLOOR, path[path.length - 1]);
    }
}
