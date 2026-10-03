package com.chappadodle.elementalarcana.api;

/**
 * Smite's numbers by level and fork (see the Smite spec). The spell: a mark on the ground, then a
 * pillar of light falling on it.
 * <pre>
 * Lv1 Smite            2.5 blocks, after 0.75 s      Lv6  Purge         strips foes' boons, players' banes
 * Lv2 Wide Judgment    3.5 blocks                    Lv7  Holy Fire     undead take double; 6 s alight
 * Lv3 Swift Verdict    after 0.5 s                   Lv8  Halo          Regeneration for the caster
 * Lv4 Consecration     holy ground for 4 s           Lv9  Radiant Burst throws back and blinds
 * Lv5 Sunlance | Triple Judgment                     Lv10 Wrath of Heaven | Avatar of Light
 * </pre>
 */
public final class SmiteRules {
    public static final String SUNLANCE = "sunlance";
    public static final String TRIPLE = "triple_judgment";
    public static final String WRATH = "wrath_of_heaven";
    public static final String AVATAR = "avatar_of_light";

    public static final float DAMAGE = 7f;
    public static final double RANGE = 24;
    public static final int GLOW_TICKS = 100;
    /** How high the pillar reaches. */
    public static final double HEIGHT = 14;
    /** Consecration: how long the ground stays holy, and what it does each second. */
    public static final int CONSECRATION_TICKS = 80;
    public static final float CONSECRATION_UNDEAD_DAMAGE = 1f;
    public static final float CONSECRATION_HEAL = 1f;
    /** Sunlance: how long the beam lasts, how fast it follows the aim, how often and how hard it burns. */
    public static final int LANCE_TICKS = 60;
    public static final double LANCE_SPEED = 0.15;
    public static final int LANCE_INTERVAL = 10;
    public static final float LANCE_SHARE = 0.4f;
    /** Triple Judgment: how wide its pillars are, how far apart they fall (so they don't overlap), and how long after one another. */
    public static final double TRIPLE_RADIUS = 2;
    public static final double TRIPLE_SPACING = 4;
    public static final int TRIPLE_GAP_TICKS = 5;
    public static final int HALO_TICKS = 80;
    /** Radiant Burst: how hard it throws foes back, and how long it blinds them. */
    public static final double BURST_PUSH = 0.9;
    public static final int BLIND_TICKS = 40;
    /** The small pillars of Wrath of Heaven and Avatar of Light. */
    public static final double SMALL_RADIUS = 1.5;
    public static final float SMALL_SHARE = 0.6f;
    public static final int SMALL_DELAY = 6;
    public static final int WRATH_PILLARS = 6;
    public static final int WRATH_TICKS = 40;
    public static final double WRATH_RADIUS = 6;
    public static final int AVATAR_TICKS = 160;
    public static final int AVATAR_INTERVAL = 20;
    public static final double AVATAR_RANGE = 12;
    /** Avatar of Light: what share of the damage it takes the caster still feels. */
    public static final float AVATAR_GUARD = 0.8f;

    private SmiteRules() {
    }

    public static double radius(int level) {
        return level >= 2 ? 3.5 : 2.5;
    }

    /** How long after the mark the pillar falls. */
    public static int delay(int level) {
        return level >= 3 ? 10 : 15;
    }

    public static boolean consecrates(int level) {
        return level >= 4;
    }

    public static boolean purges(int level) {
        return level >= 6;
    }

    /** How much harder the undead are struck. */
    public static float undead(int level) {
        return level >= 7 ? 2f : 1.5f;
    }

    public static int fireSeconds(int level) {
        return level >= 7 ? 6 : 3;
    }

    public static boolean halo(int level) {
        return level >= 8;
    }

    public static boolean bursts(int level) {
        return level >= 9;
    }

    /** When Wrath of Heaven's small pillar {@code index} (0 to 5) falls, after the first. */
    public static int wrathDelay(int index) {
        return SMALL_DELAY + index * (WRATH_TICKS / WRATH_PILLARS);
    }
}
