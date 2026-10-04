package com.chappadodle.elementalarcana.api;

/**
 * The numbers of the Arcane Crypts (docs/superpowers/specs/2026-10-04-arcane-crypts-design.md).
 * Plain Java, unit tested.
 */
public final class CryptRules {
    /** No crypt within this many blocks of the world's centre. */
    public static final int MIN_DISTANCE = 400;
    /** The rooms' floor lies at least this far below the lowest ground over them. */
    public static final int COVER = 16;
    /** From the mausoleum's middle to the entry hall's back wall, along the heading. */
    public static final int STAIR_RUN = 28;
    /** The ceiling's height over a room's floor, and over the chamber's. */
    public static final int ROOM_HEIGHT = 6;
    public static final int CHAMBER_HEIGHT = 10;
    public static final int CHAMBER_SIZE = 25;
    /** A coffin opens when someone comes this close (horizontally; within 3 blocks up or down). */
    public static final double COFFIN_REACH = 4.0;
    public static final float MAGUS_CHANCE = 0.25f;
    /** A glyph flares this long before it bursts, then rests this long. */
    public static final int GLYPH_WARNING_TICKS = 10;
    public static final int GLYPH_REST_TICKS = 60;
    public static final double GLYPH_RADIUS = 1.6;
    /** A spell lights the runestone its caster looks at, this far away at most. */
    public static final double RUNE_REACH = 24;
    public static final int RUNES_PER_GATE = 3;
    /** The Revenant rises when someone comes this close to the grave flame. */
    public static final double RISE_RADIUS = 10;
    public static final int RISE_TICKS = 40;
    public static final int GRAVE_STEP_COOLDOWN_TICKS = 100;
    public static final double GRAVE_STEP_CLOSE = 3.5;
    public static final double GRAVE_STEP_MIN = 6;
    public static final double GRAVE_STEP_MAX = 9;
    /** With no one to fight for this long, the Revenant goes back to its tomb and heals. */
    public static final int CALM_TICKS = 400;
    /** How far from the chamber's middle the Revenant may go. */
    public static final double LEASH = 11;
    /** One crypt in this many belongs to its element's kin. */
    public static final int KIN_ODDS = 6;

    private CryptRules() {
    }

    /** A glyph's burst: 4 damage, plus a quarter of the place's creature level. */
    public static float glyphDamage(int zoneLevel) {
        return 4f + Math.max(0, zoneLevel) / 4f;
    }

    /** Whether the stair from a mausoleum on {@code ground} reaches rooms whose floor is at {@code floorY}. */
    public static boolean stairReaches(int ground, int floorY) {
        int drop = ground - floorY;
        return drop >= COVER && drop <= STAIR_RUN;
    }

    /**
     * The stair's block {@code along} blocks on from the mausoleum's middle (from -1, set in its
     * floor): one lower for each block on, then level with the rooms' floor. Above the floor it's a
     * stair block, sitting on the one before.
     */
    public static int stepY(int ground, int floorY, int along) {
        return Math.max(floorY, ground - 1 - along);
    }

    /** A crypt's element given the land's: one in KIN_ODDS belongs to its kin (Water's kin is Ice). */
    public static Element element(Element land, boolean kin) {
        if (!kin) {
            return land;
        }
        return switch (land) {
            case FIRE -> Element.RADIANCE;
            case WIND -> Element.LIGHTNING;
            case EARTH -> Element.CRYSTAL;
            case WATER, ICE -> Element.ICE;
            default -> land;
        };
    }

    /** A coffin's dead: a Magus for rolls under MAGUS_CHANCE, else an Adept. */
    public static AttunementRank coffinRank(float roll) {
        return roll < MAGUS_CHANCE ? AttunementRank.MAGUS : AttunementRank.ADEPT;
    }
}
