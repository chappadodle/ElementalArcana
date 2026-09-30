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
 * An icicle shattering, sent as one particle: each client plays the whole shatter out itself (see
 * IceShatterEmitter and IcicleEffects#shatter). {@code look} is the icicle's look with its Lv 8+
 * brightness (SpellProjectile#variant), {@code size} its visual scale (the Lance is big, shrapnel
 * tiny) and {@code charge} how grown it was.
 */
public record IceShatterOptions(int look, float size, float charge) implements ParticleOptions {
    public static final MapCodec<IceShatterOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("look").forGetter(IceShatterOptions::look),
            Codec.FLOAT.fieldOf("size").forGetter(IceShatterOptions::size),
            Codec.FLOAT.fieldOf("charge").forGetter(IceShatterOptions::charge)
    ).apply(instance, IceShatterOptions::new));
    public static final StreamCodec<ByteBuf, IceShatterOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IceShatterOptions::look,
            ByteBufCodecs.FLOAT, IceShatterOptions::size,
            ByteBufCodecs.FLOAT, IceShatterOptions::charge,
            IceShatterOptions::new);

    @Override
    public ParticleType<IceShatterOptions> getType() {
        return ModContent.ICE_SHATTER.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<IceShatterOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<IceShatterOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, IceShatterOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
