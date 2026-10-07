package com.chappadodle.elementalarcana.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MentorChaptersTest {

    @Test
    void chaptersAreWellFormed() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < MentorChapters.CHAPTERS.size(); i++) {
            MentorChapters.Chapter chapter = MentorChapters.CHAPTERS.get(i);
            assertTrue(ids.add(chapter.id()), "duplicate id " + chapter.id());
            if (i == 0) {
                assertNull(chapter.advancement(), "the first chapter is finished by listening");
            } else {
                assertNotNull(chapter.advancement(), chapter.id() + " has no task");
                assertTrue(chapter.advancement().startsWith("elementalarcana:arcana/"));
            }
            for (MentorChapters.Reward reward : chapter.rewards()) {
                assertTrue(reward.count() > 0);
                assertTrue(reward.item().equals(MentorChapters.OWN_ESSENCE) || reward.item().contains(":"), reward.item());
            }
        }
        assertEquals("voice", MentorChapters.CHAPTERS.get(0).id());
        assertEquals("bound", MentorChapters.CHAPTERS.get(MentorChapters.CHAPTERS.size() - 1).id());
    }

    @Test
    void theTaleEnds() {
        int size = MentorChapters.CHAPTERS.size();
        assertNotNull(MentorChapters.at(size - 1));
        assertNull(MentorChapters.at(size));
        assertNull(MentorChapters.at(-1));
        assertFalse(MentorChapters.finished(size - 1));
        assertTrue(MentorChapters.finished(size));
    }

    @Test
    void placesAreSavedByIdAndOldNumbersStillFindTheirChapter() {
        for (int place = 0; place <= MentorChapters.CHAPTERS.size(); place++) {
            assertEquals(place, MentorChapters.placeOf(MentorChapters.idOf(place)));
        }
        assertEquals(MentorChapters.CHAPTERS.size(), MentorChapters.placeOf(MentorChapters.DONE));
        assertEquals(0, MentorChapters.placeOf("no such chapter"));
        // 0.10's number 9 was the Seals (sanctum); 15 and up, the tale told.
        assertEquals("sanctum", MentorChapters.idOf(MentorChapters.placeOfLegacy(9)));
        assertEquals("hollowed", MentorChapters.idOf(MentorChapters.placeOfLegacy(8)));
        assertTrue(MentorChapters.finished(MentorChapters.placeOfLegacy(15)));
        assertEquals(0, MentorChapters.placeOfLegacy(0));
    }

    @Test
    void theCircleAndTheForgesComeBetweenTheHollowedAndTheSeals() {
        int hollowed = MentorChapters.placeOf("hollowed");
        assertEquals("circle", MentorChapters.idOf(hollowed + 1));
        assertEquals("forge", MentorChapters.idOf(hollowed + 2));
        assertEquals("sanctum", MentorChapters.idOf(hollowed + 3));
    }

    @Test
    void xpIsLevelsWorth() {
        MentorChapters.Chapter shrine = MentorChapters.CHAPTERS.get(1);
        MentorChapters.Chapter bound = MentorChapters.CHAPTERS.get(MentorChapters.CHAPTERS.size() - 1);
        assertEquals(Progression.xpToNextLevel(1), MentorChapters.xp(shrine, 1));
        assertEquals(Progression.xpToNextLevel(30), MentorChapters.xp(shrine, 30));
        assertEquals(3 * Progression.xpToNextLevel(50), MentorChapters.xp(bound, 50), 1);
        assertEquals(0, MentorChapters.xp(MentorChapters.CHAPTERS.get(0), 10));
        assertEquals(0, MentorChapters.xp(shrine, Progression.MAX_LEVEL));
    }
}
