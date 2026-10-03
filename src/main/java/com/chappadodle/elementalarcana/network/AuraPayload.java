package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: hide your aura, or show it again (see ManaWeather). */
public record AuraPayload() implements CustomPacketPayload {
    public static final AuraPayload TOGGLE = new AuraPayload();
    public static final Type<AuraPayload> TYPE = new Type<>(ElementalArcana.id("aura"));
    public static final StreamCodec<ByteBuf, AuraPayload> STREAM_CODEC = StreamCodec.unit(TOGGLE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AuraPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        boolean hidden = !data.auraHidden();
        data.setAuraHidden(hidden);
        MagicAttachments.sync(player);
        player.displayClientMessage(Component.translatable(hidden ? "message.elementalarcana.aura.hidden" : "message.elementalarcana.aura.shown"), true);
        player.playNotifySound(hidden ? SoundEvents.AMETHYST_BLOCK_RESONATE : SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f,
                hidden ? 0.6f : 1.2f);
    }
}
