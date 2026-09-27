package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.CastingService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the cast key went down (cast) or came back up (release a held spell). */
public record CastSpellPayload(boolean pressed) implements CustomPacketPayload {
    public static final CastSpellPayload PRESS = new CastSpellPayload(true);
    public static final CastSpellPayload RELEASE = new CastSpellPayload(false);
    public static final Type<CastSpellPayload> TYPE = new Type<>(ElementalArcana.id("cast_spell"));
    public static final StreamCodec<ByteBuf, CastSpellPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, CastSpellPayload::pressed, CastSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastSpellPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        if (payload.pressed()) {
            CastingService.tryCast(player);
        } else {
            CastingService.release(player);
        }
    }
}
