package com.chappadodle.elementalarcana.network;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: awaken an element into a free affinity slot (the server re-checks the slot). */
public record AwakenPayload(ResourceLocation school) implements CustomPacketPayload {
    public static final Type<AwakenPayload> TYPE = new Type<>(ElementalArcana.id("awaken"));
    public static final StreamCodec<ByteBuf, AwakenPayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, AwakenPayload::school, AwakenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AwakenPayload payload, IPayloadContext context) {
        SpellSchool school = SpellRegistries.SCHOOLS.get(payload.school());
        if (!(context.player() instanceof ServerPlayer player) || school == null || !MagicAttachments.get(player).awaken(school)) {
            return;
        }
        MagicAttachments.sync(player);

        Component name = school.displayName().copy().withStyle(style -> style.withColor(school.color()));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.elementalarcana.awakened")
                .withStyle(style -> style.withColor(school.color()))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.elementalarcana.awakened.sub", name)));
        player.playNotifySound(ModContent.AWAKEN_SOUND.get(), SoundSource.PLAYERS, 1f, 1f);
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
    }
}
