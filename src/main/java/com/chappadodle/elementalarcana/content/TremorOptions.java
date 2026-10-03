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
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * A Tremor's shockwave, sent as one particle where it starts (its foot's height): each client
 * draws its front running along the same path, a block a tick (see TremorEmitter; the hits are the
 * server's, see Tremors). {@code (dx, dz)} is the way it runs, and {@code rise} its foot's height at
 * each block of the path (TremorRules), less the first.
 */
public record TremorOptions(float dx, float dz, List<Integer> rise) implements ParticleOptions {
    public static final MapCodec<TremorOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("dx").forGetter(TremorOptions::dx),
            Codec.FLOAT.fieldOf("dz").forGetter(TremorOptions::dz),
            Codec.INT.listOf().fieldOf("rise").forGetter(TremorOptions::rise)
    ).apply(instance, TremorOptions::new));
    public static final StreamCodec<ByteBuf, TremorOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TremorOptions::dx,
            ByteBufCodecs.FLOAT, TremorOptions::dz,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), TremorOptions::rise,
            TremorOptions::new);

    public static TremorOptions of(Vec3 direction, int[] heights) {
        return new TremorOptions((float) direction.x, (float) direction.z, Arrays.stream(heights).map(y -> y - heights[0]).boxed().toList());
    }

    @Override
    public ParticleType<TremorOptions> getType() {
        return ModContent.TREMOR.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<TremorOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<TremorOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, TremorOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
