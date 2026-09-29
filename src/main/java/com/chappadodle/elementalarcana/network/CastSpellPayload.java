package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.Conjuring;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the cast key went down or came back up, or a launch key was pressed while
 * conjured projectiles are held (see Conjuring).
 */
public record CastSpellPayload(Action action) implements CustomPacketPayload {
    public static final CastSpellPayload PRESS = new CastSpellPayload(Action.PRESS);
    public static final CastSpellPayload RELEASE = new CastSpellPayload(Action.RELEASE);
    public static final CastSpellPayload LAUNCH_ONE = new CastSpellPayload(Action.LAUNCH_ONE);
    public static final CastSpellPayload LAUNCH_ALL = new CastSpellPayload(Action.LAUNCH_ALL);
    public static final Type<CastSpellPayload> TYPE = new Type<>(ElementalArcana.id("cast_spell"));
    public static final StreamCodec<ByteBuf, CastSpellPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Action.values()[ordinal], Action::ordinal), CastSpellPayload::action,
            CastSpellPayload::new);

    public enum Action {
        PRESS, RELEASE, LAUNCH_ONE, LAUNCH_ALL
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CastSpellPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive()) {
            return;
        }
        switch (payload.action()) {
            case PRESS -> CastingService.tryCast(player);
            case RELEASE -> CastingService.release(player);
            case LAUNCH_ONE -> Conjuring.launchOne(player);
            case LAUNCH_ALL -> Conjuring.launchAll(player);
        }
    }
}
