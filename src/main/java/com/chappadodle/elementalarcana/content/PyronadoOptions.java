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
 * Pyronado's wheels, sent as one particle when they're called: each client draws them around
 * {@code caster} (an entity id) for {@code duration} ticks itself (see PyronadoEmitter; the hits are
 * the server's, see Pyronados).
 */
public record PyronadoOptions(int caster, int duration) implements ParticleOptions {
    public static final MapCodec<PyronadoOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("caster").forGetter(PyronadoOptions::caster),
            Codec.INT.fieldOf("duration").forGetter(PyronadoOptions::duration)
    ).apply(instance, PyronadoOptions::new));
    public static final StreamCodec<ByteBuf, PyronadoOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PyronadoOptions::caster,
            ByteBufCodecs.VAR_INT, PyronadoOptions::duration,
            PyronadoOptions::new);

    @Override
    public ParticleType<PyronadoOptions> getType() {
        return ModContent.PYRONADO.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<PyronadoOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<PyronadoOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, PyronadoOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
