package com.chappadodle.elementalarcana.api;

/**
 * How getting better makes magic faster. Plain Java, no Minecraft types (unit tested). A spell's
 * own level shortens its cooldown; your Magic Level speeds up mana regeneration and shortens every
 * cooldown a little more; Elemental Essence fills mastery (less at higher levels) and condenses
 * into skill points (costing more each time).
 */
public final class Progression {
    // Each spell level above 1 takes this share of the Lv 1 cooldown off (Lv 10 = 46%).
    private static final float COOLDOWN_CUT_PER_LEVEL = 0.06f;
    // Each Magic Level above 1 takes this share off every cooldown (Magic Lv 30 = 71%).
    private static final float COOLDOWN_CUT_PER_MAGIC_LEVEL = 0.01f;
    private static final float BASE_REGEN_PER_SECOND = 2.5f;
    private static final float REGEN_PER_SECOND_PER_LEVEL = 0.25f;
    private static final float ESSENCE_FIRST_FILL = 0.5f;
    private static final float ESSENCE_FALLOFF = 0.8f;
    private static final int CONDENSE_BASE_COST = 8;
    private static final int CONDENSE_COST_STEP = 4;

    private Progression() {
    }

    /** A spell's cooldown at {@code spellLevel}, from its Lv 1 cooldown {@code baseTicks} (at Magic Level 1). */
    public static int cooldownTicks(int baseTicks, int spellLevel) {
        return cooldownTicks(baseTicks, spellLevel, 1);
    }

    /**
     * A spell's cooldown at {@code spellLevel} for a caster at {@code magicLevel}: 6% of the base
     * shorter per spell level and, on top, 1% shorter per Magic Level.
     */
    public static int cooldownTicks(int baseTicks, int spellLevel, int magicLevel) {
        int level = Math.max(1, spellLevel);
        int magic = Math.max(1, magicLevel);
        return Math.round(baseTicks * (1f - COOLDOWN_CUT_PER_LEVEL * (level - 1)) * (1f - COOLDOWN_CUT_PER_MAGIC_LEVEL * (magic - 1)));
    }

    /** Mana regenerated per second at {@code magicLevel}. */
    public static float regenPerSecond(int magicLevel) {
        return BASE_REGEN_PER_SECOND + REGEN_PER_SECOND_PER_LEVEL * (Math.max(1, magicLevel) - 1);
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
