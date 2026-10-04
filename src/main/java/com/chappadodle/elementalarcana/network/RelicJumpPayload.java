package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Relic;
import com.chappadodle.elementalarcana.content.relic.Relics;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the player jumped in mid-air on the Feather of the Gale (client/RelicJump): a gust for all to see. */
public record RelicJumpPayload() implements CustomPacketPayload {
    public static final RelicJumpPayload INSTANCE = new RelicJumpPayload();
    public static final Type<RelicJumpPayload> TYPE = new Type<>(ElementalArcana.id("relic_jump"));
    public static final StreamCodec<ByteBuf, RelicJumpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RelicJumpPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && Relics.bears(player, Relic.FEATHER_OF_THE_GALE)) {
            player.resetFallDistance();
            player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 12, 0.3, 0.05, 0.3, 0.04);
            player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
    }
}
