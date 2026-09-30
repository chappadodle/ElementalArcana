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
 * One tick of a Hydro Jet stream, sent as a particle whose "velocity" is the line from the
 * caster's hand to where the stream lands (see HydroStreamEmitter, which keeps the caster's water
 * beam up to date). {@code caster} is the spraying entity's id, {@code look} the stream's look with
 * its Lv 8+ brightness (HydroJetSpell), and {@code pressure} its Pressure Build, 0 to 1.
 */
public record HydroStreamOptions(int caster, int look, float pressure) implements ParticleOptions {
    public static final MapCodec<HydroStreamOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("caster").forGetter(HydroStreamOptions::caster),
            Codec.INT.fieldOf("look").forGetter(HydroStreamOptions::look),
            Codec.FLOAT.fieldOf("pressure").forGetter(HydroStreamOptions::pressure)
    ).apply(instance, HydroStreamOptions::new));
    public static final StreamCodec<ByteBuf, HydroStreamOptions> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HydroStreamOptions::caster,
            ByteBufCodecs.VAR_INT, HydroStreamOptions::look,
            ByteBufCodecs.FLOAT, HydroStreamOptions::pressure,
            HydroStreamOptions::new);

    @Override
    public ParticleType<HydroStreamOptions> getType() {
        return ModContent.HYDRO_STREAM.get();
    }

    /** The particle type: seen from far away (the beam is long). */
    public static ParticleType<HydroStreamOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<HydroStreamOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, HydroStreamOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
