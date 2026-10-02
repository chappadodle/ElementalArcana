package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

/**
 * The elements. Plain data with no Minecraft types: which elements oppose each other, and how
 * hard a spell of one element hits a creature of another (see ElementalMatchups).
 */
public enum Element {
    FIRE(0xFF7A1F),
    WATER(0x3F9CFF),
    ICE(0x9EE6FF),
    WIND(0x8FE3C0),
    EARTH(0xB5895A);

    private static final float STRONG = 1.5f;
    private static final float RESISTED = 0.5f;

    private final int color;

    Element(int color) {
        this.color = color;
    }

    /** The element's signature color, as 0xRRGGBB. */
    public int color() {
        return color;
    }

    /** Whether this element is derived from another (Ice from Water) and so far rarer to awaken first. */
    public boolean derived() {
        return this == ICE;
    }

    /**
     * The element family this element belongs to: Ice belongs to Water's. Affinity (a stat) is per
     * family, so related elements are raised together.
     */
    public Element family() {
        return this == ICE ? WATER : this;
    }

    /** Fire is opposed to Water and to Ice ("Fire vs the cold"); every other pair is compatible. */
    public boolean opposes(Element other) {
        return this == FIRE && (other == WATER || other == ICE)
                || other == FIRE && (this == WATER || this == ICE);
    }

    /**
     * Damage multiplier for a spell of this element hitting a creature of {@code target}'s element
     * (null = a creature with no element). Water beats fire, fire beats ice, ice beats water, earth
     * grounds wind, water erodes earth, and every element resists itself.
     */
    public float multiplierAgainst(@Nullable Element target) {
        if (target == null) {
            return 1f;
        }
        if (target == this) {
            return RESISTED;
        }
        return switch (this) {
            case FIRE -> target == ICE ? STRONG : target == WATER ? RESISTED : 1f;
            case WATER -> target == FIRE || target == EARTH ? STRONG : 1f;
            case ICE -> target == WATER ? STRONG : target == FIRE ? RESISTED : 1f;
            case WIND -> 1f;
            case EARTH -> target == WIND ? STRONG : 1f;
        };
    }
}
