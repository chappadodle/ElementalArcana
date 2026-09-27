package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** A chip of ice from a shatter: flies out, tumbles, falls and settles on the ground, then fades. */
public class IceShardParticle extends TextureSheetParticle {
    private final float spin;

    protected IceShardParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        // Kick shards up a little so a burst sprays outward and rains down instead of sinking.
        this.xd = xd;
        this.yd = yd + 0.08 + random.nextFloat() * 0.08;
        this.zd = zd;
        this.gravity = 1.0f;
        this.friction = 0.96f;
        this.hasPhysics = true;
        this.lifetime = 22 + random.nextInt(18);
        this.quadSize = 0.05f + random.nextFloat() * 0.06f;
        this.spin = (random.nextFloat() - 0.5f) * 0.6f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
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
        alpha = life < 0.7f ? 1f : 1f - (life - 0.7f) / 0.3f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new IceShardParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
