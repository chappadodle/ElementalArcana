package com.chappadodle.elementalarcana.api;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.function.Supplier;

/**
 * An element (fire, water, ...). Carries the look and sound shared by all of its spells.
 * A new element is just one more registry entry.
 */
public class SpellSchool {
    private final int color;
    private final Supplier<SoundEvent> castSound;

    public SpellSchool(int color, Supplier<SoundEvent> castSound) {
        this.color = color;
        this.castSound = castSound;
    }

    /** RGB, used for the scroll tint, HUD accents and tooltips. */
    public int color() {
        return color;
    }

    public SoundEvent castSound() {
        return castSound.get();
    }

    public ResourceLocation id() {
        return SpellRegistries.SCHOOLS.getKey(this);
    }

    public Component displayName() {
        return Component.translatable(Util.makeDescriptionId("school", id()));
    }

    /** Flavor line shown on the awakening screen: lang key {@code school.<namespace>.<path>.desc}. */
    public Component description() {
        return Component.translatable(Util.makeDescriptionId("school", id()) + ".desc");
    }
}
