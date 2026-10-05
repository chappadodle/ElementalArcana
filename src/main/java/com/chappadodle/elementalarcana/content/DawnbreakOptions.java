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
 * Dawnbreak's sun, sent as one particle (at the place it was called) when it's called: each client
 * draws it rising there and shining for {@code duration} ticks itself (see DawnSunParticle; what
 * it does is the server's, see Dawnbreaks).
 */
public record DawnbreakOptions(int duration) implements ParticleOptions {
    public static final MapCodec<DawnbreakOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("duration").forGetter(DawnbreakOptions::duration)
    ).apply(instance, DawnbreakOptions::new));
    public static final StreamCodec<ByteBuf, DawnbreakOptions> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(DawnbreakOptions::new, DawnbreakOptions::duration);

    @Override
    public ParticleType<DawnbreakOptions> getType() {
        return ModContent.DAWN_SUN.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<DawnbreakOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<DawnbreakOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, DawnbreakOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
