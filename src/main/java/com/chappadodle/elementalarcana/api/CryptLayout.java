package com.chappadodle.elementalarcana.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * Plans an Arcane Crypt (docs/superpowers/specs/2026-10-04-arcane-crypts-design.md): rooms on a grid
 * of 13-block cells, counted from the entry hall at (0, 0). Plain Java, unit tested; CryptStructure
 * builds the plan. Directions are Minecraft's 2D values: 0 south (+z), 1 west (-x), 2 north (-z), 3
 * east (+x). The stair comes down into the entry hall from behind, along the heading, from the
 * mausoleum ENTRY_OFFSET blocks back.
 */
public final class CryptLayout {
    public static final int CELL = 13;
    /** How far the entry hall's middle lies from the mausoleum's, along the heading. */
    public static final int ENTRY_OFFSET = 34;
    /** How far (either axis) a cell's middle may lie from the mausoleum's: pieces must stay within 8 chunks of the start. */
    public static final int REACH = 114;
    /** The cells behind the entry hall that the stair and mausoleum take. */
    public static final int STAIR_CELLS = 3;
    private static final int[] DX = {0, -1, 0, 1};
    private static final int[] DZ = {1, 0, -1, 0};
    private static final int ATTEMPTS = 40;

    public enum Kind { ENTRY, TOMBS, GLYPHS, GATE, LIBRARY, STORE, CHAMBER }

    /**
     * A room: its cell, its way out (the way on for rooms of the main way, away from the room it
     * hangs off for side rooms) and a bit for each side with a door. A gate's way out is sealed. The
     * chamber's cell is the one it's entered through: it covers that cell and the next one on, and
     * half of each cell beside those two.
     */
    public record Room(Kind kind, int cellX, int cellZ, int forward, int doors) {
        public boolean hasDoor(int direction) {
            return (doors >> direction & 1) != 0;
        }

        Room withDoor(int direction) {
            return new Room(kind, cellX, cellZ, forward, doors | 1 << direction);
        }

        Room withForward(int direction) {
            return new Room(kind, cellX, cellZ, direction, doors);
        }
    }

    private CryptLayout() {
    }

    public static int dx(int direction) {
        return DX[direction];
    }

    public static int dz(int direction) {
        return DZ[direction];
    }

    /** The direction to the right of someone facing {@code direction}. */
    public static int right(int direction) {
        return (direction + 1) & 3;
    }

    public static int left(int direction) {
        return (direction + 3) & 3;
    }

    public static int opposite(int direction) {
        return (direction + 2) & 3;
    }

    /**
     * Plans a crypt going {@code heading} from its mausoleum. {@code random} gives a number from 0
     * to its argument (exclusive). The main way comes first (entry, a hall, a gate, a hall, a gate),
     * then the chamber, then whichever side rooms fit.
     */
    public static List<Room> plan(IntUnaryOperator random, int heading) {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            List<Room> rooms = tryPlan(random, heading, true);
            if (rooms != null) {
                return rooms;
            }
        }
        // A straight way always fits.
        return tryPlan(random, heading, false);
    }

    /** The cells the stair and mausoleum take, behind the entry hall. */
    public static List<int[]> stairCells(int heading) {
        List<int[]> cells = new ArrayList<>();
        for (int i = 1; i <= STAIR_CELLS; i++) {
            cells.add(new int[]{-DX[heading] * i, -DZ[heading] * i});
        }
        return cells;
    }

    /** The six cells the chamber covers (see Room). */
    public static List<int[]> chamberCells(int cellX, int cellZ, int forward) {
        int r = right(forward);
        List<int[]> cells = new ArrayList<>();
        for (int along = 0; along <= 1; along++) {
            for (int side = -1; side <= 1; side++) {
                cells.add(new int[]{cellX + DX[forward] * along + DX[r] * side, cellZ + DZ[forward] * along + DZ[r] * side});
            }
        }
        return cells;
    }

    /** The cells a room covers. */
    public static List<int[]> cellsOf(Room room) {
        return room.kind() == Kind.CHAMBER ? chamberCells(room.cellX(), room.cellZ(), room.forward())
                : List.of(new int[]{room.cellX(), room.cellZ()});
    }

    /** Whether a cell's middle is within REACH of the mausoleum's, for a crypt going {@code heading}. */
    public static boolean inReach(int cellX, int cellZ, int heading) {
        int x = DX[heading] * ENTRY_OFFSET + cellX * CELL;
        int z = DZ[heading] * ENTRY_OFFSET + cellZ * CELL;
        return Math.abs(x) <= REACH && Math.abs(z) <= REACH;
    }

    private static List<Room> tryPlan(IntUnaryOperator random, int heading, boolean turns) {
        Set<Long> used = new HashSet<>();
        for (int[] cell : stairCells(heading)) {
            used.add(key(cell[0], cell[1]));
        }
        boolean tombsFirst = random.applyAsInt(2) == 0;
        List<Kind> way = List.of(Kind.ENTRY, tombsFirst ? Kind.TOMBS : Kind.GLYPHS, Kind.GATE,
                tombsFirst ? Kind.GLYPHS : Kind.TOMBS, Kind.GATE);
        List<Room> rooms = new ArrayList<>();
        int x = 0;
        int z = 0;
        int direction = heading;
        used.add(key(0, 0));
        rooms.add(new Room(Kind.ENTRY, 0, 0, heading, 1 << opposite(heading)));
        for (int i = 1; i < way.size(); i++) {
            int next = choose(random, direction, x, z, used, heading, turns);
            if (next < 0) {
                return null;
            }
            int last = rooms.size() - 1;
            rooms.set(last, rooms.get(last).withForward(next).withDoor(next));
            x += DX[next];
            z += DZ[next];
            used.add(key(x, z));
            rooms.add(new Room(way.get(i), x, z, next, 1 << opposite(next)));
            direction = next;
        }

        // The chamber, beyond the last gate: straight on if it fits, else to one side.
        int[] tries = turns ? (random.applyAsInt(2) == 0
                ? new int[]{direction, left(direction), right(direction)}
                : new int[]{direction, right(direction), left(direction)})
                : new int[]{direction};
        int chamberDirection = -1;
        for (int candidate : tries) {
            if (fits(chamberCells(x + DX[candidate], z + DZ[candidate], candidate), used, heading)) {
                chamberDirection = candidate;
                break;
            }
        }
        if (chamberDirection < 0) {
            return null;
        }
        int gate = rooms.size() - 1;
        rooms.set(gate, rooms.get(gate).withForward(chamberDirection).withDoor(chamberDirection));
        Room chamber = new Room(Kind.CHAMBER, x + DX[chamberDirection], z + DZ[chamberDirection], chamberDirection,
                1 << opposite(chamberDirection));
        for (int[] cell : cellsOf(chamber)) {
            used.add(key(cell[0], cell[1]));
        }
        rooms.add(chamber);

        // Side rooms hang off the main way past the entry, wherever a neighbouring cell is free.
        List<Kind> sides = random.applyAsInt(2) == 0 ? List.of(Kind.LIBRARY, Kind.STORE) : List.of(Kind.STORE, Kind.LIBRARY);
        for (Kind kind : sides) {
            List<Integer> parents = new ArrayList<>(List.of(1, 2, 3, 4));
            shuffle(parents, random);
            attach:
            for (int parent : parents) {
                Room host = rooms.get(parent);
                List<Integer> directions = new ArrayList<>(List.of(0, 1, 2, 3));
                shuffle(directions, random);
                for (int d : directions) {
                    int cx = host.cellX() + DX[d];
                    int cz = host.cellZ() + DZ[d];
                    if (host.hasDoor(d) || used.contains(key(cx, cz)) || !inReach(cx, cz, heading)) {
                        continue;
                    }
                    rooms.set(parent, host.withDoor(d));
                    rooms.add(new Room(kind, cx, cz, d, 1 << opposite(d)));
                    used.add(key(cx, cz));
                    break attach;
                }
            }
        }
        return rooms;
    }

    /** The way on from a room: straight on twice as often as each turn, never back, into a free cell in reach. */
    private static int choose(IntUnaryOperator random, int direction, int x, int z, Set<Long> used, int heading, boolean turns) {
        List<Integer> options = new ArrayList<>(turns ? List.of(direction, direction, left(direction), right(direction)) : List.of(direction));
        while (!options.isEmpty()) {
            int pick = options.remove(random.applyAsInt(options.size()));
            int cx = x + DX[pick];
            int cz = z + DZ[pick];
            if (!used.contains(key(cx, cz)) && inReach(cx, cz, heading)) {
                return pick;
            }
            options.removeIf(other -> other == pick);
        }
        return -1;
    }

    private static boolean fits(List<int[]> cells, Set<Long> used, int heading) {
        for (int[] cell : cells) {
            if (used.contains(key(cell[0], cell[1])) || !inReach(cell[0], cell[1], heading)) {
                return false;
            }
        }
        return true;
    }

    private static <T> void shuffle(List<T> list, IntUnaryOperator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = random.applyAsInt(i + 1);
            T swap = list.get(i);
            list.set(i, list.get(j));
            list.set(j, swap);
        }
    }

    private static long key(int x, int z) {
        return (long) x << 32 | (z & 0xFFFFFFFFL);
    }
}
