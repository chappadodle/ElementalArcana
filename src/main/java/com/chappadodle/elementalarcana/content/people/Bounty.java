package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.api.BountyRules;
import com.chappadodle.elementalarcana.api.Element;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A Bounty Contract's terms (the {@code elementalarcana:bounty} component): its task, the element
 * it names (or ""), how many deeds it takes and how many are done, and what it pays: emeralds,
 * Essence (of {@code rewardElement}), and an extra item ({@code extra}, an item id, or "").
 */
public record Bounty(String task, String element, int needed, int done, int emeralds, int essence, String rewardElement,
                     String extra, int experience) {
    public static final Codec<Bounty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("task").forGetter(Bounty::task),
            Codec.STRING.optionalFieldOf("element", "").forGetter(Bounty::element),
            Codec.INT.fieldOf("needed").forGetter(Bounty::needed),
            Codec.INT.fieldOf("done").forGetter(Bounty::done),
            Codec.INT.fieldOf("emeralds").forGetter(Bounty::emeralds),
            Codec.INT.optionalFieldOf("essence", 0).forGetter(Bounty::essence),
            Codec.STRING.optionalFieldOf("reward_element", "").forGetter(Bounty::rewardElement),
            Codec.STRING.optionalFieldOf("extra", "").forGetter(Bounty::extra),
            Codec.INT.optionalFieldOf("experience", 0).forGetter(Bounty::experience)
    ).apply(instance, Bounty::new));
    public static final StreamCodec<ByteBuf, Bounty> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public BountyRules.Task kind() {
        return BountyRules.Task.byId(task);
    }

    /** The element the task names, or null. */
    @Nullable
    public Element taskElement() {
        return parse(element);
    }

    /** The element of the Essence it pays, or null. */
    @Nullable
    public Element essenceElement() {
        return parse(rewardElement);
    }

    public boolean complete() {
        return done >= needed;
    }

    /** One more deed done (never past what it needs). */
    public Bounty progressed() {
        return new Bounty(task, element, needed, Math.min(needed, done + 1), emeralds, essence, rewardElement, extra, experience);
    }

    @Nullable
    private static Element parse(String name) {
        if (name.isEmpty()) {
            return null;
        }
        try {
            return Element.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
