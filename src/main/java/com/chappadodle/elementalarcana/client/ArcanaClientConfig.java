package com.chappadodle.elementalarcana.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only settings (elementalarcana-client.toml, also editable from the Mods screen). */
public final class ArcanaClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SCREEN_EFFECTS = BUILDER
            .comment("Camera shake and screen flashes from big spells (Meteor impacts, Sunfire).")
            .translation("elementalarcana.configuration.screenEffects")
            .define("screenEffects", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ArcanaClientConfig() {
    }
}
