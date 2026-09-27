package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A soft cold puff: swells to about 2.5x its size while drifting upward and fading away. */
public class FrostMistParticle extends TextureSheetParticle {
    private static final float START_ALPHA = 0.55f;

    private final SpriteSet sprites;
    private final float baseSize;

    protected FrostMistParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.friction = 0.9f;
        this.gravity = -0.01f;
        this.hasPhysics = false;
        this.lifetime = 20 + random.nextInt(12);
        this.baseSize = 0.25f + random.nextFloat() * 0.2f;
        this.quadSize = baseSize;
        this.alpha = START_ALPHA;
        this.roll = random.nextFloat() * 6.28f;
        this.oRoll = roll;
        setColor(0.9f, 0.96f, 1f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        setSpriteFromAge(sprites);
        float life = age / (float) lifetime;
        quadSize = baseSize * (1f + 1.5f * life);
        alpha = START_ALPHA * (1f - life);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new FrostMistParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
