package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.spell.SkywardLeaps;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player started or stopped gliding (client/Gliding steers it; see SkywardLeaps). */
public record GlidePayload(boolean gliding) implements CustomPacketPayload {
    public static final Type<GlidePayload> TYPE = new Type<>(ElementalArcana.id("glide"));
    public static final StreamCodec<ByteBuf, GlidePayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(GlidePayload::new, GlidePayload::gliding);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GlidePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            SkywardLeaps.setGliding(player, payload.gliding());
        }
    }
}
