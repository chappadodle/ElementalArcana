package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.core.PlayerStats;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Client -> server: one dev-menu button press. The server re-checks op permission (level 2),
 * so the menu is only a convenience - it grants nothing a cheating client couldn't already do.
 */
public record DevActionPayload(Action action, int value, String target) implements CustomPacketPayload {
    public static final Type<DevActionPayload> TYPE = new Type<>(ElementalArcana.id("dev_action"));
    public static final StreamCodec<ByteBuf, DevActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> Action.values()[ordinal], Action::ordinal), DevActionPayload::action,
            ByteBufCodecs.VAR_INT, DevActionPayload::value,
            ByteBufCodecs.STRING_UTF8, DevActionPayload::target,
            DevActionPayload::new);

    public enum Action {
        ADD_LEVELS, SET_LEVEL, ADD_XP, SET_MANA_PERCENT, GIVE_SICKNESS, CURE_SICKNESS,
        TOGGLE_AFFINITY, RESET_AFFINITIES, RESET_COOLDOWNS, TOGGLE_FREE_CAST, HEAL,
        MAX_SPELL, FILL_MASTERY, RESET_TREE, SPAWN_ATTUNED, RESET_STATS
    }

    public static DevActionPayload of(Action action, int value) {
        return new DevActionPayload(action, value, "");
    }

    public static DevActionPayload toggleAffinity(SpellSchool school) {
        return new DevActionPayload(Action.TOGGLE_AFFINITY, 0, school.id().toString());
    }

    /** An action on one spell (level, mastery, branches). */
    public static DevActionPayload forSpell(Action action, Spell spell, int value) {
        return new DevActionPayload(action, value, spell.id().toString());
    }

    /** Spawn a zombie Attuned to {@code element} at {@code rank}. */
    public static DevActionPayload spawnAttuned(Element element, AttunementRank rank) {
        return new DevActionPayload(Action.SPAWN_ATTUNED, rank.ordinal(), element.name());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DevActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player) || !player.hasPermissions(2)) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        switch (payload.action()) {
            case ADD_LEVELS -> data.setLevel(data.level() + payload.value());
            case SET_LEVEL -> data.setLevel(payload.value());
            case ADD_XP -> data.addXp(payload.value());
            case SET_MANA_PERCENT -> data.setMana(data.maxMana() * payload.value() / 100f);
            case GIVE_SICKNESS -> player.addEffect(new MobEffectInstance(ModContent.MANA_SICKNESS, 15 * 20));
            case CURE_SICKNESS -> player.removeEffect(ModContent.MANA_SICKNESS);
            case TOGGLE_AFFINITY -> {
                ResourceLocation id = ResourceLocation.tryParse(payload.target());
                SpellSchool school = id == null ? null : SpellRegistries.SCHOOLS.get(id);
                if (school != null) {
                    if (data.hasAffinity(school)) {
                        data.removeAffinity(school);
                    } else {
                        data.forceAffinity(school);
                    }
                }
            }
            case RESET_AFFINITIES -> data.clearAffinities();
            case RESET_STATS -> data.resetStats();
            case RESET_COOLDOWNS -> data.clearCooldowns();
            case TOGGLE_FREE_CAST -> data.setFreeCast(!data.freeCast());
            case HEAL -> player.setHealth(player.getMaxHealth());
            case RESET_TREE -> data.resetTree();
            case MAX_SPELL, FILL_MASTERY -> {
                ResourceLocation id = ResourceLocation.tryParse(payload.target());
                Spell spell = id == null ? null : SpellRegistries.SPELLS.get(id);
                if (spell != null) {
                    switch (payload.action()) {
                        case MAX_SPELL -> data.takeWholePath(spell);
                        default -> data.fillMastery(spell);
                    }
                }
            }
            case SPAWN_ATTUNED -> {
                Element element = parseElement(payload.target());
                int rank = payload.value();
                if (element != null && rank >= 0 && rank < AttunementRank.values().length) {
                    Attunement.spawnForTesting(player, element, AttunementRank.values()[rank]);
                }
            }
        }
        PlayerStats.apply(player);
        MagicAttachments.sync(player);
    }

    @Nullable
    private static Element parseElement(String name) {
        try {
            return Element.valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
