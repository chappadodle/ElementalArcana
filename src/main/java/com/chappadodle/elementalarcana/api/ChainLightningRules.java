package com.chappadodle.elementalarcana.api;

/**
 * Chain Lightning's numbers by level and fork (see the Chain Lightning spec). The chain: a strike
 * on the creature under the crosshair, then jumps from creature to creature, each a little weaker;
 * a wet creature conducts it (more damage, more jumps from it).
 * <pre>
 * Lv1 Chain Lightning   3 jumps of 6 blocks, a fifth weaker each
 * Lv2 Long Arc          jumps of 8, first strike 26 away
 * Lv3 Static            each strike stuns for half a second
 * Lv4 Forked Chain      4 jumps, a tenth weaker each
 * Lv5 Storm Fork | Overload
 * Lv6 Conductor         wet: double damage, 3 more jumps, up to 8 jumps
 * Lv7 Thunderstruck     a bolt from the sky on the first strike, half again as hard
 * Lv8 Live Wire         5 jumps; struck creatures arc to each other for 3 s
 * Lv9 Supercharge       no falloff
 * Lv10 Ball Lightning | Thunder Lord
 * </pre>
 */
public final class ChainLightningRules {
    public static final String STORM_FORK = "storm_fork";
    public static final String OVERLOAD = "overload";
    public static final String BALL_LIGHTNING = "ball_lightning";
    public static final String THUNDER_LORD = "thunder_lord";

    public static final float DAMAGE = 5f;
    /** Storm Fork: how many creatures a forking chain may strike in all. */
    public static final int FORK_STRIKES = 10;
    /** Overload: the chain's jumps, and the shock around every strike. */
    public static final int OVERLOAD_JUMPS = 2;
    public static final double OVERLOAD_RADIUS = 2.5;
    public static final float OVERLOAD_SHARE = 0.5f;
    public static final int STUN_TICKS = 10;
    public static final float SKY_BONUS = 1.5f;
    /** Live Wire: how long struck creatures stay charged, how often and how far they arc, and for how much. */
    public static final int LIVE_WIRE_TICKS = 60;
    public static final int LIVE_WIRE_INTERVAL = 20;
    public static final double LIVE_WIRE_RANGE = 4;
    public static final float LIVE_WIRE_DAMAGE = 1f;
    /** Ball Lightning: its orb's life, speed, how often and how far it strikes, and its chains. */
    public static final int BALL_TICKS = 80;
    public static final double BALL_SPEED = 1 / 3.0;
    public static final int BALL_INTERVAL = 15;
    public static final double BALL_REACH = 6;
    public static final int BALL_JUMPS = 2;
    public static final float BALL_SHARE = 0.4f;
    /** Thunder Lord: how long it can be held, how often it strikes again, how hard, and the mana per pulse. */
    public static final int LORD_TICKS = 60;
    public static final int LORD_INTERVAL = 10;
    public static final float LORD_SHARE = 0.6f;
    public static final int LORD_UPKEEP = 6;

    private ChainLightningRules() {
    }

    /** How far away the first strike can be. */
    public static double range(int level) {
        return level >= 2 ? 26 : 20;
    }

    /** How far the bolt jumps from one creature to the next. */
    public static double jumpRange(int level) {
        return level >= 2 ? 8 : 6;
    }

    /** How many jumps the chain makes (more from wet creatures). */
    public static int jumps(int level, String fork) {
        if (OVERLOAD.equals(fork)) {
            return OVERLOAD_JUMPS;
        }
        return level >= 8 ? 5 : level >= 4 ? 4 : 3;
    }

    /** What each jump keeps of the last one's damage. */
    public static float falloff(int level) {
        return level >= 9 ? 1f : level >= 4 ? 0.9f : 0.8f;
    }

    /** How much harder a wet creature is struck. */
    public static float conducted(int level) {
        return level >= 6 ? 2f : 1.5f;
    }

    /** How many more jumps a wet creature gives the chain. */
    public static int wetJumps(int level) {
        return level >= 6 ? 3 : 2;
    }

    /** How many creatures the chain may strike in all. */
    public static int maxStrikes(int level, String fork) {
        if (STORM_FORK.equals(fork)) {
            return FORK_STRIKES;
        }
        return (level >= 6 ? 8 : 6) + 1;
    }

    /** How many creatures each strike passes the bolt on to. */
    public static int fanOut(String fork) {
        return STORM_FORK.equals(fork) ? 2 : 1;
    }

    public static int stunTicks(int level) {
        return level >= 3 ? STUN_TICKS : 0;
    }

    public static boolean skyBolt(int level) {
        return level >= 7;
    }

    public static boolean liveWire(int level) {
        return level >= 8;
    }

    /** The damage of a strike {@code jump} jumps down the chain (0: the first), before conduction and power. */
    public static float strike(int level, int jump) {
        return DAMAGE * (float) Math.pow(falloff(level), jump);
    }
}
