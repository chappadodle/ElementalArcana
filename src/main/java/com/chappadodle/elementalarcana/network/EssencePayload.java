package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.EssenceService;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.joml.Vector3f;


/**
 * Client -> server: spend Elemental Essence. INFUSE puts Essence of a spell's element into its
 * mastery bar (one, or just enough to fill it); CONDENSE turns Essence of any elements into a
 * bonus skill point. The server checks every rule and takes the items itself.
 */
public record EssencePayload(Action action, ResourceLocation spell, boolean fill) implements CustomPacketPayload {
    public static final Type<EssencePayload> TYPE = new Type<>(ElementalArcana.id("essence"));
    public static final StreamCodec<ByteBuf, EssencePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Action.values()[ordinal], Action::ordinal), EssencePayload::action,
            ResourceLocation.STREAM_CODEC, EssencePayload::spell,
            ByteBufCodecs.BOOL, EssencePayload::fill,
            EssencePayload::new);

    public enum Action {
        INFUSE, CONDENSE
    }

    public static EssencePayload infuse(Spell spell, boolean fill) {
        return new EssencePayload(Action.INFUSE, spell.id(), fill);
    }

    public static EssencePayload condense() {
        return new EssencePayload(Action.CONDENSE, ElementalArcana.id("none"), false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EssencePayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        switch (payload.action()) {
            case INFUSE -> infuse(player, data, SpellRegistries.SPELLS.get(payload.spell()), payload.fill());
            case CONDENSE -> condense(player, data);
        }
        MagicAttachments.sync(player);
    }

    private static void infuse(ServerPlayer player, MagicData data, Spell spell, boolean fill) {
        if (spell == null || !data.canCast(spell)) {
            return;
        }
        Element element = EssenceService.elementOf(spell);
        if (element == null) {
            return;
        }
        int count = EssenceService.infuseCount(data, spell, fill, EssenceService.count(player, element));
        int each = EssenceService.infuseAmount(data, spell);
        if (count <= 0 || each <= 0) {
            return;
        }
        int taken = EssenceService.remove(player, element, count);
        data.addMastery(spell, taken * each);
        int color = element.color();
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.0f);
        player.serverLevel().sendParticles(dust, player.getX(), player.getY(1.0), player.getZ(), 16, 0.4, 0.5, 0.4, 0.05);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
    }

    private static void condense(ServerPlayer player, MagicData data) {
        int cost = Progression.condenseCost(data.bonusTreePoints());
        if (EssenceService.total(player) < cost) {
            return;
        }
        EssenceService.removeAny(player, cost);
        data.addBonusTreePoint();
        player.sendSystemMessage(Component.translatable("message.elementalarcana.condensed", cost).withStyle(ChatFormatting.LIGHT_PURPLE));
        player.playNotifySound(ModContent.LEVEL_UP_SOUND.get(), SoundSource.PLAYERS, 0.9f, 1.2f);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 20, 0.4, 0.6, 0.4, 0.04);
    }
}
