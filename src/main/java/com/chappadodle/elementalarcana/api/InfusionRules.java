package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The numbers of Arcane Infusion (docs/superpowers/specs/2026-10-04-arcane-infusion-design.md):
 * the Essence an infusion takes, what infused armour wards off, and the infused hits' extras.
 * Plain Java, unit tested.
 */
public final class InfusionRules {
    /** Essence of one element poured into the altar's basin to infuse its item. */
    public static final int ESSENCE_NEEDED = 8;
    /** Each infused armour piece takes this much off its element family's harm. */
    public static final float WARD_PER_PIECE = 0.10f;
    public static final int SET_PIECES = 4;
    /** A lightning-infused hit sparks to one more foe this near, for this share of the hit. */
    public static final double SPARK_RANGE = 4;
    public static final float SPARK_SHARE = 1f / 3f;
    /** A radiance-infused hit on the undead. */
    public static final float RADIANCE_UNDEAD = 1.5f;
    /** A crystal-infused hit now and then shatters for more. */
    public static final float CRYSTAL_SHATTER_CHANCE = 0.25f;
    public static final float CRYSTAL_SHATTER = 1.5f;
    /** The Radiance set's rest and the Crystal set's ward. */
    public static final int RADIANCE_HEAL_TICKS = 80;
    public static final int CRYSTAL_WARD_TICKS = 600;

    private InfusionRules() {
    }

    /** What's left of harm of {@code harm}'s family after {@code pieces} armour pieces infused with {@code infusions}. */
    public static float wardFactor(List<Element> infusions, Element harm) {
        int pieces = 0;
        for (Element infusion : infusions) {
            if (infusion != null && infusion.family() == harm.family()) {
                pieces++;
            }
        }
        return 1f - WARD_PER_PIECE * Math.min(pieces, SET_PIECES);
    }

    /** The element all four armour pieces are infused with, or null if they aren't four of one. */
    @Nullable
    public static Element setElement(List<Element> infusions) {
        if (infusions.size() < SET_PIECES) {
            return null;
        }
        Element first = infusions.get(0);
        if (first == null) {
            return null;
        }
        for (Element infusion : infusions) {
            if (infusion != first) {
                return null;
            }
        }
        return first;
    }

    /** Whether Essence of {@code essence} can be poured into a basin holding {@code held} (null: empty). */
    public static boolean canPour(@Nullable Element held, int charge, Element essence) {
        return charge < ESSENCE_NEEDED && (held == null || charge == 0 || held == essence);
    }
}
