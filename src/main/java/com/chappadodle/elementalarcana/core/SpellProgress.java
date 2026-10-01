package com.chappadodle.elementalarcana.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;

/**
 * One player's mastery in one spell: how full its bar toward the next level is. A spell's level and
 * branches come from the skill tree (see MagicData#grants); saves from before the tree still have
 * "level" and "branches" fields, which are ignored, so those levels come back as tree points.
 */
public final class SpellProgress {
    public static final Codec<SpellProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("mastery", 0).forGetter(progress -> progress.mastery)
    ).apply(instance, SpellProgress::new));

    private int mastery;

    public SpellProgress() {
        this(0);
    }

    private SpellProgress(int mastery) {
        this.mastery = Math.max(0, mastery);
    }

    static void write(FriendlyByteBuf buf, SpellProgress progress) {
        buf.writeVarInt(progress.mastery);
    }

    static SpellProgress read(FriendlyByteBuf buf) {
        return new SpellProgress(buf.readVarInt());
    }

    public int mastery() {
        return mastery;
    }

    void setMastery(int mastery) {
        this.mastery = Math.max(0, mastery);
    }
}
