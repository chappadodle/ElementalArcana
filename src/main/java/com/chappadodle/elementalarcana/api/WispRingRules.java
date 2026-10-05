package com.chappadodle.elementalarcana.api;

import java.util.Arrays;
import java.util.List;

/**
 * Wisp Rings' rules (docs/superpowers/specs/2026-10-06-wisp-rings-design.md): the ring's size, when
 * it's night, which night it is (each ring acts once a night for each player), and what the wisps do
 * to whoever steps in: a boon two times in three, a trick otherwise.
 */
public final class WispRingRules {
    /** The ring of mushrooms and flowers: this far from the middle (seven across). */
    public static final int RADIUS = 3;
    public static final double BOON_CHANCE = 2.0 / 3.0;
    private static final long DAY = 24000;
    private static final long DUSK = 13000;
    private static final long DAWN = 23000;

    /** What the wisps do. */
    public enum Outcome {
        FEY_LUCK(true), WISP_SIGHT(true), MOONLIT_STEP(true), GIFT(true), FULL_MOON(true),
        TURNED_AROUND(false), SMALL_FOLK(false), WILL_O_THE_WISP(false);

        private final boolean boon;

        Outcome(boolean boon) {
            this.boon = boon;
        }

        public boolean boon() {
            return boon;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    private WispRingRules() {
    }

    /** Whether the wisps dance at {@code dayTime} (between dusk and dawn). */
    public static boolean isNight(long dayTime) {
        long time = Math.floorMod(dayTime, DAY);
        return time >= DUSK && time < DAWN;
    }

    /** Which night {@code dayTime} belongs to: the same number from dusk until the next noon. */
    public static long night(long dayTime) {
        return Math.floorDiv(dayTime - DAY / 2, DAY);
    }

    /** What the wisps do, from two rolls in [0, 1): one for boon or trick, one for which. */
    public static Outcome pick(double boonRoll, double which) {
        boolean boon = boonRoll < BOON_CHANCE;
        List<Outcome> pool = Arrays.stream(Outcome.values()).filter(outcome -> outcome.boon() == boon).toList();
        return pool.get(Math.min(pool.size() - 1, (int) (which * pool.size())));
    }

    /** Whether a horizontal offset from a ring's middle is inside it. */
    public static boolean inside(double dx, double dz) {
        return dx * dx + dz * dz <= (RADIUS + 0.5) * (RADIUS + 0.5);
    }
}
