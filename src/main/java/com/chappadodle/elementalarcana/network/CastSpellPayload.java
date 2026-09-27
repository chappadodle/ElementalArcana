package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.CastingService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: cast the selected spell. */
public record CastSpellPayload() implements CustomPacketPayload {
    public static final CastSpellPayload INSTANCE = new CastSpellPayload();
    public static final Type<CastSpellPayload> TYPE = new Type<>(ElementalArcana.id("cast_spell"));
    public static final StreamCodec<ByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastSpellPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && player.isAlive()) {
            CastingService.tryCast(player);
        }
    }
}
