package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.api.SkillTree.Check;
import com.chappadodle.elementalarcana.api.SkillTree.Node;
import com.chappadodle.elementalarcana.api.SkillTree.Type;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreeTest {
    private static final String FIREBALL = "elementalarcana:fireball";
    private static final String ICICLE = "elementalarcana:icicle";

    /**
     * fire_start(fireball) - fb2 - {fb3a, fb3b} - fb4 (linked to both forks)
     * fire_start - core1(+2 potency) - core2(+2 water affinity) - ice_gate(ice, +2 ward, needs water affinity 4) - ice_start(icicle)
     * fire_start - core3(+2 water affinity)
     */
    private static final SkillTree TREE = new SkillTree(List.of(
            node("fire_start", Type.START, "fire", null, 0, FIREBALL, 0, null, null, 0),
            node("fb2", Type.UPGRADE, "fire", null, 0, FIREBALL, 2, null, null, 0),
            node("fb3a", Type.FORK, "fire", null, 0, FIREBALL, 3, "a", null, 0),
            node("fb3b", Type.FORK, "fire", null, 0, FIREBALL, 3, "b", null, 0),
            node("fb4", Type.UPGRADE, "fire", null, 0, FIREBALL, 4, null, null, 0),
            node("core1", Type.SMALL, null, "potency", 2, null, 0, null, null, 0),
            node("core2", Type.SMALL, null, "affinity/water", 2, null, 0, null, null, 0),
            node("core3", Type.SMALL, null, "affinity/water", 2, null, 0, null, null, 0),
            node("ice_gate", Type.SMALL, "ice", "ward", 2, null, 0, null, "affinity/water", 4),
            node("ice_start", Type.START, "ice", null, 0, ICICLE, 0, null, null, 0)
    ), List.of(
            List.of("fire_start", "fb2"), List.of("fb2", "fb3a"), List.of("fb2", "fb3b"),
            List.of("fb3a", "fb4"), List.of("fb3b", "fb4"),
            List.of("fire_start", "core1"), List.of("core1", "core2"), List.of("core2", "ice_gate"),
            List.of("ice_gate", "ice_start"), List.of("fire_start", "core3")));

    private static Node node(String id, Type type, String element, String stat, int amount, String spell, int level,
                             String branch, String requires, int min) {
        return new Node(id, type, 0, 0, element, stat, amount, spell, level, branch, requires, min);
    }

    /** A test player: their exact elements, free points, spent stat points and whether mastery is full. */
    private record TestPlayer(Set<String> elements, int freePoints, Map<String, Integer> stats, boolean mastery) implements SkillTree.Player {
        @Override
        public boolean holdsElement(String element) {
            return elements.contains(element);
        }

        @Override
        public boolean holdsFamily(String element) {
            Element family = Element.valueOf(element.toUpperCase(Locale.ROOT)).family();
            return elements.stream().anyMatch(held -> Element.valueOf(held.toUpperCase(Locale.ROOT)).family() == family);
        }

        @Override
        public int statPoints(String key) {
            return stats.getOrDefault(key, 0);
        }

        @Override
        public boolean masteryFull(String spell, int currentLevel) {
            return mastery;
        }
    }

    private static TestPlayer fire(int points, boolean mastery) {
        return new TestPlayer(Set.of("fire"), points, Map.of(), mastery);
    }

    private static TestPlayer fireAndWater(int waterAffinity) {
        return new TestPlayer(Set.of("fire", "water"), 5, Map.of("affinity/water", waterAffinity), true);
    }

    @Test
    void startsAreHeldForTheirElement() {
        Set<String> held = TREE.held(Set.of(), fire(0, false));
        assertEquals(Set.of("fire_start"), held);
        SkillTree.Grants grants = TREE.grants(held);
        assertEquals(Set.of(FIREBALL), grants.spells());
        assertEquals(1, grants.spellLevel(FIREBALL));
    }

    @Test
    void takingNeedsALinkAPointAndAFamily() {
        assertEquals(Check.NOT_LINKED, TREE.canTake(Set.of(), "core2", fire(1, false)));
        assertEquals(Check.OK, TREE.canTake(Set.of(), "core1", fire(1, false)));
        assertEquals(Check.NO_POINTS, TREE.canTake(Set.of(), "core1", fire(0, false)));
        assertEquals(Check.SEALED, TREE.canTake(Set.of("core1", "core2"), "ice_gate", fire(1, false)));
        assertEquals(Check.START, TREE.canTake(Set.of(), "fire_start", fire(1, false)));
        assertEquals(Check.UNKNOWN, TREE.canTake(Set.of(), "nope", fire(1, false)));
    }

    @Test
    void upgradesNeedFullMasteryAndCountLevels() {
        assertEquals(Check.MASTERY, TREE.canTake(Set.of(), "fb2", fire(1, false)));
        assertEquals(Check.OK, TREE.canTake(Set.of(), "fb2", fire(1, true)));
        SkillTree.Grants grants = TREE.grants(TREE.held(Set.of("fb2", "fb3a"), fire(0, false)));
        assertEquals(3, grants.spellLevel(FIREBALL));
        assertEquals(Map.of(3, "a"), grants.branches().get(FIREBALL));
    }

    @Test
    void forksLockEachOther() {
        assertEquals(Check.FORK_TAKEN, TREE.canTake(Set.of("fb2", "fb3a"), "fb3b", fire(1, true)));
        assertEquals(Check.OK, TREE.canTake(Set.of("fb2", "fb3a"), "fb4", fire(1, true)));
    }

    @Test
    void requirementsUseTreeBonusesToo() {
        assertEquals(Check.OK, TREE.canTake(Set.of("core1", "core2"), "ice_gate", fireAndWater(2)));
        assertEquals(Check.REQUIREMENT, TREE.canTake(Set.of("core1", "core2"), "ice_gate", fireAndWater(1)));
    }

    @Test
    void refundsKeepTheTreeConnected() {
        assertEquals(Check.DISCONNECTS, TREE.canRefund(Set.of("core1", "core2"), "core1", fire(0, false)));
        assertEquals(Check.OK, TREE.canRefund(Set.of("core1", "core2"), "core2", fire(0, false)));
        assertEquals(Check.START, TREE.canRefund(Set.of(), "fire_start", fire(0, false)));
        assertEquals(Check.UNKNOWN, TREE.canRefund(Set.of(), "core1", fire(0, false)));
    }

    @Test
    void refundsKeepRequirements() {
        // core3 is off to the side, so giving it back keeps the tree connected but drops the gate's Affinity to 2.
        assertEquals(Check.NEEDED, TREE.canRefund(Set.of("core1", "core2", "core3", "ice_gate"), "core3", fireAndWater(0)));
        assertTrue(TREE.requirementsMet(Set.of("core1", "core2", "ice_gate"), key -> 2));
        assertFalse(TREE.requirementsMet(Set.of("core1", "core2", "ice_gate"), key -> 1));
    }

    @Test
    void familyMembersBuyTheStart() {
        Set<String> taken = Set.of("core1", "core2", "ice_gate");
        assertEquals(Check.OK, TREE.canTake(taken, "ice_start", fireAndWater(2)));
        assertTrue(TREE.grants(TREE.held(Set.of("core1", "core2", "ice_gate", "ice_start"), fireAndWater(2))).spells().contains(ICICLE));
        TestPlayer icePlayer = new TestPlayer(Set.of("ice"), 1, Map.of(), false);
        assertEquals(Check.START, TREE.canTake(Set.of(), "ice_start", icePlayer));
        // A bought start isn't a root: giving back the gate would strand it.
        assertEquals(Check.DISCONNECTS, TREE.canRefund(Set.of("core1", "core2", "ice_gate", "ice_start"), "ice_gate", fireAndWater(2)));
    }
}
