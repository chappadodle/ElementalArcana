package com.chappadodle.elementalarcana.content.sanctum;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** A sanctum's Seal: sealed (its Sovereign sleeps), awake (it fights) or restored (it fell). */
public enum SealState implements StringRepresentable {
    SEALED, AWAKE, RESTORED;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
