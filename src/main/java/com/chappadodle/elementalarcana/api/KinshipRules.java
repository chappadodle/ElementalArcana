package com.chappadodle.elementalarcana.api;

/**
 * What Kinship with an element does (see the Arcane Flora spec): a tonic brewed from the element's
 * herb makes your spells of the element stronger and its magic harm you less, by the same share.
 * Plain Java, unit tested.
 */
public final class KinshipRules {
    /** The share a tonic gives, and how much more each level of strength adds (a strong tonic is level 2). */
    public static final float SHARE = 0.2f;
    public static final float SHARE_PER_LEVEL = 0.15f;
    /** However strong (an /effect can go to 255), Kinship never more than this. */
    public static final float MAX_SHARE = 0.6f;

    private KinshipRules() {
    }

    /** The share for an effect amplifier (0 for a plain tonic, 1 for a strong one). */
    public static float share(int amplifier) {
        return Math.min(MAX_SHARE, SHARE + SHARE_PER_LEVEL * Math.max(0, amplifier));
    }

    /** The multiplier on the power of the element's spells. */
    public static float powerFactor(int amplifier) {
        return 1 + share(amplifier);
    }

    /** The multiplier on harm by the element's magic. */
    public static float wardFactor(int amplifier) {
        return 1 - share(amplifier);
    }
}
