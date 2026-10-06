package com.chappadodle.elementalarcana.api;

import java.util.Locale;

/**
 * The herbs of the elements (docs/superpowers/specs/2026-10-06-arcane-flora-design.md): one for
 * each element, growing where it's strong, glowing a little, and brewed into a tonic that gives
 * Kinship with it. Plain data, unit tested.
 */
public enum Herb {
    EMBERBLOOM(Element.FIRE, "embers", 7, Bloom.ALWAYS),
    MOONLILY(Element.WATER, "tides", 8, Bloom.BY_NIGHT),
    SKYPLUME(Element.WIND, "gales", 0, Bloom.ALWAYS),
    DEEPCAP(Element.EARTH, "stone", 6, Bloom.ALWAYS),
    FROSTCAP(Element.ICE, "frost", 3, Bloom.ALWAYS),
    PRISMLEAF(Element.CRYSTAL, "prisms", 5, Bloom.ALWAYS),
    STORMTHISTLE(Element.LIGHTNING, "storms", 4, Bloom.ALWAYS),
    SUNPETAL(Element.RADIANCE, "dawn", 6, Bloom.BY_DAY);

    /** When a herb's flower is open: always, or only by day or by night (and then only open it glows). */
    public enum Bloom {
        ALWAYS, BY_DAY, BY_NIGHT
    }

    private final Element element;
    private final String tonic;
    private final int light;
    private final Bloom bloom;

    Herb(Element element, String tonic, int light, Bloom bloom) {
        this.element = element;
        this.tonic = tonic;
        this.light = light;
        this.bloom = bloom;
    }

    public Element element() {
        return element;
    }

    /** The herb's id, "emberbloom". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Its tonic's id, "tonic_of_embers" (the long and strong ones add "long_" and "strong_"). */
    public String tonicId() {
        return "tonic_of_" + tonic;
    }

    public Bloom bloom() {
        return bloom;
    }

    /** Whether its flower is open, by day or by night. */
    public boolean openAt(boolean day) {
        return switch (bloom) {
            case ALWAYS -> true;
            case BY_DAY -> day;
            case BY_NIGHT -> !day;
        };
    }

    /** The light it gives, open or shut. */
    public int light(boolean open) {
        return open ? light : 0;
    }

    /** The herb of an element. */
    public static Herb of(Element element) {
        for (Herb herb : values()) {
            if (herb.element == element) {
                return herb;
            }
        }
        throw new IllegalArgumentException("No herb for " + element);
    }

    public static Herb byId(String id) {
        return valueOf(id.toUpperCase(Locale.ROOT));
    }
}
