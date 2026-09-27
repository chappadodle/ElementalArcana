package com.chappadodle.elementalarcana.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** One player's progress in one spell: its level, mastery toward the next level, and branch picks. */
public final class SpellProgress {
    public static final Codec<SpellProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("level", 1).forGetter(progress -> progress.level),
            Codec.INT.optionalFieldOf("mastery", 0).forGetter(progress -> progress.mastery),
            // JSON/NBT map keys must be strings, so branch levels are stored as "5", "10", ...
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("branches", Map.of()).forGetter(SpellProgress::branchesByString)
    ).apply(instance, SpellProgress::fromStrings));

    private int level;
    private int mastery;
    private final Map<Integer, String> branches;

    public SpellProgress() {
        this(1, 0, Map.of());
    }

    private SpellProgress(int level, int mastery, Map<Integer, String> branches) {
        this.level = Math.max(1, level);
        this.mastery = Math.max(0, mastery);
        this.branches = new HashMap<>(branches);
    }

    private static SpellProgress fromStrings(int level, int mastery, Map<String, String> branches) {
        Map<Integer, String> byLevel = new HashMap<>();
        branches.forEach((key, value) -> {
            try {
                byLevel.put(Integer.parseInt(key), value);
            } catch (NumberFormatException ignored) {
                // Unreadable entry from a hand-edited save: drop it, the player can pick again.
            }
        });
        return new SpellProgress(level, mastery, byLevel);
    }

    private Map<String, String> branchesByString() {
        Map<String, String> out = new HashMap<>();
        branches.forEach((key, value) -> out.put(String.valueOf(key), value));
        return out;
    }

    static void write(FriendlyByteBuf buf, SpellProgress progress) {
        buf.writeVarInt(progress.level);
        buf.writeVarInt(progress.mastery);
        buf.writeMap(progress.branches, (out, level) -> out.writeVarInt(level), (out, branch) -> out.writeUtf(branch));
    }

    static SpellProgress read(FriendlyByteBuf buf) {
        return new SpellProgress(buf.readVarInt(), buf.readVarInt(), buf.readMap(in -> in.readVarInt(), in -> in.readUtf()));
    }

    public int level() {
        return level;
    }

    public int mastery() {
        return mastery;
    }

    public Map<Integer, String> branches() {
        return Collections.unmodifiableMap(branches);
    }

    void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    void setMastery(int mastery) {
        this.mastery = Math.max(0, mastery);
    }

    void setBranch(int level, String branch) {
        branches.put(level, branch);
    }

    void clearBranches() {
        branches.clear();
    }

    void clearBranchesAbove(int level) {
        branches.keySet().removeIf(branchLevel -> branchLevel > level);
    }
}
