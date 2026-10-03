package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TremorRulesTest {
    private static final int FLOOR = 64;

    /** A shockwave running east from (0.5, 64, 0.5), its point i at x = 0.5 + i. */
    private static int[] eastward(GroundPath.Ground ground) {
        return TremorRules.path(ground, 0.5, FLOOR, 0.5, 1, 0);
    }

    @Test
    void flatGroundRunsTenBlocks() {
        int[] path = eastward((x, y, z) -> y < FLOOR);
        assertEquals(TremorRules.LENGTH + 1, path.length);
        assertTrue(TremorRules.hasRoom(path));
    }

    @Test
    void itClimbsAStepAndFollowsADropOfTwo() {
        int[] path = eastward((x, y, z) -> y < (x >= 7 ? FLOOR - 1 : x >= 3 ? FLOOR + 1 : FLOOR));
        assertArrayEquals(new int[]{64, 64, 64, 65, 65, 65, 65, 63, 63, 63, 63}, path);
    }

    @Test
    void aWallOrACliffEndsIt() {
        assertEquals(4, eastward((x, y, z) -> y < (x >= 4 ? FLOOR + 2 : FLOOR)).length);
        assertEquals(6, eastward((x, y, z) -> y < (x >= 6 ? FLOOR - 3 : FLOOR)).length);
    }

    @Test
    void aWallRightAheadLeavesNoRoom() {
        assertFalse(TremorRules.hasRoom(eastward((x, y, z) -> y < (x >= 1 ? FLOOR + 2 : FLOOR))));
    }

    @Test
    void itCatchesWhatStandsWhereItPasses() {
        assertTrue(TremorRules.catches(0, 0, 0, 0.3));
        assertTrue(TremorRules.catches(0.8, 1.8, 0.5, 0.3));
        assertFalse(TremorRules.catches(1.0, 0, 0, 0.3));
        assertFalse(TremorRules.catches(0, 2.0, 0, 0.3));
    }

    @Test
    void aJumpOverItEscapes() {
        assertTrue(TremorRules.catches(0, 0, 1.0, 0.3));
        assertFalse(TremorRules.catches(0, 0, 1.2, 0.3));
    }
}
