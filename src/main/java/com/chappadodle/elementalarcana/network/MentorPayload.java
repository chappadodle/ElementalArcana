package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.client.MentorScreen;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server -> client: Caelith's chapter for the stone's screen: which (past the last: the tale is
 * told), whether its task is done, and its reward (items and XP).
 */
public record MentorPayload(int chapter, boolean done, List<ItemStack> rewards, int xp) implements CustomPacketPayload {
    public static final Type<MentorPayload> TYPE = new Type<>(ElementalArcana.id("mentor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MentorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MentorPayload::chapter,
            ByteBufCodecs.BOOL, MentorPayload::done,
            ItemStack.LIST_STREAM_CODEC, MentorPayload::rewards,
            ByteBufCodecs.VAR_INT, MentorPayload::xp,
            MentorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MentorPayload payload, IPayloadContext context) {
        MentorScreen.show(payload);
    }
}
