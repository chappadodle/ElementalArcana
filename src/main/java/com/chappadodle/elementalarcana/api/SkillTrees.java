package com.chappadodle.elementalarcana.api;

/**
 * The skill tree in use: loaded from data on the server (SkillTreeLoader) and synced to each
 * client. The version goes up every time it's replaced, so cached results can tell they're stale.
 */
public final class SkillTrees {
    private static volatile SkillTree current = SkillTree.EMPTY;
    private static volatile int version;

    private SkillTrees() {
    }

    public static SkillTree current() {
        return current;
    }

    public static int version() {
        return version;
    }

    public static void set(SkillTree tree) {
        current = tree;
        version++;
    }
}
