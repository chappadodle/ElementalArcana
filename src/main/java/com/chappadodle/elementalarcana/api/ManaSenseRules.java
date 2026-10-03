package com.chappadodle.elementalarcana.api;

/**
 * What a player reads of a creature's aura (see the mana sense spec): with little Insight only how
 * dangerous it feels, then its level, then its element too. Plain Java, unit tested.
 */
public final class ManaSenseRules {
    public static final int READ_LEVEL = 5;
    public static final int READ_ELEMENT = 15;

    public enum Danger {
        WEAK(0x7FE07F), EVEN(0xFFD866), STRONG(0xFF5A5A), DEADLY(0xC060FF);

        private final int color;

        Danger(int color) {
            this.color = color;
        }

        public int color() {
            return color;
        }
    }

    public enum Detail { DANGER, LEVEL, ELEMENT }

    private ManaSenseRules() {
    }

    /** How dangerous a creature {@code gap} levels above you feels (negative: below). */
    public static Danger danger(int gap) {
        if (gap <= -5) {
            return Danger.WEAK;
        }
        if (gap <= 4) {
            return Danger.EVEN;
        }
        return gap <= 9 ? Danger.STRONG : Danger.DEADLY;
    }

    public static Detail detail(int insight) {
        return insight >= READ_ELEMENT ? Detail.ELEMENT : insight >= READ_LEVEL ? Detail.LEVEL : Detail.DANGER;
    }
}
