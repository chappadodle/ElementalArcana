package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.client.sound.IcicleSounds;
import com.chappadodle.elementalarcana.content.IceShatterOptions;
import com.chappadodle.elementalarcana.content.spell.IcicleEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.world.phys.Vec3;

/**
 * An icicle shattering, played out over a few ticks on this client (the server sends it as one
 * particle, see IceShatterOptions): each tick asks IcicleEffects#shatter for that moment's pieces.
 */
public class IceShatterEmitter extends NoRenderParticle {
    private final IceShatterOptions shatter;

    protected IceShatterEmitter(ClientLevel level, double x, double y, double z, IceShatterOptions shatter) {
        super(level, x, y, z);
        this.shatter = shatter;
        this.lifetime = IcicleEffects.SHATTER_TICKS;
        IcicleSounds.shatter(level, new Vec3(x, y, z), shatter.look(), shatter.size(), shatter.charge());
    }

    @Override
    public void tick() {
        IcicleEffects.shatter(level, new Vec3(x, y, z), shatter.look(), shatter.size(), shatter.charge(), age);
        if (++age >= lifetime) {
            remove();
        }
    }

    public static class Provider implements ParticleProvider<IceShatterOptions> {
        @Override
        public Particle createParticle(IceShatterOptions shatter, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new IceShatterEmitter(level, x, y, z, shatter);
        }
    }
}
