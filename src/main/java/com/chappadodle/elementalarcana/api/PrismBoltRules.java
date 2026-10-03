package com.chappadodle.elementalarcana.api;

/**
 * Prism Bolt's numbers by level and fork (see the Prism Bolt spec). The bolt: a fast crystal that
 * bursts into shards on whatever it strikes, the shards flying on.
 * <pre>
 * Lv1 Prism Bolt       6 damage; 3 shards of 2.5     Lv6  Crystal Shell   a heart of absorption per hit
 * Lv2 Keen Edge        a fifth harder                Lv7  Twin Prisms     two bolts
 * Lv3 Refraction       5 shards                      Lv8  Resonance       shards hit its bolt's mark half again as hard
 * Lv4 Piercing Light   bursts through one creature   Lv9  Brilliance      shards split once more
 * Lv5 Prismatic Lance | Geode Burst                  Lv10 Crystal Spire | Prism Barrage
 * </pre>
 */
public final class PrismBoltRules {
    public static final String LANCE = "prismatic_lance";
    public static final String GEODE = "geode_burst";
    public static final String SPIRE = "crystal_spire";
    public static final String BARRAGE = "prism_barrage";

    public static final float DAMAGE = 6f;
    public static final float SHARD_DAMAGE = 2.5f;
    public static final float KEEN = 1.2f;
    public static final double SPEED = 2.0;
    public static final double LANCE_SPEED = 3.0;
    public static final int LANCE_PIERCE = 8;
    public static final float GEODE_SHARD_FACTOR = 1.5f;
    /** Crystal Shell: absorption per hit, how long it lasts, and the most it builds to. */
    public static final float SHELL_PER_HIT = 2f;
    public static final int SHELL_TICKS = 300;
    public static final float SHELL_MAX = 8f;
    /** Twin Prisms: how far to either side of the aim the two bolts fly. */
    public static final double TWIN_OFFSET = 0.35;
    public static final float RESONANCE = 1.5f;
    /** Resonance: how long a creature stays marked by the bolt that struck it. */
    public static final int RESONANCE_TICKS = 40;
    public static final float SPLIT_FACTOR = 0.5f;
    public static final double SPLIT_SPREAD_DEGREES = 20;
    /** Crystal Spire: how long it stands, how often and how far it throws, and how fast. */
    public static final int SPIRE_TICKS = 120;
    public static final int SPIRE_INTERVAL = 10;
    public static final double SPIRE_RANGE = 10;
    public static final double SPIRE_SHARD_SPEED = 1.4;
    /** Prism Barrage: how long it can be held, how often it fires (one bolt, at 40%), and the mana per bolt. */
    public static final int BARRAGE_TICKS = 40;
    public static final int BARRAGE_INTERVAL = 6;
    public static final float BARRAGE_SHARE = 0.4f;
    public static final int BARRAGE_UPKEEP = 6;

    private PrismBoltRules() {
    }

    /** What the bolt and its shards deal, as a share of Lv 1's. */
    public static float damageFactor(int level) {
        return level >= 2 ? KEEN : 1f;
    }

    /** How many shards a burst throws. */
    public static int shards(int level, String fork) {
        if (GEODE.equals(fork)) {
            return 10;
        }
        return level >= 3 ? 5 : 3;
    }

    /** The angle between neighbouring shards (a Geode Burst's go all the way round). */
    public static double spreadDegrees(int level, String fork) {
        if (GEODE.equals(fork)) {
            return 360.0 / shards(level, fork);
        }
        return level >= 3 ? 18 : 25;
    }

    /** How many creatures the bolt bursts through before it stops. */
    public static int pierce(int level, String fork) {
        if (LANCE.equals(fork)) {
            return LANCE_PIERCE;
        }
        if (GEODE.equals(fork)) {
            return 0;
        }
        return level >= 4 ? 1 : 0;
    }

    /** Whether the bolt bursts on each creature it passes (a Prismatic Lance bursts only where it stops). */
    public static boolean burstsOnCreatures(String fork) {
        return !LANCE.equals(fork);
    }

    public static double speed(String fork) {
        return LANCE.equals(fork) ? LANCE_SPEED : SPEED;
    }

    /** What a shard deals, as a share of its usual, for the fork. */
    public static float shardFactor(String fork) {
        return GEODE.equals(fork) ? GEODE_SHARD_FACTOR : 1f;
    }

    public static boolean shell(int level) {
        return level >= 6;
    }

    public static int bolts(int level) {
        return level >= 7 ? 2 : 1;
    }

    public static boolean resonance(int level) {
        return level >= 8;
    }

    public static boolean splits(int level) {
        return level >= 9;
    }

    /** The absorption after a Crystal Shell hit, from {@code current}: up one hit's worth, to the cap, never down. */
    public static float shellAfterHit(float current) {
        return current >= SHELL_MAX ? current : Math.min(SHELL_MAX, current + SHELL_PER_HIT);
    }
}
