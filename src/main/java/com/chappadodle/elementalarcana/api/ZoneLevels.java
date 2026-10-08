package com.chappadodle.elementalarcana.api;

/**
 * A creature's level from where it is (fixed zones, never from the player's level). Plain Java,
 * unit tested; CreatureLevels feeds it the world. {@code spread} is a random 0..SPREAD-1, so the
 * creatures in one place aren't all the same.
 * <ul>
 * <li>Overworld: 1 to 5 near world spawn, +1 every 150 blocks away (up to +30); caves (by depth below
 * sea level, up to +10), structures (+5) and dangerous biomes (+10) add up to +15 more.</li>
 * <li>Nether: 25 to 50, rising with distance from its centre and in structures.</li>
 * <li>End: 40 on the central island (where the dragon waits), rising 1 every 40 blocks out to 85 on the
 * far outer islands.</li>
 * </ul>
 */
public final class ZoneLevels {
    public static final int SPREAD = 5;
    private static final int SEA_LEVEL = 63;

    public enum Dimension { OVERWORLD, NETHER, END }

    private ZoneLevels() {
    }

    public static int level(Dimension dimension, double distance, int y, boolean underground, boolean inStructure,
                            boolean dangerousBiome, int spread) {
        int level = switch (dimension) {
            case OVERWORLD -> {
                int cave = underground ? Math.clamp((SEA_LEVEL - y) / 10, 0, 10) : 0;
                int extra = Math.min(15, cave + (inStructure ? 5 : 0) + (dangerousBiome ? 10 : 0));
                yield 1 + spread + Math.min(30, (int) (distance / 150)) + extra;
            }
            case NETHER -> Math.clamp(25 + spread + (int) (distance / 50) + (inStructure ? 5 : 0), 25, 50);
            case END -> 40 + spread + Math.min(45, (int) (distance / 40));
        };
        return Math.clamp(level, 1, Progression.MAX_LEVEL);
    }
}
