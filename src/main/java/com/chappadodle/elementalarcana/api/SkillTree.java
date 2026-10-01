package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * The skill tree (see docs/superpowers/specs/2026-10-01-skill-tree-design.md): a graph of nodes
 * and the rules for taking and refunding them. Plain Java with string ids (unit tested); the
 * loader builds it from data files and MagicData asks it what a player's nodes give.
 * <ul>
 * <li>START: one per element, held for free by players of that element (it gives the element's
 * starter spell); a 1-point spell node for players of the same family.</li>
 * <li>SMALL: +amount points to one stat key ("potency", "affinity/water").</li>
 * <li>SPELL: unlocks a spell.</li>
 * <li>UPGRADE / FORK: the next level of a spell; a fork also picks a branch and locks the other
 * forks of that level. Both need the spell's mastery bar full.</li>
 * </ul>
 */
public final class SkillTree {
    public static final SkillTree EMPTY = new SkillTree(List.of(), List.of());

    public enum Type { START, SMALL, SPELL, UPGRADE, FORK }

    /**
     * A node. {@code element} is its region (null in the shared core); {@code stat}/{@code amount}
     * for SMALL; {@code spell} for START, SPELL, UPGRADE and FORK; {@code spellLevel} for UPGRADE
     * and FORK; {@code branch} for FORK; {@code requiresStat}/{@code requiresMin} an optional stat
     * requirement.
     */
    public record Node(String id, Type type, int x, int y, @Nullable String element,
                       @Nullable String stat, int amount, @Nullable String spell, int spellLevel,
                       @Nullable String branch, @Nullable String requiresStat, int requiresMin) {
    }

    /** What the rules need to know about a player. */
    public interface Player {
        int freePoints();

        /** Whether the player holds exactly this element (owns its start). */
        boolean holdsElement(String element);

        /** Whether the player holds any element of this element's family. */
        boolean holdsFamily(String element);

        /** Stat points spent on {@code key}, without tree bonuses. */
        int statPoints(String key);

        /** Whether {@code spell}'s mastery bar toward the level after {@code currentLevel} is full. */
        boolean masteryFull(String spell, int currentLevel);
    }

    /** Why a node can or can't be taken or refunded. */
    public enum Check { OK, HELD, NO_POINTS, NOT_LINKED, SEALED, REQUIREMENT, MASTERY, FORK_TAKEN, START, DISCONNECTS, NEEDED, UNKNOWN }

    /**
     * What a set of held nodes gives: stat bonuses by key, castable spells, each castable spell's
     * level (1 plus its upgrades and forks) and its chosen branches by level.
     */
    public record Grants(Map<String, Integer> stats, Map<String, Integer> spellLevels,
                         Map<String, Map<Integer, String>> branches, Set<String> spells) {
        public int stat(String key) {
            return stats.getOrDefault(key, 0);
        }

        public int spellLevel(String spell) {
            return spellLevels.getOrDefault(spell, 1);
        }
    }

    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, Set<String>> links = new HashMap<>();
    private final List<List<String>> linkPairs = new ArrayList<>();

    /** Links are undirected pairs of ids; links to unknown nodes are dropped. */
    public SkillTree(Collection<Node> nodes, Collection<List<String>> links) {
        for (Node node : nodes) {
            this.nodes.put(node.id(), node);
            this.links.put(node.id(), new HashSet<>());
        }
        for (List<String> pair : links) {
            String a = pair.get(0);
            String b = pair.get(1);
            if (!a.equals(b) && this.nodes.containsKey(a) && this.nodes.containsKey(b) && this.links.get(a).add(b)) {
                this.links.get(b).add(a);
                linkPairs.add(List.of(a, b));
            }
        }
    }

    @Nullable
    public Node node(String id) {
        return nodes.get(id);
    }

    public Collection<Node> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Set<String> links(String id) {
        return Collections.unmodifiableSet(links.getOrDefault(id, Set.of()));
    }

    public List<List<String>> linkPairs() {
        return Collections.unmodifiableList(linkPairs);
    }

    /** The starts the player holds for free: those of their own elements. */
    public Set<String> heldStarts(Player player) {
        Set<String> starts = new HashSet<>();
        for (Node node : nodes.values()) {
            if (node.type() == Type.START && node.element() != null && player.holdsElement(node.element())) {
                starts.add(node.id());
            }
        }
        return starts;
    }

    /** Every node the player holds: the ones they took, plus their free starts. */
    public Set<String> held(Set<String> taken, Player player) {
        Set<String> held = new HashSet<>(heldStarts(player));
        for (String id : taken) {
            if (nodes.containsKey(id)) {
                held.add(id);
            }
        }
        return held;
    }

    public Grants grants(Set<String> held) {
        Map<String, Integer> stats = new HashMap<>();
        Set<String> spells = new HashSet<>();
        Map<String, Integer> levels = new HashMap<>();
        Map<String, Map<Integer, String>> branches = new HashMap<>();
        for (String id : held) {
            Node node = nodes.get(id);
            if (node == null) {
                continue;
            }
            switch (node.type()) {
                case SMALL -> {
                    if (node.stat() != null) {
                        stats.merge(node.stat(), node.amount(), Integer::sum);
                    }
                }
                case START, SPELL -> {
                    if (node.spell() != null) {
                        spells.add(node.spell());
                    }
                }
                default -> {
                }
            }
        }
        for (String id : held) {
            Node node = nodes.get(id);
            if (node != null && (node.type() == Type.UPGRADE || node.type() == Type.FORK) && spells.contains(node.spell())) {
                levels.merge(node.spell(), 1, Integer::sum);
                if (node.type() == Type.FORK && node.branch() != null) {
                    branches.computeIfAbsent(node.spell(), spell -> new HashMap<>()).put(node.spellLevel(), node.branch());
                }
            }
        }
        levels.replaceAll((spell, upgrades) -> upgrades + 1);
        return new Grants(stats, levels, branches, spells);
    }

    /** Whether the player can take node {@code id} now. */
    public Check canTake(Set<String> taken, String id, Player player) {
        Node node = nodes.get(id);
        if (node == null) {
            return Check.UNKNOWN;
        }
        Set<String> held = held(taken, player);
        if (held.contains(id)) {
            return node.type() == Type.START && !taken.contains(id) ? Check.START : Check.HELD;
        }
        if (player.freePoints() <= 0) {
            return Check.NO_POINTS;
        }
        if (links(id).stream().noneMatch(held::contains)) {
            return Check.NOT_LINKED;
        }
        if (node.element() != null && !player.holdsFamily(node.element())) {
            return Check.SEALED;
        }
        Grants grants = grants(held);
        if (node.requiresStat() != null && player.statPoints(node.requiresStat()) + grants.stat(node.requiresStat()) < node.requiresMin()) {
            return Check.REQUIREMENT;
        }
        if (node.type() == Type.FORK) {
            for (String other : held) {
                Node fork = nodes.get(other);
                if (fork != null && fork.type() == Type.FORK && fork.spellLevel() == node.spellLevel() && fork.spell().equals(node.spell())) {
                    return Check.FORK_TAKEN;
                }
            }
        }
        if ((node.type() == Type.UPGRADE || node.type() == Type.FORK) && !player.masteryFull(node.spell(), grants.spellLevel(node.spell()))) {
            return Check.MASTERY;
        }
        return Check.OK;
    }

    /** Whether the player can give node {@code id} back now (the Essence cost is checked elsewhere). */
    public Check canRefund(Set<String> taken, String id, Player player) {
        Node node = nodes.get(id);
        if (node == null) {
            return Check.UNKNOWN;
        }
        if (!taken.contains(id)) {
            return node.type() == Type.START && player.holdsElement(node.element()) ? Check.START : Check.UNKNOWN;
        }
        Set<String> rest = held(taken, player);
        rest.remove(id);
        if (!connected(rest, heldStarts(player))) {
            return Check.DISCONNECTS;
        }
        if (!requirementsMet(rest, player::statPoints)) {
            return Check.NEEDED;
        }
        return Check.OK;
    }

    /** Whether every held node with a stat requirement still meets it. */
    public boolean requirementsMet(Set<String> held, ToIntFunction<String> statPoints) {
        Grants grants = grants(held);
        for (String id : held) {
            Node node = nodes.get(id);
            if (node != null && node.requiresStat() != null
                    && statPoints.applyAsInt(node.requiresStat()) + grants.stat(node.requiresStat()) < node.requiresMin()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether every node in {@code held} can be reached through held nodes from one of
     * {@code roots} (the player's own starts; a start bought by a family member isn't a root).
     */
    private boolean connected(Set<String> held, Set<String> roots) {
        Set<String> reached = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (String id : roots) {
            if (held.contains(id)) {
                reached.add(id);
                queue.add(id);
            }
        }
        while (!queue.isEmpty()) {
            for (String next : links(queue.poll())) {
                if (held.contains(next) && reached.add(next)) {
                    queue.add(next);
                }
            }
        }
        return reached.containsAll(held);
    }
}
