package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.api.SkillTree.Node;
import com.chappadodle.elementalarcana.api.SkillTree.Type;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotablesAndKeystonesTest {
    private static final SkillTree TREE = new SkillTree(List.of(
            new Node("small", Type.SMALL, 0, 0, null, "potency", 2, null, 0, null, null, 0),
            new Node("notable", Type.NOTABLE, 0, 0, "fire", null, 0, null, 0, null, null, 0,
                    Map.of("affinity/fire", 6, "potency", 4), null),
            new Node("keystone", Type.KEYSTONE, 0, 0, "fire", null, 0, null, 0, null, null, 0,
                    Map.of(), Keystones.GLASS_CANNON)
    ), List.of(List.of("small", "notable"), List.of("notable", "keystone")));

    @Test
    void notablesAddAllTheirStatsToSmallNodes() {
        SkillTree.Grants grants = TREE.grants(Set.of("small", "notable"));
        assertEquals(6, grants.stat("potency"));
        assertEquals(6, grants.stat("affinity/fire"));
        assertTrue(grants.keystones().isEmpty());
    }

    @Test
    void keystonesAreCollectedAndGiveNoStats() {
        SkillTree.Grants grants = TREE.grants(Set.of("keystone"));
        assertTrue(grants.hasKeystone(Keystones.GLASS_CANNON));
        assertFalse(grants.hasKeystone(Keystones.BLOOD_MAGIC));
        assertTrue(grants.stats().isEmpty());
    }

    @Test
    void glassCannonTradesHealthForPower() {
        Set<String> held = Set.of(Keystones.GLASS_CANNON);
        assertEquals(1.3f, Keystones.powerFactor(held), 1e-6);
        assertEquals(-0.3, Keystones.healthBonus(held), 1e-9);
        assertEquals(1f, Keystones.powerFactor(Set.of()), 1e-6);
    }

    @Test
    void wellspringTradesPoolForRegen() {
        Set<String> held = Set.of(Keystones.WELLSPRING);
        assertEquals(0.75f, Keystones.maxManaFactor(held), 1e-6);
        assertEquals(1.6f, Keystones.regenFactor(held), 1e-6);
    }

    @Test
    void healthAndSpeedChangesAddUp() {
        Set<String> held = Set.of(Keystones.GLASS_CANNON, Keystones.MOUNTAIN_HEART, Keystones.GALE_STEP);
        assertEquals(0.1, Keystones.healthBonus(held), 1e-9);
        assertEquals(0.0, Keystones.speedBonus(held), 1e-9);
    }

    @Test
    void galeStepAndWintersGraspChangeDamageTaken() {
        assertEquals(1.15f, Keystones.damageTakenFactor(Set.of(Keystones.GALE_STEP), Element.WATER), 1e-6);
        assertEquals(1.4f, Keystones.damageTakenFactor(Set.of(Keystones.WINTERS_GRASP), Element.FIRE), 1e-6);
        assertEquals(1f, Keystones.damageTakenFactor(Set.of(Keystones.WINTERS_GRASP), Element.WATER), 1e-6);
        assertEquals(1.15f * 1.4f, Keystones.damageTakenFactor(Set.of(Keystones.GALE_STEP, Keystones.WINTERS_GRASP), Element.FIRE), 1e-5);
    }

    @Test
    void wintersGraspOnlyHitsTheChilledHarder() {
        Set<String> held = Set.of(Keystones.WINTERS_GRASP);
        assertEquals(1.3f, Keystones.damageDealtFactor(held, true), 1e-6);
        assertEquals(1f, Keystones.damageDealtFactor(held, false), 1e-6);
        assertEquals(1f, Keystones.damageDealtFactor(Set.of(), true), 1e-6);
    }

    @Test
    void conductorTradesManaForArcs() {
        assertEquals(0.8f, Keystones.maxManaFactor(Set.of(Keystones.CONDUCTOR)), 1e-6);
        assertEquals(0.75f * 0.8f, Keystones.maxManaFactor(Set.of(Keystones.CONDUCTOR, Keystones.WELLSPRING)), 1e-6);
        assertEquals(0.35f, Keystones.CONDUCTOR_SHARE, 1e-6);
    }

    @Test
    void sunbornFollowsTheLight() {
        Set<String> held = Set.of(Keystones.SUNBORN);
        assertEquals(1.25f, Keystones.lightFactor(held, 15), 1e-6);
        assertEquals(1.25f, Keystones.lightFactor(held, 12), 1e-6);
        assertEquals(1f, Keystones.lightFactor(held, 10), 1e-6);
        assertEquals(1f, Keystones.lightFactor(held, 8), 1e-6);
        assertEquals(0.8f, Keystones.lightFactor(held, 7), 1e-6);
        assertEquals(0.8f, Keystones.lightFactor(held, 0), 1e-6);
        assertEquals(1f, Keystones.lightFactor(Set.of(), 0), 1e-6);
    }

    @Test
    void refractionTurnsBackEveryThird() {
        Set<String> held = Set.of(Keystones.REFRACTION);
        assertFalse(Keystones.refracts(held, 1));
        assertFalse(Keystones.refracts(held, 2));
        assertTrue(Keystones.refracts(held, 3));
        assertTrue(Keystones.refracts(held, 6));
        assertFalse(Keystones.refracts(Set.of(), 3));
        assertEquals(1.2f, Keystones.meleeTakenFactor(held), 1e-6);
        assertEquals(1f, Keystones.meleeTakenFactor(Set.of()), 1e-6);
    }
}
