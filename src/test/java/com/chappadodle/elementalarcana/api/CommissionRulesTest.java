package com.chappadodle.elementalarcana.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommissionRulesTest {

    @Test
    void aSleepingMageIsGivenNoWork() {
        assertTrue(CommissionRules.offers(CircleTalk.Stage.SLEEPING).isEmpty());
        assertNull(CommissionRules.roll(CircleTalk.Stage.SLEEPING, 0.5));
    }

    @Test
    void everyAwakeStageHasThreeCommissions() {
        for (CircleTalk.Stage stage : CircleTalk.Stage.values()) {
            if (stage != CircleTalk.Stage.SLEEPING) {
                assertEquals(3, CommissionRules.offers(stage).size(), stage.id());
            }
        }
    }

    @Test
    void thePayGrowsWithTheStory() {
        int last = 0;
        for (CircleTalk.Stage stage : List.of(CircleTalk.Stage.NOVICE, CircleTalk.Stage.ADEPT, CircleTalk.Stage.MASTER, CircleTalk.Stage.ARCHMAGE)) {
            int least = CommissionRules.offers(stage).stream().mapToInt(CommissionRules.Offer::marks).min().orElseThrow();
            assertTrue(least > last, stage.id() + " pays more than the stage before");
            last = CommissionRules.offers(stage).stream().mapToInt(CommissionRules.Offer::marks).max().orElseThrow();
        }
    }

    @Test
    void everyCommissionTakesAtLeastOneDeedAndPays() {
        for (CircleTalk.Stage stage : CircleTalk.Stage.values()) {
            for (CommissionRules.Offer offer : CommissionRules.offers(stage)) {
                assertTrue(offer.count() >= 1 && offer.marks() >= 1, offer.toString());
            }
        }
    }

    @Test
    void theRollReachesEveryCommission() {
        List<CommissionRules.Offer> offers = CommissionRules.offers(CircleTalk.Stage.ADEPT);
        assertEquals(offers.get(0), CommissionRules.roll(CircleTalk.Stage.ADEPT, 0.0));
        assertEquals(offers.get(1), CommissionRules.roll(CircleTalk.Stage.ADEPT, 0.5));
        assertEquals(offers.get(2), CommissionRules.roll(CircleTalk.Stage.ADEPT, 0.999));
    }

    @Test
    void tasksAreFoundByTheirIds() {
        for (CommissionRules.Task task : CommissionRules.Task.values()) {
            assertEquals(task, CommissionRules.Task.byId(task.id()));
        }
        assertEquals(CommissionRules.Task.HOLLOWED, CommissionRules.Task.byId("no such task"));
    }

    @Test
    void everyTaskIsOfferedSomewhere() {
        for (CommissionRules.Task task : CommissionRules.Task.values()) {
            assertEquals(task, CommissionRules.offerFor(task).task());
        }
        assertEquals(new CommissionRules.Offer(CommissionRules.Task.ARCHMAGE, 1, 7), CommissionRules.offerFor(CommissionRules.Task.ARCHMAGE));
    }

    @Test
    void experienceIsEightPointsAMark() {
        assertEquals(16, CommissionRules.experience(2));
        assertEquals(96, CommissionRules.experience(12));
    }
}
