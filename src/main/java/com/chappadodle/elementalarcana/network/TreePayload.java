package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.SkillTree;
import com.chappadodle.elementalarcana.content.EssenceService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.core.PlayerStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: take a skill tree node, give one back, or give back a stat point. Refunds cost
 * Essence of any element (Progression#refundCost). The server checks every rule itself.
 */
public record TreePayload(Action action, String id) implements CustomPacketPayload {
    public static final Type<TreePayload> TYPE = new Type<>(ElementalArcana.id("tree"));
    public static final StreamCodec<ByteBuf, TreePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Action.values()[ordinal], Action::ordinal), TreePayload::action,
            ByteBufCodecs.STRING_UTF8, TreePayload::id,
            TreePayload::new);

    public enum Action {
        TAKE, REFUND, REFUND_STAT
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TreePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        switch (payload.action()) {
            case TAKE -> {
                if (data.take(payload.id()) == SkillTree.Check.OK) {
                    player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.1f);
                }
            }
            case REFUND -> {
                if (data.checkRefund(payload.id()) == SkillTree.Check.OK && pay(player, data)) {
                    data.refund(payload.id());
                    player.playNotifySound(SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
            }
            case REFUND_STAT -> {
                if (data.canRefundStat(payload.id()) && pay(player, data)) {
                    data.refundStat(payload.id());
                    player.playNotifySound(SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
                }
            }
        }
        PlayerStats.apply(player);
        MagicAttachments.sync(player);
    }

    /** Takes the refund's Essence, or says it's short. */
    private static boolean pay(ServerPlayer player, MagicData data) {
        int cost = Progression.refundCost(data.level());
        if (EssenceService.total(player) < cost) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.refund.no_essence", cost)
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        EssenceService.removeAny(player, cost);
        return true;
    }
}
