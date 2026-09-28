package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/** A glowing spark: drifts upward, cools from yellow to deep orange, and winks out. */
public class EmberParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected EmberParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd + (random.nextDouble() - 0.5) * 0.02;
        this.yd = yd;
        this.zd = zd + (random.nextDouble() - 0.5) * 0.02;
        this.friction = 0.94f;
        this.gravity = -0.03f;
        this.hasPhysics = false;
        this.lifetime = 12 + random.nextInt(10);
        this.quadSize = 0.04f + random.nextFloat() * 0.04f;
        setColor(1f, 0.9f, 0.45f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = age / (float) lifetime;
        // Cool from yellow-white to orange-red as it rises.
        setColor(1f, 0.9f - 0.5f * life, 0.45f - 0.35f * life);
        alpha = life < 0.6f ? 1f : 1f - (life - 0.6f) / 0.4f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new EmberParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
