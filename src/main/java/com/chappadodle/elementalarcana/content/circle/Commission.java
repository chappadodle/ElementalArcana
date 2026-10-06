package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.api.CommissionRules;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A Circle Commission's terms (the {@code elementalarcana:commission} component): its task, how
 * many deeds it takes and how many are done, and what it pays in Marks of the Circle (see the
 * Circle spec, part 2).
 */
public record Commission(String task, int needed, int done, int marks) {
    public static final Codec<Commission> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("task").forGetter(Commission::task),
            Codec.INT.fieldOf("needed").forGetter(Commission::needed),
            Codec.INT.fieldOf("done").forGetter(Commission::done),
            Codec.INT.fieldOf("marks").forGetter(Commission::marks)
    ).apply(instance, Commission::new));
    public static final StreamCodec<ByteBuf, Commission> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** A fresh commission, as offered. */
    public static Commission of(CommissionRules.Offer offer) {
        return new Commission(offer.task().id(), offer.count(), 0, offer.marks());
    }

    public CommissionRules.Task kind() {
        return CommissionRules.Task.byId(task);
    }

    public boolean complete() {
        return done >= needed;
    }

    /** One more deed done (never past what it needs). */
    public Commission progressed() {
        return new Commission(task, needed, Math.min(needed, done + 1), marks);
    }

    /** All its deeds done (for testing). */
    public Commission finished() {
        return new Commission(task, needed, needed, marks);
    }
}
