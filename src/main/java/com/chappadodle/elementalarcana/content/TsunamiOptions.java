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
 * A Tsunami's wave, sent as one particle when it rises (at the start of its path, its foot's
 * height): each client draws it rolling along the same path (see TsunamiParticle; the sweeping is
 * the server's, see Tsunamis). {@code (dx, dz)} is the way it rolls, and {@code rise} its foot's
 * height at each point of the path (TsunamiRules), less the first.
 */
public record TsunamiOptions(float dx, float dz, List<Integer> rise) implements ParticleOptions {
    public static final MapCodec<TsunamiOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("dx").forGetter(TsunamiOptions::dx),
            Codec.FLOAT.fieldOf("dz").forGetter(TsunamiOptions::dz),
            Codec.INT.listOf().fieldOf("rise").forGetter(TsunamiOptions::rise)
    ).apply(instance, TsunamiOptions::new));
    public static final StreamCodec<ByteBuf, TsunamiOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, TsunamiOptions::dx,
            ByteBufCodecs.FLOAT, TsunamiOptions::dz,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), TsunamiOptions::rise,
            TsunamiOptions::new);

    public static TsunamiOptions of(Vec3 direction, int[] heights) {
        return new TsunamiOptions((float) direction.x, (float) direction.z, Arrays.stream(heights).map(y -> y - heights[0]).boxed().toList());
    }

    @Override
    public ParticleType<TsunamiOptions> getType() {
        return ModContent.TSUNAMI.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<TsunamiOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<TsunamiOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, TsunamiOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
