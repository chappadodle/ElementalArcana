package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.client.StarfallClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;

/** Server -> client: a falling star's streak across the sky, from {@code from} to {@code to} over {@code ticks}. */
public record StarStreakPayload(Vector3f from, Vector3f to, int ticks) implements CustomPacketPayload {
    public static final Type<StarStreakPayload> TYPE = new Type<>(ElementalArcana.id("star_streak"));
    public static final StreamCodec<ByteBuf, StarStreakPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VECTOR3F, StarStreakPayload::from,
            ByteBufCodecs.VECTOR3F, StarStreakPayload::to,
            ByteBufCodecs.VAR_INT, StarStreakPayload::ticks,
            StarStreakPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StarStreakPayload payload, IPayloadContext context) {
        StarfallClient.streak(payload.from(), payload.to(), payload.ticks());
    }
}
