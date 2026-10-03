package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Bubble;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellShield;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class MagicAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ElementalArcana.MODID);

    // Synced only to the player who owns it - nobody else needs your mana.
    public static final Supplier<AttachmentType<MagicData>> MAGIC = ATTACHMENT_TYPES.register("magic",
            () -> AttachmentType.builder(MagicData::new)
                    .serialize(MagicData.CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, MagicData.STREAM_CODEC)
                    .build());

    // Synced to everyone who can see the player (so their shield's visuals show), never saved.
    public static final Supplier<AttachmentType<SpellShield>> SHIELD = ATTACHMENT_TYPES.register("shield",
            () -> AttachmentType.builder(SpellShield::new)
                    .sync(SpellShield.STREAM_CODEC)
                    .build());

    // A creature or player trapped in a Bubble Prison. Synced to everyone who can see it (they draw
    // the bubble; a trapped player's own client holds them in it), never saved.
    public static final Supplier<AttachmentType<Bubble>> BUBBLE = ATTACHMENT_TYPES.register("bubble",
            () -> AttachmentType.builder(() -> new Bubble(Vec3.ZERO, 0L, 0L))
                    .sync(Bubble.STREAM_CODEC)
                    .build());

    // When a creature's Frozen effect ends (game time). Synced to everyone who can see it, since
    // vanilla doesn't send mobs' effects to other players, so they see it encased in ice
    // (client/FrozenShells). Set and cleared by content/FrozenState; not saved.
    public static final Supplier<AttachmentType<Long>> FROZEN_UNTIL = ATTACHMENT_TYPES.register("frozen_until",
            () -> AttachmentType.builder(() -> 0L)
                    .sync(ByteBufCodecs.VAR_LONG)
                    .build());

    // A player gliding on Skyward Leap's wind (see SkywardLeaps; client/Gliding steers it). Synced to
    // everyone who can see them, so they're drawn leaning into the glide; never saved.
    public static final Supplier<AttachmentType<Boolean>> GLIDING = ATTACHMENT_TYPES.register("gliding",
            () -> AttachmentType.builder(() -> false)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    // An Attuned creature's element and rank. Only Attuned creatures have it (check hasData; the
    // default below is never read). Saved, and synced to everyone who can see the creature, so the
    // Jade tooltip can show its element.
    public static final Supplier<AttachmentType<CreatureMagic>> CREATURE_MAGIC = ATTACHMENT_TYPES.register("creature_magic",
            () -> AttachmentType.builder(() -> new CreatureMagic(Element.FIRE, AttunementRank.ADEPT))
                    .serialize(CreatureMagic.CODEC)
                    .sync(CreatureMagic.STREAM_CODEC)
                    .build());

    // A creature's level from the zone it was in on its first tick (an Attuned creature's rank adds
    // bonus levels on top; see CreatureLevels). Saved, and synced to everyone who can see it so the
    // Jade tooltip can show it. Players use MagicData's level instead.
    public static final Supplier<AttachmentType<Integer>> CREATURE_LEVEL = ATTACHMENT_TYPES.register("creature_level",
            () -> AttachmentType.builder(() -> 1)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    private MagicAttachments() {
    }

    public static MagicData get(Player player) {
        return player.getData(MAGIC);
    }

    public static void sync(ServerPlayer player) {
        player.syncData(MAGIC);
    }
}
