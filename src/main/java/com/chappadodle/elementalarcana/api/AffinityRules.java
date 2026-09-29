package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Rules for a player's affinities (awakened elements). Plain Java, no Minecraft types (unit
 * tested). Opposed elements (Fire vs Water, Fire vs Ice) can't be awakened together until Magic
 * Level 30, and a player takes less spell damage of the elements they've awakened.
 */
public final class AffinityRules {
    public static final int OPPOSITES_UNLOCK_LEVEL = 30;
    private static final float OWN_ELEMENT_DAMAGE_TAKEN = 0.75f;

    private AffinityRules() {
    }

    /**
     * The element in {@code owned} that keeps {@code candidate} from being awakened (it opposes it,
     * and the player is below Magic Level 30), or null if nothing does.
     */
    @Nullable
    public static Element blockingOpposite(Collection<Element> owned, Element candidate, int magicLevel) {
        if (magicLevel >= OPPOSITES_UNLOCK_LEVEL) {
            return null;
        }
        for (Element element : owned) {
            if (candidate.opposes(element)) {
                return element;
            }
        }
        return null;
    }

    /** Damage multiplier for a player hit by a spell of {@code spell}: 25% less for their own elements. */
    public static float spellDamageTaken(Collection<Element> owned, Element spell) {
        return owned.contains(spell) ? OWN_ELEMENT_DAMAGE_TAKEN : 1f;
    }
}
