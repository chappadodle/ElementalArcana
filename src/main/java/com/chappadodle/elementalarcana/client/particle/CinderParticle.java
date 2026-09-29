package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/** A chip of dark rock with a glowing edge: falls, tumbles, bounces off the ground and fades. */
public class CinderParticle extends TextureSheetParticle {
    private final float spin;

    protected CinderParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.9f;
        this.friction = 0.97f;
        this.lifetime = 30 + random.nextInt(20);
        this.quadSize = 0.05f + random.nextFloat() * 0.05f;
        this.spin = (random.nextFloat() - 0.5f) * 0.5f;
        this.roll = oRoll = random.nextFloat() * 6.28f;
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        if (!onGround) {
            roll += spin;
        }
        float life = age / (float) lifetime;
        alpha = life < 0.75f ? 1f : 1f - (life - 0.75f) / 0.25f;
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
            return new CinderParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
