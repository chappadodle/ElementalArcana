package com.chappadodle.elementalarcana.content.relic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/**
 * Who bound a relic, and when (game time): of the relics a player carries bound to them, the one
 * bound last is the one that works (see Relics).
 */
public record RelicBond(UUID owner, long boundAt) {
    public static final Codec<RelicBond> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(RelicBond::owner),
            Codec.LONG.fieldOf("bound_at").forGetter(RelicBond::boundAt)
    ).apply(instance, RelicBond::new));
    public static final StreamCodec<ByteBuf, RelicBond> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RelicBond::owner,
            ByteBufCodecs.VAR_LONG, RelicBond::boundAt,
            RelicBond::new);
}
