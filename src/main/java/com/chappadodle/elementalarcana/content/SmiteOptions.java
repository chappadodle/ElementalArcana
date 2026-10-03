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
 * Smite's light, sent as one particle on the ground where it is (see SmiteParticle): a {@link #MARK}
 * brightening for {@code life} ticks before the pillar falls, the {@link #PILLAR} itself, a
 * Sunlance's steady {@link #BEAM}, or consecrated {@link #GROUND}; {@code radius} is how wide it is.
 */
public record SmiteOptions(int kind, float radius, int life) implements ParticleOptions {
    public static final int MARK = 0;
    public static final int PILLAR = 1;
    public static final int BEAM = 2;
    public static final int GROUND = 3;

    public static final MapCodec<SmiteOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("kind").forGetter(SmiteOptions::kind),
            Codec.FLOAT.fieldOf("radius").forGetter(SmiteOptions::radius),
            Codec.INT.fieldOf("life").forGetter(SmiteOptions::life)
    ).apply(instance, SmiteOptions::new));
    public static final StreamCodec<ByteBuf, SmiteOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SmiteOptions::kind,
            ByteBufCodecs.FLOAT, SmiteOptions::radius,
            ByteBufCodecs.VAR_INT, SmiteOptions::life,
            SmiteOptions::new);

    @Override
    public ParticleType<SmiteOptions> getType() {
        return ModContent.SMITE.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<SmiteOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<SmiteOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, SmiteOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
