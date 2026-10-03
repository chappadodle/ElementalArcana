package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

/**
 * Stormeye's numbers by spell level and branch (see the Stormeye spec): how far the tornado
 * reaches, how hard it pulls, how long it lasts, what it hits for, and how big it is.
 */
public final class StormeyeRules {
    public static final String WANDERING = "wandering_storm";
    public static final String TWIN = "twin_storms";
    public static final String GREAT_TEMPEST = "great_tempest";
    public static final String EYE_OF_CALM = "eye_of_calm";

    /** How far away it can be set down, in blocks. */
    public static final double RANGE = 20;
    /** Every how many ticks it hurts what it holds. */
    public static final int HIT_TICKS = 10;
    /** Every how many ticks it pulses (Eye Pulse, Absorption's element, Eye of Calm's mending). */
    public static final int PULSE_TICKS = 20;
    /** Twin Storms: how far each tornado is from the middle they circle. */
    public static final double TWIN_ORBIT = 3;
    /** Wandering Storm: how fast it drifts after where its caster looks, blocks a tick. */
    public static final double WANDER_SPEED = 0.18;
    /** How fast what it holds is carried round its eye, blocks a tick. */
    public static final double SPIN = 0.25;
    /** How high above its foot what it holds is lifted to. */
    public static final double LIFT_HEIGHT = 2.5;

    private StormeyeRules() {
    }

    /** How far from its eye it takes hold, in blocks. */
    public static double reach(int level, @Nullable String branch5, @Nullable String branch10) {
        double reach = level >= 4 ? 7 : 5;
        if (TWIN.equals(branch5)) {
            reach *= 0.6;
        }
        if (GREAT_TEMPEST.equals(branch10)) {
            reach *= 2;
        }
        return reach;
    }

    /** How tall it stands, in blocks. */
    public static double height(@Nullable String branch10) {
        return GREAT_TEMPEST.equals(branch10) ? 14 : 7;
    }

    /** How hard it drags toward its eye, blocks a tick (Gale Force from level 2). */
    public static double pull(int level) {
        return level >= 2 ? 0.32 : 0.22;
    }

    /** How long it lasts, in ticks: 4 seconds, 5 from level 2, 7 from level 8. */
    public static int lifetime(int level) {
        return level >= 8 ? 140 : level >= 2 ? 100 : 80;
    }

    /** What it hits for every half second, before its caster's power (Crushing Winds from level 6). */
    public static float damage(int level) {
        return level >= 6 ? 2.25f : 1.5f;
    }
}
