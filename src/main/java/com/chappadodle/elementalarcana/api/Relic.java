package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

/**
 * The relics (docs/superpowers/specs/2026-10-04-relics-design.md): one for each element, found in
 * its crypts, and the Revenant's Phylactery. Each adds STAT_BONUS points to one stat while borne;
 * its power lives in content/relic/Relics.
 */
public enum Relic {
    EMBER_HEART("ember_heart", Element.FIRE, Stat.POTENCY),
    TIDECALLERS_PEARL("tidecallers_pearl", Element.WATER, Stat.VITALITY),
    RIMEHEART_LOCKET("rimeheart_locket", Element.ICE, Stat.WARD),
    FEATHER_OF_THE_GALE("feather_of_the_gale", Element.WIND, Stat.FOCUS),
    STONEHEART_IDOL("stoneheart_idol", Element.EARTH, Stat.VITALITY),
    PRISM_OF_THE_DEEP("prism_of_the_deep", Element.CRYSTAL, Stat.INSIGHT),
    STORM_SIGIL("storm_sigil", Element.LIGHTNING, Stat.FOCUS),
    SUNSTONE("sunstone", Element.RADIANCE, Stat.POTENCY),
    REVENANTS_PHYLACTERY("revenants_phylactery", null, Stat.RESERVOIR);

    private final String id;
    @Nullable
    private final Element element;
    private final Stat stat;

    Relic(String id, @Nullable Element element, Stat stat) {
        this.id = id;
        this.element = element;
        this.stat = stat;
    }

    /** Its item's name, e.g. "ember_heart". */
    public String id() {
        return id;
    }

    /** The element whose crypts hold it, or null (the Phylactery comes from Revenants). */
    @Nullable
    public Element element() {
        return element;
    }

    public Stat stat() {
        return stat;
    }

    /** The relic of {@code element}'s crypts. */
    public static Relic of(Element element) {
        for (Relic relic : values()) {
            if (relic.element == element) {
                return relic;
            }
        }
        throw new IllegalArgumentException("no relic for " + element);
    }
}
