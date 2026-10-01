package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Rules for a player's affinities (awakened elements). Plain Java, no Minecraft types (unit
 * tested). Opposed elements (Fire vs Water, Fire vs Ice) can't be awakened together until level
 * 50, and a player takes less damage from the elements they've awakened.
 */
public final class AffinityRules {
    public static final int OPPOSITES_UNLOCK_LEVEL = 50;
    private static final float OWN_ELEMENT_DAMAGE_TAKEN = 0.75f;

    private AffinityRules() {
    }

    /**
     * The element in {@code owned} that keeps {@code candidate} from being awakened (it opposes it,
     * and the player is below level 50), or null if nothing does.
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

    /**
     * Damage multiplier for a player hurt by {@code element} (its spells, or its everyday damage:
     * burning, freezing, drowning, falling): 25% less for the elements they've awakened.
     */
    public static float damageTaken(Collection<Element> owned, Element element) {
        return owned.contains(element) ? OWN_ELEMENT_DAMAGE_TAKEN : 1f;
    }
}
