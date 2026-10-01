package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.PlayerStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: spend one stat point on a stat key (see StatPoints). The server checks it. */
public record StatPayload(String key) implements CustomPacketPayload {
    public static final Type<StatPayload> TYPE = new Type<>(ElementalArcana.id("stat"));
    public static final StreamCodec<ByteBuf, StatPayload> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(StatPayload::new, StatPayload::key);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StatPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && MagicAttachments.get(player).spend(payload.key())) {
            PlayerStats.apply(player);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
            MagicAttachments.sync(player);
        }
    }
}
