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

/**
 * A bolt of lightning, sent as one particle where it starts: each client draws it, jagged and
 * flickering, to {@code (dx, dy, dz)} away (see LightningArcParticle). Nothing is drawn of the
 * first {@code skip} blocks (so a caster's own bolt doesn't balloon in front of their eyes);
 * {@code width} thickens it (1 for a spell's bolt; a bolt from the sky is thicker and flashes the sky).
 */
public record ArcOptions(float dx, float dy, float dz, float skip, float width) implements ParticleOptions {
    public static final MapCodec<ArcOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.FLOAT.fieldOf("dx").forGetter(ArcOptions::dx),
            Codec.FLOAT.fieldOf("dy").forGetter(ArcOptions::dy),
            Codec.FLOAT.fieldOf("dz").forGetter(ArcOptions::dz),
            Codec.FLOAT.fieldOf("skip").forGetter(ArcOptions::skip),
            Codec.FLOAT.fieldOf("width").forGetter(ArcOptions::width)
    ).apply(instance, ArcOptions::new));
    public static final StreamCodec<ByteBuf, ArcOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, ArcOptions::dx,
            ByteBufCodecs.FLOAT, ArcOptions::dy,
            ByteBufCodecs.FLOAT, ArcOptions::dz,
            ByteBufCodecs.FLOAT, ArcOptions::skip,
            ByteBufCodecs.FLOAT, ArcOptions::width,
            ArcOptions::new);

    /** A bolt from {@code from} to {@code to}. */
    public static ArcOptions between(Vec3 from, Vec3 to, double skip, float width) {
        Vec3 line = to.subtract(from);
        return new ArcOptions((float) line.x, (float) line.y, (float) line.z, (float) skip, width);
    }

    @Override
    public ParticleType<ArcOptions> getType() {
        return ModContent.ARC.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<ArcOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<ArcOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, ArcOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
