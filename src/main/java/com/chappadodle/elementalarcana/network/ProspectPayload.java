package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.client.ProspectOutlines;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Server -> client: the ores a Prospect found, their outline colours, and how long to show them. */
public record ProspectPayload(List<BlockPos> positions, List<Integer> colours, int ticks) implements CustomPacketPayload {
    public static final Type<ProspectPayload> TYPE = new Type<>(ElementalArcana.id("prospect"));
    public static final StreamCodec<ByteBuf, ProspectPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), ProspectPayload::positions,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), ProspectPayload::colours,
            ByteBufCodecs.VAR_INT, ProspectPayload::ticks,
            ProspectPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProspectPayload payload, IPayloadContext context) {
        ProspectOutlines.show(payload.positions(), payload.colours(), payload.ticks());
    }
}
