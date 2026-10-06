package com.chappadodle.elementalarcana.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The server's settings (docs/superpowers/specs/2026-10-05-server-config-design.md), in
 * config/elementalarcana-server.toml (a world's serverconfig/ folder can override them) for whoever
 * runs it: how often the world's events
 * come and creatures turn up, as multiples of their usual chances (0 turns one off), whether mana
 * tides rise, how quickly magic wakes on its own, and whether the mentor's stone is given. The
 * commands that call these things up (/arcana rift, /arcana starfall, /arcana wild...) ignore them.
 */
public final class ArcanaServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("The world's events").push("events");
    }

    public static final ModConfigSpec.DoubleValue RIFTS = BUILDER
            .comment("How often elemental rifts tear open near players, as a multiple of the usual chance (0: never).")
            .translation("elementalarcana.configuration.rifts")
            .defineInRange("rifts", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue STARFALL = BUILDER
            .comment("How often a star falls near an awakened player at night, as a multiple of the usual one night in three (0: never; 3: every night).")
            .translation("elementalarcana.configuration.starfall")
            .defineInRange("starfall", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.BooleanValue MANA_TIDES = BUILDER
            .comment("Whether a mana tide rises every few nights (mana flows faster; awakening, Attunement and wisps come more often; creatures are stronger).")
            .translation("elementalarcana.configuration.manaTides")
            .define("manaTides", true);

    static {
        BUILDER.pop().comment("Creatures that turn up on their own").push("creatures");
    }

    public static final ModConfigSpec.DoubleValue WISPS = BUILDER
            .comment("How often wisps appear near players, as a multiple of the usual chance (0: never).")
            .translation("elementalarcana.configuration.wisps")
            .defineInRange("wisps", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue WILD_CREATURES = BUILDER
            .comment("How often the Creatures of the Wild (treants, wraiths, salamanders, harpies, crawlers, lurkers) appear, as a multiple of the usual chance (0: never).")
            .translation("elementalarcana.configuration.wildCreatures")
            .defineInRange("wildCreatures", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue DRAKES = BUILDER
            .comment("How often a wild Elemental Drake flies in, as a multiple of the usual chance (0: never; their nests still have theirs).")
            .translation("elementalarcana.configuration.drakes")
            .defineInRange("drakes", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue GOLEMS = BUILDER
            .comment("How often Elemental Golems rise, as a multiple of the usual chance (0: never).")
            .translation("elementalarcana.configuration.golems")
            .defineInRange("golems", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue WANDERING_MAGE = BUILDER
            .comment("How often a Wandering Mage comes to an awakened player of a morning, as a multiple of the usual one morning in three (0: never; 3: every morning, if none is about).")
            .translation("elementalarcana.configuration.wanderingMage")
            .defineInRange("wanderingMage", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue HOLLOWED_PATROLS = BUILDER
            .comment("How often the Hollowed come hunting an awakened player of level 15 or more at dusk, as a multiple of the usual chance (8% at level 15, up to 20%; 0: never). Their camps are worldgen.")
            .translation("elementalarcana.configuration.hollowedPatrols")
            .defineInRange("hollowedPatrols", 1.0, 0.0, 5.0);

    static {
        BUILDER.pop().comment("Magic").push("magic");
    }

    public static final ModConfigSpec.DoubleValue NATURAL_AWAKENING = BUILDER
            .comment("How quickly magic wakes in a player on its own, day by day, as a multiple of the usual chance (0: only a brush with an element, a Catalyst or a command wakes it).")
            .translation("elementalarcana.configuration.naturalAwakening")
            .defineInRange("naturalAwakening", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.BooleanValue SENDING_STONE = BUILDER
            .comment("Whether awakened players are given the Sending Stone, through which Caelith leads them (the mod's questline).")
            .translation("elementalarcana.configuration.sendingStone")
            .define("sendingStone", true);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ArcanaServerConfig() {
    }
}
