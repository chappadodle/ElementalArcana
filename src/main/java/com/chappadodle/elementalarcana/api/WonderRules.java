package com.chappadodle.elementalarcana.api;

/**
 * The Wonders of the Wild's numbers (docs/superpowers/specs/2026-10-06-wonders-of-the-wild-design.md):
 * when glowmoths are about and how many come, where skyrays fly, and how a skyray's wake lifts a
 * glider. Plain Java, unit tested.
 */
public final class WonderRules {
    /** Glowmoths: how many may be about a player before no more come, and how near they come. */
    public static final int MOTHS_NEAR = 6;
    public static final int MOTH_REACH = 16;
    /** How far a glowmoth looks for a brighter spot to flutter toward. */
    public static final int LIGHT_REACH = 4;
    /** Skyrays: how many may be in view of a player, and the heights they fly between. */
    public static final int SKYRAYS_NEAR = 3;
    public static final int SKYRAY_MIN_Y = 100;
    public static final int SKYRAY_MAX_Y = 150;
    /** A skyray's wake: how near a glider must be, the lift it gives a tick, the fastest it lifts. */
    public static final double WAKE_REACH = 8;
    public static final double WAKE_LIFT = 0.05;
    public static final double WAKE_MAX_RISE = 0.12;
    /** The server's reach for the wake's advancement: a little more, as it checks only now and then. */
    public static final double WAKE_NOTICE = WAKE_REACH + 2;

    private WonderRules() {
    }

    /** Whether glowmoths are about: from dusk (12500) until just before dawn (23500). */
    public static boolean mothTime(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= 12500 && time < 23500;
    }

    /** How many glowmoths come now to a player with {@code near} about: two or three (by {@code roll}), never past six. */
    public static int mothsToBring(int near, double roll) {
        if (near >= MOTHS_NEAR) {
            return 0;
        }
        return Math.min(roll < 0.5 ? 2 : 3, MOTHS_NEAR - near);
    }

    /** A glider's upward speed this tick, borne up by a skyray's wake from {@code rise}. */
    public static double wakeRise(double rise) {
        return Math.max(rise, Math.min(rise + WAKE_LIFT, WAKE_MAX_RISE));
    }
}
