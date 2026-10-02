package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.api.Element;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Which element a shrine belongs to; also the Shrine Core's block-state property. */
public enum ShrineKind implements StringRepresentable {
    FIRE(Element.FIRE),
    WATER(Element.WATER),
    ICE(Element.ICE),
    WIND(Element.WIND),
    EARTH(Element.EARTH);

    public static final Codec<ShrineKind> CODEC = StringRepresentable.fromEnum(ShrineKind::values);

    private final Element element;

    ShrineKind(Element element) {
        this.element = element;
    }

    public Element element() {
        return element;
    }

    @Override
    public String getSerializedName() {
        return element.name().toLowerCase(java.util.Locale.ROOT);
    }
}
