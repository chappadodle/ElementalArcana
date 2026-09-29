package com.chappadodle.elementalarcana.api;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Which element a spell school is (fire, water, ice, wind). Schools added by other mods have none. */
public final class SchoolElements {

    private SchoolElements() {
    }

    @Nullable
    public static Element of(SpellSchool school) {
        return of(school.id());
    }

    @Nullable
    public static Element of(ResourceLocation schoolId) {
        try {
            return Element.valueOf(schoolId.getPath().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
