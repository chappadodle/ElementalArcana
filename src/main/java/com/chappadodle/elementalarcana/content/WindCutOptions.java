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
 * A Wind Blade striking something, sent as one particle: each client plays the impact out itself
 * (see WindCutEmitter and WindBladeEffects#impact). {@code look} is the blade's look with its Lv 8+
 * brightness, {@code size} its visual scale, {@code charge} how grown it was, {@code dx/dy/dz} the
 * way it was flying and {@code roll} its fan tilt (so the cut mark runs along the blade), and
 * {@code vortex} whether it left a Tempest Edge whirlwind.
 */
public record WindCutOptions(int look, float size, float charge, float dx, float dy, float dz, float roll, boolean vortex)
        implements ParticleOptions {
    public static final MapCodec<WindCutOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("look").forGetter(WindCutOptions::look),
            Codec.FLOAT.fieldOf("size").forGetter(WindCutOptions::size),
            Codec.FLOAT.fieldOf("charge").forGetter(WindCutOptions::charge),
            Codec.FLOAT.fieldOf("dx").forGetter(WindCutOptions::dx),
            Codec.FLOAT.fieldOf("dy").forGetter(WindCutOptions::dy),
            Codec.FLOAT.fieldOf("dz").forGetter(WindCutOptions::dz),
            Codec.FLOAT.fieldOf("roll").forGetter(WindCutOptions::roll),
            Codec.BOOL.fieldOf("vortex").forGetter(WindCutOptions::vortex)
    ).apply(instance, WindCutOptions::new));
    public static final StreamCodec<ByteBuf, WindCutOptions> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public WindCutOptions decode(ByteBuf buf) {
            return new WindCutOptions(ByteBufCodecs.VAR_INT.decode(buf), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean());
        }

        @Override
        public void encode(ByteBuf buf, WindCutOptions cut) {
            ByteBufCodecs.VAR_INT.encode(buf, cut.look());
            buf.writeFloat(cut.size());
            buf.writeFloat(cut.charge());
            buf.writeFloat(cut.dx());
            buf.writeFloat(cut.dy());
            buf.writeFloat(cut.dz());
            buf.writeFloat(cut.roll());
            buf.writeBoolean(cut.vortex());
        }
    };

    public static WindCutOptions of(int look, float size, float charge, Vec3 direction, float roll, boolean vortex) {
        return new WindCutOptions(look, size, charge, (float) direction.x, (float) direction.y, (float) direction.z, roll, vortex);
    }

    public Vec3 direction() {
        return new Vec3(dx, dy, dz);
    }

    @Override
    public ParticleType<WindCutOptions> getType() {
        return ModContent.WIND_CUT.get();
    }

    /** The particle type: seen from far away. */
    public static ParticleType<WindCutOptions> newType() {
        return new ParticleType<>(true) {
            @Override
            public MapCodec<WindCutOptions> codec() {
                return CODEC;
            }

            @Override
            public StreamCodec<? super RegistryFriendlyByteBuf, WindCutOptions> streamCodec() {
                return STREAM_CODEC;
            }
        };
    }
}
