package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A glowing 4-point glint that shrinks through its frames and fades. Always full-bright, so it
 * reads as magic light even at night. Spawned with a velocity, it glides and slows to a stop.
 */
public class FrostSparkleParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected FrostSparkleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.friction = 0.86f;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.lifetime = 10 + random.nextInt(8);
        this.quadSize = 0.06f + random.nextFloat() * 0.07f;
        float tint = random.nextFloat();
        setColor(0.72f + 0.28f * tint, 0.9f + 0.1f * tint, 1f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = age / (float) lifetime;
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
            return new FrostSparkleParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
