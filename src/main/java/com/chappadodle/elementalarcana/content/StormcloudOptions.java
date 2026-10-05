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
 * Stormcall's cloud, sent as one particle when it's called: each client draws it over
 * {@code caster} (an entity id) for {@code duration} ticks itself, rain and all (see
 * StormcloudParticle; the strikes are the server's, see Stormcalls).
 */
public record StormcloudOptions(int caster, int duration) implements ParticleOptions {
    public static final MapCodec<StormcloudOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("caster").forGetter(StormcloudOptions::caster),
            Codec.INT.fieldOf("duration").forGetter(StormcloudOptions::duration)
    ).apply(instance, StormcloudOptions::new));
    public static final StreamCodec<ByteBuf, StormcloudOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StormcloudOptions::caster,
            ByteBufCodecs.VAR_INT, StormcloudOptions::duration,
            StormcloudOptions::new);

    @Override
    public ParticleType<StormcloudOptions> getType() {
        return ModContent.STORMCLOUD.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<StormcloudOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<StormcloudOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, StormcloudOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
