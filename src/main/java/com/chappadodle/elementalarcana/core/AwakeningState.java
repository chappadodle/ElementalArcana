package com.chappadodle.elementalarcana.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * What awakening remembers about a player (see AwakeningRules): the world day-clock time they were
 * first seen without magic (the day count starts there), the last day a roll was made, and how many
 * Catalysts have failed since the last success, by number of element families held.
 */
public final class AwakeningState {
    public static final Codec<AwakeningState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("start", -1L).forGetter(state -> state.start),
            Codec.INT.optionalFieldOf("last_rolled_day", -1).forGetter(state -> state.lastRolledDay),
            Codec.INT.listOf().optionalFieldOf("failures", List.of()).forGetter(state -> state.failures)
    ).apply(instance, AwakeningState::new));

    private long start;
    private int lastRolledDay;
    private final List<Integer> failures;

    public AwakeningState() {
        this(-1L, -1, List.of());
    }

    private AwakeningState(long start, int lastRolledDay, List<Integer> failures) {
        this.start = start;
        this.lastRolledDay = lastRolledDay;
        this.failures = new ArrayList<>(failures);
    }

    static void write(FriendlyByteBuf buf, AwakeningState state) {
        buf.writeLong(state.start);
        buf.writeVarInt(state.lastRolledDay);
        buf.writeCollection(state.failures, FriendlyByteBuf::writeVarInt);
    }

    static AwakeningState read(FriendlyByteBuf buf) {
        return new AwakeningState(buf.readLong(), buf.readVarInt(), buf.readList(FriendlyByteBuf::readVarInt));
    }

    /** The day-clock time the day count started at, or -1 if it hasn't started. */
    public long start() {
        return start;
    }

    void setStart(long start) {
        this.start = start;
    }

    public int lastRolledDay() {
        return lastRolledDay;
    }

    void setLastRolledDay(int day) {
        this.lastRolledDay = day;
    }

    /** Failed Catalysts since the last success while holding {@code familiesHeld} families. */
    public int failures(int familiesHeld) {
        return familiesHeld >= 1 && familiesHeld <= failures.size() ? failures.get(familiesHeld - 1) : 0;
    }

    void setFailures(int familiesHeld, int count) {
        while (failures.size() < familiesHeld) {
            failures.add(0);
        }
        failures.set(familiesHeld - 1, Math.max(0, count));
    }
}
