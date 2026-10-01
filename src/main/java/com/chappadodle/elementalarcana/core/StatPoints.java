package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Stat;
import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A player's spent stat points, by key: a Stat's key ("reservoir") or an element family's
 * Affinity ("affinity/water"). Keys are strings so new elements and stats never break a save.
 */
public final class StatPoints {
    public static final Codec<StatPoints> CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT)
            .xmap(StatPoints::new, points -> points.points);
    private static final String AFFINITY_PREFIX = "affinity/";

    private final Map<String, Integer> points;

    public StatPoints() {
        this(Map.of());
    }

    private StatPoints(Map<String, Integer> points) {
        this.points = new HashMap<>(points);
    }

    /** The key of {@code element}'s family Affinity, e.g. "affinity/water" for Ice. */
    public static String affinityKey(Element element) {
        return AFFINITY_PREFIX + element.family().name().toLowerCase(Locale.ROOT);
    }

    public int get(String key) {
        return points.getOrDefault(key, 0);
    }

    public int get(Stat stat) {
        return get(stat.key());
    }

    /** Points in the Affinity of {@code element}'s family. */
    public int affinity(Element element) {
        return get(affinityKey(element));
    }

    public int total() {
        return points.values().stream().mapToInt(Integer::intValue).sum();
    }

    void add(String key, int amount) {
        points.merge(key, amount, Integer::sum);
    }

    void clear() {
        points.clear();
    }

    static void write(FriendlyByteBuf buf, StatPoints stats) {
        buf.writeMap(stats.points, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeVarInt);
    }

    static StatPoints read(FriendlyByteBuf buf) {
        return new StatPoints(buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readVarInt));
    }
}
