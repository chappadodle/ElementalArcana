package com.chappadodle.elementalarcana.api;

import java.util.Locale;

/**
 * What the Circle's mages say (see the Circle spec, part 1): a few lines for each stage of a
 * player's story, the stage by their magic (asleep, or their level), and each mage going round its
 * lines from its own starting place as it's greeted again and again. Plain Java, unit tested; the
 * lines are lang keys, {@code circle.elementalarcana.talk.<stage>.<n>}.
 */
public final class CircleTalk {
    /** How many lines each stage has. */
    public static final int LINES = 4;

    /** A stage of a player's story, as the Circle sees it. */
    public enum Stage {
        SLEEPING, NOVICE, ADEPT, MASTER, ARCHMAGE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private CircleTalk() {
    }

    /** A player's stage: asleep, or by their level (an Adept from 15, a Master from 35, an Archmage from 60). */
    public static Stage stage(boolean awakened, int level) {
        if (!awakened) {
            return Stage.SLEEPING;
        }
        return level >= 60 ? Stage.ARCHMAGE : level >= 35 ? Stage.MASTER : level >= 15 ? Stage.ADEPT : Stage.NOVICE;
    }

    /** Which of a stage's lines a mage says when greeted for the {@code times}th time (from its own starting line, {@code mage}). */
    public static int line(int mage, int times) {
        return Math.floorMod(mage + times, LINES);
    }
}
