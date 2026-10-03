package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.client.ManaTideClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: whether a mana tide runs now (see ManaTides). */
public record ManaTidePayload(boolean active) implements CustomPacketPayload {
    public static final Type<ManaTidePayload> TYPE = new Type<>(ElementalArcana.id("mana_tide"));
    public static final StreamCodec<ByteBuf, ManaTidePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ManaTidePayload::new, ManaTidePayload::active);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ManaTidePayload payload, IPayloadContext context) {
        ManaTideClient.setActive(payload.active());
    }
}
