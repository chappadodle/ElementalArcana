package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.content.WindCutOptions;
import com.chappadodle.elementalarcana.content.spell.WindBladeEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.world.phys.Vec3;

/**
 * A Wind Blade striking something, played out over a few ticks on this client (the server sends it
 * as one particle, see WindCutOptions): each tick asks WindBladeEffects#impact for that moment's pieces.
 */
public class WindCutEmitter extends NoRenderParticle {
    private final WindCutOptions cut;

    protected WindCutEmitter(ClientLevel level, double x, double y, double z, WindCutOptions cut) {
        super(level, x, y, z);
        this.cut = cut;
        this.lifetime = WindBladeEffects.IMPACT_TICKS;
    }

    @Override
    public void tick() {
        WindBladeEffects.impact(level, new Vec3(x, y, z), cut, age);
        if (++age >= lifetime) {
            remove();
        }
    }

    public static class Provider implements ParticleProvider<WindCutOptions> {
        @Override
        public Particle createParticle(WindCutOptions cut, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new WindCutEmitter(level, x, y, z, cut);
        }
    }
}
