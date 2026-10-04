package com.chappadodle.elementalarcana.api;

import java.util.Locale;
import java.util.function.DoubleSupplier;

/**
 * Arcanist Bounties (see the Bounties spec): what an Arcanist asks at each of its trade levels, how
 * many, and what it pays. The contract's items are made from these by content code.
 */
public final class BountyRules {
    /** What a bounty asks for. */
    public enum Task {
        /** Slay creatures Attuned to an element. */
        ATTUNED,
        /** Defeat wisps. */
        WISPS,
        /** Slay a Magus, of any element. */
        MAGUS,
        /** Close an elemental rift. */
        RIFT,
        /** Slay an Archmage, of any element. */
        ARCHMAGE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Task byId(String id) {
            for (Task task : values()) {
                if (task.id().equals(id)) {
                    return task;
                }
            }
            return ATTUNED;
        }

        /** Whether the task names an element (the contract then carries one). */
        public boolean hasElement() {
            return this == ATTUNED;
        }
    }

    /** What a contract costs, in emeralds. */
    public static final int PRICE = 1;

    private BountyRules() {
    }

    /** The task an Arcanist offers at trade level {@code level} (1 Novice to 5 Master). */
    public static Task taskFor(int level) {
        return switch (level) {
            case 1 -> Task.ATTUNED;
            case 2 -> Task.WISPS;
            case 3 -> Task.MAGUS;
            case 4 -> Task.RIFT;
            default -> Task.ARCHMAGE;
        };
    }

    /** How many deeds the task takes ({@code roll} is uniform in [0, 1)). */
    public static int count(Task task, DoubleSupplier roll) {
        return switch (task) {
            case ATTUNED -> 5 + (int) (roll.getAsDouble() * 4);
            case WISPS -> 3 + (int) (roll.getAsDouble() * 3);
            default -> 1;
        };
    }

    /** The emeralds it pays ({@code roll} as above). */
    public static int emeralds(Task task, DoubleSupplier roll) {
        return switch (task) {
            case ATTUNED, WISPS -> 8 + (int) (roll.getAsDouble() * 5);
            case MAGUS -> 16;
            case RIFT -> 20;
            case ARCHMAGE -> 32;
        };
    }

    /** The Essence it pays (of the task's element, or the Arcanist's land's). */
    public static int essence(Task task) {
        return switch (task) {
            case ATTUNED -> 4;
            case ARCHMAGE -> 8;
            default -> 0;
        };
    }

    /** The experience handing it in gives. */
    public static int experience(Task task) {
        return switch (task) {
            case ATTUNED, WISPS -> 10;
            case MAGUS -> 20;
            case RIFT -> 30;
            case ARCHMAGE -> 50;
        };
    }
}
