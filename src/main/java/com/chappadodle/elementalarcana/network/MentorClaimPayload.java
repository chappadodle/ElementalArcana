package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.mentor.Mentor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: take the reward of the chapter the screen showed (refused unless it's still the player's, and done). */
public record MentorClaimPayload(int chapter) implements CustomPacketPayload {
    public static final Type<MentorClaimPayload> TYPE = new Type<>(ElementalArcana.id("mentor_claim"));
    public static final StreamCodec<ByteBuf, MentorClaimPayload> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(MentorClaimPayload::new, MentorClaimPayload::chapter);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MentorClaimPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            Mentor.claim(player, payload.chapter());
        }
    }
}
