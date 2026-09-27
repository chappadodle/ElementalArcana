package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellShield;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
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

    private MagicAttachments() {
    }

    public static MagicData get(Player player) {
        return player.getData(MAGIC);
    }

    public static void sync(ServerPlayer player) {
        player.syncData(MAGIC);
    }
}
