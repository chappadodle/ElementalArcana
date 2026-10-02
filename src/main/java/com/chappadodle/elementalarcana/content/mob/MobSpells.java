package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.Element;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The spells each element's Attuned creatures know, lowest rank first. */
public final class MobSpells {
    /** Mob spell power: 60% of what a player's Lv 1 spell does. */
    public static final float POWER = 0.6f;

    private static final Map<Element, List<MobSpell>> BY_ELEMENT = new EnumMap<>(Element.class);

    static {
        BY_ELEMENT.put(Element.FIRE, FireMobSpells.ALL);
        BY_ELEMENT.put(Element.WATER, WaterMobSpells.ALL);
        BY_ELEMENT.put(Element.ICE, IceMobSpells.ALL);
        BY_ELEMENT.put(Element.WIND, WindMobSpells.ALL);
        BY_ELEMENT.put(Element.EARTH, EarthMobSpells.ALL);
    }

    private MobSpells() {
    }

    public static List<MobSpell> of(Element element) {
        return BY_ELEMENT.get(element);
    }
}
