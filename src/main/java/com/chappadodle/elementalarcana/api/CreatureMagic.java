package com.chappadodle.elementalarcana.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Locale;

/** An Attuned creature's magic: its element and rank. Saved with the creature and synced to clients. */
public record CreatureMagic(Element element, AttunementRank rank) {
    public static final Codec<CreatureMagic> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            enumCodec(Element.class).fieldOf("element").forGetter(CreatureMagic::element),
            enumCodec(AttunementRank.class).fieldOf("rank").forGetter(CreatureMagic::rank)
    ).apply(instance, CreatureMagic::new));

    public static final StreamCodec<ByteBuf, CreatureMagic> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Element.values()[ordinal], Element::ordinal), CreatureMagic::element,
            ByteBufCodecs.VAR_INT.map(ordinal -> AttunementRank.values()[ordinal], AttunementRank::ordinal), CreatureMagic::rank,
            CreatureMagic::new);

    /** Saves an enum as its lowercase name ("fire", "archmage"). */
    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
            }
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }
}
