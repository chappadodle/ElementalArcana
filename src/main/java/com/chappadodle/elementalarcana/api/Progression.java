package com.chappadodle.elementalarcana.api;

/**
 * How getting better makes magic faster. Plain Java, no Minecraft types (unit tested). A spell's
 * own level shortens its cooldown; your Magic Level speeds up mana regeneration.
 */
public final class Progression {
    // Each spell level above 1 takes this share of the Lv 1 cooldown off (Lv 10 = 46%).
    private static final float COOLDOWN_CUT_PER_LEVEL = 0.06f;
    private static final float BASE_REGEN_PER_SECOND = 2.5f;
    private static final float REGEN_PER_SECOND_PER_LEVEL = 0.25f;

    private Progression() {
    }

    /** A spell's cooldown at {@code spellLevel}, from its Lv 1 cooldown {@code baseTicks}. */
    public static int cooldownTicks(int baseTicks, int spellLevel) {
        int level = Math.max(1, spellLevel);
        return Math.round(baseTicks * (1f - COOLDOWN_CUT_PER_LEVEL * (level - 1)));
    }

    /** Mana regenerated per second at {@code magicLevel}. */
    public static float regenPerSecond(int magicLevel) {
        return BASE_REGEN_PER_SECOND + REGEN_PER_SECOND_PER_LEVEL * (Math.max(1, magicLevel) - 1);
    }
}
