package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: level up a spell, pick one of its branches, or respec its branches.
 * Every rule (mastery, skill points, valid branch, respec cost) is re-checked here.
 */
public record SpellProgressPayload(Action action, ResourceLocation spell, int level, String branch) implements CustomPacketPayload {
    /** Respec: needs a full mana bar, drains it, and can then only be done again after a day. */
    public static final long RESPEC_COOLDOWN_TICKS = 24000;

    public static final Type<SpellProgressPayload> TYPE = new Type<>(ElementalArcana.id("spell_progress"));
    public static final StreamCodec<ByteBuf, SpellProgressPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Action.values()[ordinal], Action::ordinal), SpellProgressPayload::action,
            ResourceLocation.STREAM_CODEC, SpellProgressPayload::spell,
            ByteBufCodecs.VAR_INT, SpellProgressPayload::level,
            ByteBufCodecs.STRING_UTF8, SpellProgressPayload::branch,
            SpellProgressPayload::new);

    public enum Action {
        LEVEL_UP, CHOOSE_BRANCH, RESPEC
    }

    public static SpellProgressPayload levelUp(Spell spell) {
        return new SpellProgressPayload(Action.LEVEL_UP, spell.id(), 0, "");
    }

    public static SpellProgressPayload chooseBranch(Spell spell, int level, String branch) {
        return new SpellProgressPayload(Action.CHOOSE_BRANCH, spell.id(), level, branch);
    }

    public static SpellProgressPayload respec(Spell spell) {
        return new SpellProgressPayload(Action.RESPEC, spell.id(), 0, "");
    }

    /** Why a respec isn't possible right now, or null if it is. Shared with the client UI. */
    public static Component whyNoRespec(MagicData data, Spell spell, long gameTime) {
        if (data.progress(spell).branches().isEmpty()) {
            return Component.translatable("message.elementalarcana.respec.nothing");
        }
        if (gameTime < data.respecReadyAt()) {
            long minutes = (data.respecReadyAt() - gameTime) / 1200 + 1;
            return Component.translatable("message.elementalarcana.respec.cooldown", minutes);
        }
        if (data.mana() < data.maxMana()) {
            return Component.translatable("message.elementalarcana.respec.mana");
        }
        return null;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpellProgressPayload payload, IPayloadContext context) {
        Spell spell = SpellRegistries.SPELLS.get(payload.spell());
        if (!(context.player() instanceof ServerPlayer player) || spell == null) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        switch (payload.action()) {
            case LEVEL_UP -> {
                if (data.levelUp(spell)) {
                    int level = data.spellLevel(spell);
                    player.sendSystemMessage(Component.translatable("message.elementalarcana.spell_level_up",
                                    spell.displayName(), level, spell.tierName(level))
                            .withStyle(style -> style.withColor(spell.school().color())));
                    if (!spell.branchOptions(level).isEmpty()) {
                        player.sendSystemMessage(Component.translatable("message.elementalarcana.branch_available", spell.displayName())
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    celebrate(player, 1.3f);
                }
            }
            case CHOOSE_BRANCH -> {
                if (data.chooseBranch(spell, payload.level(), payload.branch())) {
                    player.sendSystemMessage(Component.translatable("message.elementalarcana.branch_chosen",
                                    spell.displayName(), spell.branchName(payload.branch()))
                            .withStyle(style -> style.withColor(spell.school().color())));
                    celebrate(player, 1.0f);
                }
            }
            case RESPEC -> {
                long gameTime = player.level().getGameTime();
                if (whyNoRespec(data, spell, gameTime) == null) {
                    data.setMana(0);
                    data.respec(spell, gameTime, RESPEC_COOLDOWN_TICKS);
                    player.sendSystemMessage(Component.translatable("message.elementalarcana.respec.done", spell.displayName())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    celebrate(player, 0.7f);
                }
            }
        }
        MagicAttachments.sync(player);
    }

    private static void celebrate(ServerPlayer player, float pitch) {
        player.playNotifySound(ModContent.LEVEL_UP_SOUND.get(), SoundSource.PLAYERS, 0.9f, pitch);
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.6, 0.4, 0.04);
    }
}
