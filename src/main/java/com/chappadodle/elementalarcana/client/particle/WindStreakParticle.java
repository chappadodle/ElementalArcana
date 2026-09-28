package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A curl of wind: glides along its velocity, slowing down and turning slightly, and fades out.
 * Pale green-white by default; the Swirl version is tinted with the swirled element's color.
 */
public class WindStreakParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float spin;

    protected WindStreakParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.friction = 0.9f;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.lifetime = 12 + random.nextInt(8);
        this.quadSize = 0.12f + random.nextFloat() * 0.1f;
        this.roll = random.nextFloat() * 6.28f;
        this.oRoll = roll;
        this.spin = (random.nextFloat() - 0.5f) * 0.3f;
        float shade = 0.85f + random.nextFloat() * 0.15f;
        setColor(0.85f * shade, shade, 0.92f * shade);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        oRoll = roll;
        roll += spin;
        setSpriteFromAge(sprites);
        float life = age / (float) lifetime;
        alpha = life < 0.5f ? 0.9f : 0.9f * (1f - (life - 0.5f) / 0.5f);
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
            return new WindStreakParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }

    /** The Swirl version: same wind curl, tinted with the element's color. */
    public record SwirlProvider(SpriteSet sprites) implements ParticleProvider<ColorParticleOption> {
        @Override
        public Particle createParticle(ColorParticleOption color, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            WindStreakParticle particle = new WindStreakParticle(level, x, y, z, xd, yd, zd, sprites);
            particle.setColor(color.getRed(), color.getGreen(), color.getBlue());
            particle.quadSize *= 1.4f;
            return particle;
        }
    }
}
