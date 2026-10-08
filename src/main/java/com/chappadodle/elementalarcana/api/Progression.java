package com.chappadodle.elementalarcana.api;

/**
 * The level curve and what getting better is worth. Plain Java, no Minecraft types (unit tested).
 * Everyone (players and creatures) has a level up to 100; XP to the next one grows 9% a level and
 * comes from absorbing the mana of what you kill, scaled by the level gap. The same gap scales
 * damage both ways. A spell's own level shortens its cooldown and Focus shortens it more;
 * Elemental Essence fills mastery (less at higher levels) and condenses into skill points (costing
 * more each time). See docs/superpowers/specs/2026-10-01-progression-system-design.md.
 */
public final class Progression {
    public static final int MAX_LEVEL = 100;
    // Each spell level above 1 takes this share of the Lv 1 cooldown off (Lv 10 = 46%).
    private static final float COOLDOWN_CUT_PER_LEVEL = 0.06f;
    private static final double XP_BASE = 55;
    private static final double XP_GROWTH = 1.09;
    private static final double FIRST_KILLS_PER_LEVEL = 8;
    private static final double LAST_KILLS_PER_LEVEL = 40;
    private static final double DAMAGE_PER_LEVEL = 1.045;
    private static final int MAX_DAMAGE_GAP = 40;
    private static final float ESSENCE_FIRST_FILL = 0.5f;
    private static final float ESSENCE_FALLOFF = 0.8f;
    private static final int CONDENSE_BASE_COST = 8;
    private static final int CONDENSE_COST_STEP = 4;

    /**
     * The levels a story boss (a crypt's Revenant, a tower's Magister, the Sovereigns, the Hollow)
     * stands above its place, whatever its rank: its fight is its mechanics, not a wall of levels.
     * (An Archmage's +20 made each a level-15 player's death in seconds: see the balance pass spec.)
     */
    public static final int STORY_BOSS_BONUS_LEVELS = 10;

    private Progression() {
    }

    /** The levels a creature stands above its zone: a story boss's fixed bonus, or its rank's. */
    public static int bonusLevels(AttunementRank rank, boolean storyBoss) {
        return storyBoss ? STORY_BOSS_BONUS_LEVELS : rank == null ? 0 : rank.bonusLevels();
    }

    /** XP from {@code level} to the next; 0 at the cap. */
    public static int xpToNextLevel(int level) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return (int) Math.round(XP_BASE * Math.pow(XP_GROWTH, Math.max(1, level) - 1));
    }

    /** Same-level kills a level takes at {@code level}: 8 at 1, rising to 40 at 100. */
    public static double killsPerLevel(int level) {
        int l = Math.clamp(level, 1, MAX_LEVEL);
        return FIRST_KILLS_PER_LEVEL + (LAST_KILLS_PER_LEVEL - FIRST_KILLS_PER_LEVEL) * (l - 1) / (MAX_LEVEL - 1);
    }

    /** XP (absorbed mana) of an ordinary creature of {@code level} killed by a same-level player. */
    public static int creatureXp(int level) {
        int l = Math.clamp(level, 1, MAX_LEVEL);
        return Math.max(1, (int) Math.round(xpToNextLevel(Math.min(l, MAX_LEVEL - 1)) / killsPerLevel(l)));
    }

    /** Share of a kill's XP a player absorbs: 10% less per level below them (none at 10), 5% more per level above (up to +50%). */
    public static double xpGapFactor(int playerLevel, int creatureLevel) {
        int gap = creatureLevel - playerLevel;
        return gap >= 0 ? 1 + Math.min(0.5, 0.05 * gap) : Math.max(0, 1 + 0.1 * gap);
    }

    /** XP for a kill: the creature's mana, by its size and rank, by the level gap. */
    public static int killXp(int playerLevel, int creatureLevel, double sizeFactor, double rankMultiplier) {
        double factor = xpGapFactor(playerLevel, creatureLevel);
        if (factor <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(creatureXp(creatureLevel) * sizeFactor * rankMultiplier * factor));
    }

    /** How much mana a creature's body holds, by its base max health (a zombie's 20 = 1, from 0.1 to 5). */
    public static double sizeFactor(double baseMaxHealth) {
        return Math.clamp(baseMaxHealth / 20, 0.1, 5);
    }

    /** Damage multiplier for an attacker of {@code attackerLevel} hitting a target of {@code targetLevel}: x1.045 a level, gap capped at 40. */
    public static float damageLevelFactor(int attackerLevel, int targetLevel) {
        int gap = Math.clamp(attackerLevel - targetLevel, -MAX_DAMAGE_GAP, MAX_DAMAGE_GAP);
        return (float) Math.pow(DAMAGE_PER_LEVEL, gap);
    }

    /** A spell's cooldown at {@code spellLevel}, from its Lv 1 cooldown {@code baseTicks}. */
    public static int cooldownTicks(int baseTicks, int spellLevel) {
        return cooldownTicks(baseTicks, spellLevel, 1f);
    }

    /** A spell's cooldown at {@code spellLevel} (6% of the base shorter each), times the caster's Focus factor. */
    public static int cooldownTicks(int baseTicks, int spellLevel, float cooldownFactor) {
        int level = Math.max(1, spellLevel);
        return Math.round(baseTicks * (1f - COOLDOWN_CUT_PER_LEVEL * (level - 1)) * cooldownFactor);
    }

    /** Essence to refund one skill tree node or stat point at {@code level}: 1, plus 1 every 20 levels. */
    public static int refundCost(int level) {
        return 1 + Math.max(1, level) / 20;
    }

    /** Share of a spell's mastery bar one Essence fills: half at Lv 1, then 20% less each level. */
    public static float essenceBarFraction(int spellLevel) {
        return ESSENCE_FIRST_FILL * (float) Math.pow(ESSENCE_FALLOFF, Math.max(1, spellLevel) - 1);
    }

    /** Mastery one Essence gives at {@code spellLevel}, for a bar of {@code barSize}; at least 1. */
    public static int essenceMastery(int spellLevel, int barSize) {
        return Math.max(1, Math.round(barSize * essenceBarFraction(spellLevel)));
    }

    /** How many Essence fill the bar from {@code currentMastery}. */
    public static int essenceToFill(int spellLevel, int barSize, int currentMastery) {
        int missing = barSize - currentMastery;
        if (missing <= 0) {
            return 0;
        }
        int each = essenceMastery(spellLevel, barSize);
        return (missing + each - 1) / each;
    }

    /** Essence needed to condense the next bonus skill point, after {@code alreadyCondensed} of them. */
    public static int condenseCost(int alreadyCondensed) {
        return CONDENSE_BASE_COST + CONDENSE_COST_STEP * Math.max(0, alreadyCondensed);
    }
}
