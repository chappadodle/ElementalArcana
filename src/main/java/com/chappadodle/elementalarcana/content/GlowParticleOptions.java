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
import net.minecraft.world.entity.Entity;

/**
 * Settings for the glowing particles (Flare, Spark, Feather): the colour a particle starts with and
 * the colour it fades to (0xRRGGBB), its size in blocks, its lifetime in ticks, and optionally an
 * entity it's anchored to. An anchored particle's position and motion are relative to that entity
 * (a held projectile: its place in the caster's hand), so it stays with it as the caster turns.
 */
public record GlowParticleOptions(ParticleType<GlowParticleOptions> type, int color, int fadeColor, float size,
                                  int lifetime, int anchor) implements ParticleOptions {
    public static final int NO_ANCHOR = -1;

    public static GlowParticleOptions of(ParticleType<GlowParticleOptions> type, int color, int fadeColor, float size, int lifetime) {
        return new GlowParticleOptions(type, color, fadeColor, size, lifetime, NO_ANCHOR);
    }

    /** The same particle, anchored to {@code entity}. */
    public GlowParticleOptions anchoredTo(Entity entity) {
        return new GlowParticleOptions(type, color, fadeColor, size, lifetime, entity.getId());
    }

    @Override
    public ParticleType<GlowParticleOptions> getType() {
        return type;
    }

    /** A new particle type that uses these options. */
    public static ParticleType<GlowParticleOptions> newType() {
        return new ParticleType<>(false) {
            @Override
            public MapCodec<GlowParticleOptions> codec() {
                return GlowParticleOptions.codec(this);
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, GlowParticleOptions> streamCodec() {
                return GlowParticleOptions.streamCodec(this);
            }
        };
    }

    private static MapCodec<GlowParticleOptions> codec(ParticleType<GlowParticleOptions> type) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("color").forGetter(GlowParticleOptions::color),
                Codec.INT.fieldOf("fade_color").forGetter(GlowParticleOptions::fadeColor),
                Codec.FLOAT.fieldOf("size").forGetter(GlowParticleOptions::size),
                Codec.INT.fieldOf("lifetime").forGetter(GlowParticleOptions::lifetime),
                Codec.INT.optionalFieldOf("anchor", NO_ANCHOR).forGetter(GlowParticleOptions::anchor)
        ).apply(instance, (color, fadeColor, size, lifetime, anchor) -> new GlowParticleOptions(type, color, fadeColor, size, lifetime, anchor)));
    }

    private static StreamCodec<ByteBuf, GlowParticleOptions> streamCodec(ParticleType<GlowParticleOptions> type) {
        return StreamCodec.composite(
                ByteBufCodecs.INT, GlowParticleOptions::color,
                ByteBufCodecs.INT, GlowParticleOptions::fadeColor,
                ByteBufCodecs.FLOAT, GlowParticleOptions::size,
                ByteBufCodecs.VAR_INT, GlowParticleOptions::lifetime,
                ByteBufCodecs.VAR_INT, options -> options.anchor + 1,
                (color, fadeColor, size, lifetime, anchor) -> new GlowParticleOptions(type, color, fadeColor, size, lifetime, anchor - 1));
    }
}
