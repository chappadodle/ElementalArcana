package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.core.Conjuring;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: select a specific known spell (from the spellbook screen). */
public record SelectSpellPayload(ResourceLocation spell) implements CustomPacketPayload {
    public static final Type<SelectSpellPayload> TYPE = new Type<>(ElementalArcana.id("select_spell"));
    public static final StreamCodec<ByteBuf, SelectSpellPayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, SelectSpellPayload::spell, SelectSpellPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectSpellPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Spell current = MagicAttachments.get(player).selectedSpell();
        if (current != null && !current.id().equals(payload.spell())) {
            // Switching spells throws whatever is conjured and held.
            Conjuring.onSpellSwitched(player);
        }
        if (MagicAttachments.get(player).select(payload.spell())) {
            MagicAttachments.sync(player);
        }
    }
}
