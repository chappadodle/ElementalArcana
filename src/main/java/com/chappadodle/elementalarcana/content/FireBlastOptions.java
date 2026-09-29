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
 * A fireball's explosion, sent as one particle: each client plays the whole blast out itself
 * (see FireBlastEmitter and FireballEffects#blast). {@code look} is the fireball's look
 * (SpellProjectile#variant), {@code radius} the blast radius in blocks, and {@code bomblet} marks
 * a Cluster Bomb bomblet's small pop.
 */
public record FireBlastOptions(int look, float radius, boolean bomblet) implements ParticleOptions {
    public static final MapCodec<FireBlastOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("look").forGetter(FireBlastOptions::look),
            Codec.FLOAT.fieldOf("radius").forGetter(FireBlastOptions::radius),
            Codec.BOOL.optionalFieldOf("bomblet", false).forGetter(FireBlastOptions::bomblet)
    ).apply(instance, FireBlastOptions::new));
    public static final StreamCodec<ByteBuf, FireBlastOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FireBlastOptions::look,
            ByteBufCodecs.FLOAT, FireBlastOptions::radius,
            ByteBufCodecs.BOOL, FireBlastOptions::bomblet,
            FireBlastOptions::new);

    @Override
    public ParticleType<FireBlastOptions> getType() {
        return ModContent.FIRE_BLAST.get();
    }

    /** The particle type: seen from far away (explosions are big). */
    public static ParticleType<FireBlastOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<FireBlastOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, FireBlastOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
