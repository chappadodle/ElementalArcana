package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A water droplet: flies along its velocity, sags under gravity, shrinks and fades. */
public class HydroDropParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected HydroDropParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.friction = 0.96f;
        this.gravity = 0.4f;
        this.hasPhysics = false;
        this.lifetime = 5 + random.nextInt(5);
        this.quadSize = 0.07f + random.nextFloat() * 0.07f;
        float shade = 0.85f + random.nextFloat() * 0.15f;
        setColor(0.8f * shade, 0.92f * shade, shade);
        this.alpha = 0.85f;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new HydroDropParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
