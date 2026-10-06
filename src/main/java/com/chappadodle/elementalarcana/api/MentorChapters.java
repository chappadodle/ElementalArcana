package com.chappadodle.elementalarcana.api;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The chapters of Caelith's tale (docs/superpowers/specs/2026-10-05-voice-in-the-stone-design.md):
 * each one's task (an advancement the mod already has; none for the first, which you finish by
 * listening) and what it's worth. The words are in the language file, written by
 * tools/gen_mentor.py under the same ids.
 */
public final class MentorChapters {
    /** A reward's item id meaning "Essence of your first element". */
    public static final String OWN_ESSENCE = "essence";

    /** An item and how many of it. */
    public record Reward(String item, int count) {
    }

    /** A chapter: its id, its task (an advancement id, or null), its XP in levels' worth, its items. */
    public record Chapter(String id, @Nullable String advancement, double xpLevels, List<Reward> rewards) {
    }

    public static final List<Chapter> CHAPTERS = List.of(
            new Chapter("voice", null, 0, List.of(new Reward("elementalarcana:seekers_compass", 1))),
            new Chapter("shrine", task("places_of_power"), 1, List.of(new Reward(OWN_ESSENCE, 4))),
            new Chapter("wisps", task("wisp_hunter"), 1, List.of(new Reward("elementalarcana:binding_charm", 1))),
            new Chapter("arcanist", task("the_arcanist"), 1, List.of(new Reward("minecraft:emerald", 6))),
            new Chapter("crypt", task("crypt"), 1, List.of()),
            new Chapter("revenant", task("laid_to_rest"), 2, List.of(new Reward("elementalarcana:charm_pouch", 1))),
            new Chapter("tower", task("tower"), 1, List.of()),
            new Chapter("magister", task("magister"), 2, List.of(new Reward("elementalarcana:wishing_star", 1))),
            new Chapter("hollowed", task("the_hollowed"), 1, List.of(new Reward("elementalarcana:hungerward_charm", 1))),
            new Chapter("sanctum", task("sanctum"), 1, List.of()),
            new Chapter("sovereign", task("sovereign"), 2, List.of(new Reward("minecraft:golden_apple", 2))),
            new Chapter("hearts", task("four_hearts"), 2, List.of()),
            new Chapter("key", task("prime_key"), 2, List.of(new Reward("minecraft:totem_of_undying", 1))),
            new Chapter("hollow", task("into_the_hollow"), 1, List.of()),
            new Chapter("bound", task("bound_again"), 3, List.of(new Reward("minecraft:enchanted_golden_apple", 1))));

    private MentorChapters() {
    }

    private static String task(String advancement) {
        return "elementalarcana:arcana/" + advancement;
    }

    /** The chapter at {@code index}, or null once the tale is told. */
    @Nullable
    public static Chapter at(int index) {
        return index >= 0 && index < CHAPTERS.size() ? CHAPTERS.get(index) : null;
    }

    /** Whether the tale is over at {@code index}. */
    public static boolean finished(int index) {
        return index >= CHAPTERS.size();
    }

    /** A chapter's XP for a mage of {@code level}: its levels' worth of what their next level takes. */
    public static int xp(Chapter chapter, int level) {
        return (int) Math.round(chapter.xpLevels() * Progression.xpToNextLevel(level));
    }
}
