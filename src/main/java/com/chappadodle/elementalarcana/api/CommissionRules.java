package com.chappadodle.elementalarcana.api;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The Archmagister's commissions (see the Circle spec, part 2): the tasks each stage of a player's
 * story is offered (the stage the Circle's talk follows, CircleTalk), how many deeds each takes and
 * what it pays in Marks of the Circle. Plain Java, unit tested; the commission letters are content
 * code's.
 */
public final class CommissionRules {
    /** What a commission asks. */
    public enum Task {
        /** Slay the Hollowed (any of them). */
        HOLLOWED,
        /** Close an elemental rift. */
        RIFT,
        /** Slay creatures Attuned to any element. */
        ATTUNED,
        /** Break a Hunger Obelisk. */
        OBELISK,
        /** Slay a Magus, of any element. */
        MAGUS,
        /** Slay a Hollow Herald. */
        HERALD,
        /** Slay a tower's Magister. */
        MAGISTER,
        /** Slay a crypt's Revenant. */
        REVENANT,
        /** Slay an Archmage, of any element. */
        ARCHMAGE,
        /** Defeat a Sovereign. */
        SOVEREIGN;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Task byId(String id) {
            for (Task task : values()) {
                if (task.id().equals(id)) {
                    return task;
                }
            }
            return HOLLOWED;
        }
    }

    /** A commission as offered: its task, how many deeds it takes, and its pay in marks. */
    public record Offer(Task task, int count, int marks) {
    }

    /** The (vanilla) experience a commission pays for each of its marks. */
    public static final int XP_PER_MARK = 8;

    private CommissionRules() {
    }

    /** The commissions a player at {@code stage} may be given: none while their magic sleeps. */
    public static List<Offer> offers(CircleTalk.Stage stage) {
        return switch (stage) {
            case SLEEPING -> List.of();
            case NOVICE -> List.of(new Offer(Task.HOLLOWED, 5, 2), new Offer(Task.RIFT, 1, 2), new Offer(Task.ATTUNED, 6, 2));
            case ADEPT -> List.of(new Offer(Task.OBELISK, 1, 4), new Offer(Task.MAGUS, 1, 4), new Offer(Task.HERALD, 1, 4));
            case MASTER -> List.of(new Offer(Task.MAGISTER, 1, 7), new Offer(Task.REVENANT, 1, 7), new Offer(Task.ARCHMAGE, 1, 7));
            case ARCHMAGE -> List.of(new Offer(Task.SOVEREIGN, 1, 12), new Offer(Task.OBELISK, 2, 10), new Offer(Task.ARCHMAGE, 2, 10));
        };
    }

    /** The commission a player at {@code stage} is given ({@code roll} uniform in [0, 1)), or null while their magic sleeps. */
    @Nullable
    public static Offer roll(CircleTalk.Stage stage, double roll) {
        List<Offer> offers = offers(stage);
        if (offers.isEmpty()) {
            return null;
        }
        return offers.get(Math.min(offers.size() - 1, Math.max(0, (int) (roll * offers.size()))));
    }

    /** The first commission offered for {@code task}, the earliest stage's (every task has one; for testing). */
    public static Offer offerFor(Task task) {
        for (CircleTalk.Stage stage : CircleTalk.Stage.values()) {
            for (Offer offer : offers(stage)) {
                if (offer.task() == task) {
                    return offer;
                }
            }
        }
        throw new IllegalArgumentException("no commission for " + task);
    }

    /** The experience a commission paying {@code marks} gives. */
    public static int experience(int marks) {
        return marks * XP_PER_MARK;
    }
}
