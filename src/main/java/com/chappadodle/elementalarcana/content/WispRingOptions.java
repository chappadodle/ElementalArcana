package com.chappadodle.elementalarcana.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A Wisp Ring's dancing lights for {@code ticks} ticks, sent as one particle over the ring's middle
 * (see content/world/WispRings): the client draws them circling (WispRingEmitter).
 */
public record WispRingOptions(int ticks) implements ParticleOptions {
    public static final MapCodec<WispRingOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("ticks").forGetter(WispRingOptions::ticks)
    ).apply(instance, WispRingOptions::new));
    public static final StreamCodec<ByteBuf, WispRingOptions> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(WispRingOptions::new, WispRingOptions::ticks);

    @Override
    public ParticleType<WispRingOptions> getType() {
        return ModContent.WISP_RING.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<WispRingOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<WispRingOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, WispRingOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
