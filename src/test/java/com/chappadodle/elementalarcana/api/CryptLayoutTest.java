package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.api.CryptLayout.Kind;
import com.chappadodle.elementalarcana.api.CryptLayout.Room;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CryptLayoutTest {
    private static final int SEEDS = 3000;

    private static List<Room> plan(long seed, int heading) {
        Random random = new Random(seed);
        return CryptLayout.plan(random::nextInt, heading);
    }

    @Test
    void theMainWayRunsEntryHallGateHallGateChamber() {
        for (int heading = 0; heading < 4; heading++) {
            for (long seed = 0; seed < SEEDS; seed++) {
                List<Room> rooms = plan(seed, heading);
                assertEquals(Kind.ENTRY, rooms.get(0).kind());
                Set<Kind> halls = Set.of(rooms.get(1).kind(), rooms.get(3).kind());
                assertEquals(Set.of(Kind.TOMBS, Kind.GLYPHS), halls);
                assertEquals(Kind.GATE, rooms.get(2).kind());
                assertEquals(Kind.GATE, rooms.get(4).kind());
                assertEquals(Kind.CHAMBER, rooms.get(5).kind());
                assertTrue(rooms.size() <= 8);
                for (Room side : rooms.subList(6, rooms.size())) {
                    assertTrue(side.kind() == Kind.LIBRARY || side.kind() == Kind.STORE);
                }
            }
        }
    }

    @Test
    void roomsNeverOverlapEachOtherOrTheStair() {
        for (int heading = 0; heading < 4; heading++) {
            for (long seed = 0; seed < SEEDS; seed++) {
                Set<Long> cells = new HashSet<>();
                for (int[] cell : CryptLayout.stairCells(heading)) {
                    assertTrue(cells.add(key(cell[0], cell[1])));
                }
                for (Room room : plan(seed, heading)) {
                    for (int[] cell : CryptLayout.cellsOf(room)) {
                        assertTrue(cells.add(key(cell[0], cell[1])), "seed " + seed + " heading " + heading + " " + room);
                    }
                }
            }
        }
    }

    @Test
    void everythingStaysWithinReachOfTheStartChunk() {
        for (int heading = 0; heading < 4; heading++) {
            for (long seed = 0; seed < SEEDS; seed++) {
                for (Room room : plan(seed, heading)) {
                    for (int[] cell : CryptLayout.cellsOf(room)) {
                        assertTrue(CryptLayout.inReach(cell[0], cell[1], heading), "seed " + seed + " " + room);
                    }
                }
            }
        }
        // A straight crypt fits: the chamber's far wall is 117 blocks out.
        assertTrue(CryptLayout.inReach(0, 6, 0));
        assertFalse(CryptLayout.inReach(0, 7, 0));
    }

    @Test
    void doorsComeInPairsAndTheWayIsConnected() {
        for (int heading = 0; heading < 4; heading++) {
            for (long seed = 0; seed < SEEDS; seed++) {
                List<Room> rooms = plan(seed, heading);
                Map<Long, Room> byCell = new HashMap<>();
                for (Room room : rooms) {
                    byCell.put(key(room.cellX(), room.cellZ()), room);
                }
                Room entry = rooms.get(0);
                assertTrue(entry.hasDoor(CryptLayout.opposite(heading)), "the stair comes in from behind");
                for (Room room : rooms) {
                    for (int d = 0; d < 4; d++) {
                        if (!room.hasDoor(d) || room == entry && d == CryptLayout.opposite(heading)) {
                            continue;
                        }
                        Room other = byCell.get(key(room.cellX() + CryptLayout.dx(d), room.cellZ() + CryptLayout.dz(d)));
                        if (room.kind() == Kind.CHAMBER) {
                            assertEquals(CryptLayout.opposite(room.forward()), d);
                            other = byCell.get(key(room.cellX() - CryptLayout.dx(room.forward()), room.cellZ() - CryptLayout.dz(room.forward())));
                            assertNotNull(other);
                            assertEquals(Kind.GATE, other.kind());
                            continue;
                        }
                        assertNotNull(other, "seed " + seed + " " + room + " door " + d);
                        assertTrue(other.hasDoor(CryptLayout.opposite(d)), "seed " + seed + " " + room + " -> " + other);
                    }
                }
                // Each room of the main way opens forward onto the next.
                for (int i = 0; i < 5; i++) {
                    Room room = rooms.get(i);
                    Room next = rooms.get(i + 1);
                    assertTrue(room.hasDoor(room.forward()));
                    assertEquals(next.cellX(), room.cellX() + CryptLayout.dx(room.forward()));
                    assertEquals(next.cellZ(), room.cellZ() + CryptLayout.dz(room.forward()));
                }
                assertEquals(rooms.get(4).forward(), rooms.get(5).forward(), "the chamber lies on along the last gate's way out");
                // Side rooms hang off the way past the entry, by one door.
                for (Room side : rooms.subList(6, rooms.size())) {
                    assertEquals(1, Integer.bitCount(side.doors()));
                    Room host = byCell.get(key(side.cellX() - CryptLayout.dx(side.forward()), side.cellZ() - CryptLayout.dz(side.forward())));
                    assertNotNull(host);
                    assertTrue(rooms.indexOf(host) >= 1 && rooms.indexOf(host) <= 4, "seed " + seed + " " + side);
                }
            }
        }
    }

    @Test
    void theWayTurnsSometimesAndSideRoomsUsuallyFit() {
        int turned = 0;
        int sideRooms = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            List<Room> rooms = plan(seed, 0);
            if (rooms.stream().limit(6).anyMatch(room -> room.forward() != 0)) {
                turned++;
            }
            sideRooms += rooms.size() - 6;
        }
        assertTrue(turned > SEEDS / 2, "turned " + turned);
        assertTrue(sideRooms > SEEDS * 3 / 2, "side rooms " + sideRooms);
    }

    @Test
    void plansAreRepeatable() {
        assertEquals(plan(42, 1), plan(42, 1));
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }
}
