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
 * A big water effect, sent as one particle and drawn on each client out of little water cubes (see
 * WaterBurstParticle): a whirlpool (Maelstrom, a Water Archmage's) or a crashing wave ring (where a
 * Tsunami Lance lands). {@code radius} is in blocks, {@code ticks} how long it lasts.
 */
public record WaterBurstOptions(int kind, float radius, int ticks) implements ParticleOptions {
    public static final int WHIRLPOOL = 0;
    public static final int WAVE = 1;

    public static final MapCodec<WaterBurstOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("kind").forGetter(WaterBurstOptions::kind),
            Codec.FLOAT.fieldOf("radius").forGetter(WaterBurstOptions::radius),
            Codec.INT.fieldOf("ticks").forGetter(WaterBurstOptions::ticks)
    ).apply(instance, WaterBurstOptions::new));
    public static final StreamCodec<ByteBuf, WaterBurstOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WaterBurstOptions::kind,
            ByteBufCodecs.FLOAT, WaterBurstOptions::radius,
            ByteBufCodecs.VAR_INT, WaterBurstOptions::ticks,
            WaterBurstOptions::new);

    @Override
    public ParticleType<WaterBurstOptions> getType() {
        return ModContent.WATER_BURST.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<WaterBurstOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<WaterBurstOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, WaterBurstOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
