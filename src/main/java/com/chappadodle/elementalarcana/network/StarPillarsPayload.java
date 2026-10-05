package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.client.StarfallClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server -> client: every fallen star in this dimension with its pillar of light still standing. */
public record StarPillarsPayload(List<BlockPos> pillars) implements CustomPacketPayload {
    public static final Type<StarPillarsPayload> TYPE = new Type<>(ElementalArcana.id("star_pillars"));
    public static final StreamCodec<ByteBuf, StarPillarsPayload> STREAM_CODEC =
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()).map(StarPillarsPayload::new, StarPillarsPayload::pillars);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StarPillarsPayload payload, IPayloadContext context) {
        StarfallClient.pillars(payload.pillars());
    }
}
