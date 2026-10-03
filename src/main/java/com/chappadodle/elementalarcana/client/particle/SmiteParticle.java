package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.api.SmiteRules;
import com.chappadodle.elementalarcana.content.SmiteOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Smite's light (see SmiteOptions), drawn additively in white-gold, blocky like a beacon's beam:
 * <ul>
 * <li>the mark: a ring of light on the ground with short shafts rising from it, brightening until
 * the pillar falls;</li>
 * <li>the pillar: a column of light, a white core in a wide gold glow, dropping out of the sky in
 * two ticks, a ring flashing out where it lands, then narrowing away;</li>
 * <li>a Sunlance's beam: the column held steady (sent again every other tick as it moves);</li>
 * <li>consecrated ground: a faint ring and a soft glow on the ground, motes of light rising.</li>
 * </ul>
 */
public class SmiteParticle extends Particle {
    private static final float GOLD_R = 1f;
    private static final float GOLD_G = 0.82f;
    private static final float GOLD_B = 0.4f;
    private static final int SHAFTS = 12;
    private static final int FALL_TICKS = 2;

    private final int kind;
    private final float radius;
    private final float u;
    private final float v;

    protected SmiteParticle(ClientLevel level, double x, double y, double z, SmiteOptions options, TextureAtlasSprite white) {
        super(level, x, y, z);
        this.kind = options.kind();
        this.radius = options.radius();
        this.lifetime = Math.max(1, options.life());
        this.gravity = 0f;
        this.hasPhysics = false;
        // The flare's middle is solid white: everything is drawn from it, tinted.
        this.u = white.getU(0.5f);
        this.v = white.getV(0.5f);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        if (kind == SmiteOptions.GROUND && random.nextInt(2) == 0) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = radius * Math.sqrt(random.nextDouble());
            level.addParticle(ParticleTypes.END_ROD, x + Math.cos(angle) * distance, y + 0.1, z + Math.sin(angle) * distance, 0, 0.03, 0);
        }
        if (kind == SmiteOptions.PILLAR && age == FALL_TICKS) {
            // Where it lands: a burst of motes.
            for (int i = 0; i < 24; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                double speed = 0.1 + random.nextDouble() * 0.2;
                level.addParticle(ParticleTypes.END_ROD, x, y + 0.2, z, Math.cos(angle) * speed, 0.05 + random.nextDouble() * 0.1, Math.sin(angle) * speed);
            }
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cameraPos = camera.getPosition();
        Vector3f foot = new Vector3f((float) (x - cameraPos.x), (float) (y - cameraPos.y), (float) (z - cameraPos.z));
        float time = age + partialTicks;
        float life = Mth.clamp(time / lifetime, 0f, 1f);
        switch (kind) {
            case SmiteOptions.MARK -> mark(buffer, foot, life);
            case SmiteOptions.PILLAR -> pillar(buffer, foot, time, life);
            case SmiteOptions.BEAM -> column(buffer, foot, 0f, radius * 0.4f, 0.8f);
            default -> ground(buffer, foot, time, life);
        }
    }

    /** The mark: a ring on the ground and shafts rising from it, brighter and taller as the pillar nears. */
    private void mark(VertexConsumer buffer, Vector3f foot, float life) {
        float alpha = 0.35f + 0.65f * life;
        Vector3f ground = new Vector3f(foot).add(0, 0.06f, 0);
        GlowMesh.ring(buffer, u, v, ground, radius - 0.09f, radius + 0.09f, 32, GOLD_R, GOLD_G, GOLD_B, alpha);
        GlowMesh.ring(buffer, u, v, ground, radius - 0.03f, radius + 0.03f, 32, 1f, 1f, 0.9f, alpha);
        float rise = 0.3f + 1.2f * life;
        float turn = (age % 40) * 0.05f;
        for (int i = 0; i < SHAFTS; i++) {
            float angle = Mth.TWO_PI * i / SHAFTS + turn;
            Vector3f base = new Vector3f(foot).add(Mth.cos(angle) * radius, 0, Mth.sin(angle) * radius);
            GlowMesh.bar(buffer, u, v, base, new Vector3f(base).add(0, rise, 0), 0.035f, GOLD_R, GOLD_G, GOLD_B, alpha * 0.8f);
        }
    }

    /** The pillar: falling from the sky, a ring flashing out where it lands, then narrowing away. */
    private void pillar(VertexConsumer buffer, Vector3f foot, float time, float life) {
        float landed = Math.min(1f, time / FALL_TICKS);
        // Its foot drops from the top of the column to the ground.
        float bottom = (float) SmiteRules.HEIGHT * (1f - landed);
        float after = Math.max(0f, (time - FALL_TICKS) / Math.max(1, lifetime - FALL_TICKS));
        float width = radius * 0.55f * (1f - after * after);
        column(buffer, foot, bottom, width, 1f - after * 0.6f);
        if (time >= FALL_TICKS) {
            float flash = Math.max(0f, 1f - (time - FALL_TICKS) / 5f);
            if (flash > 0f) {
                float r = radius * (1f + 0.25f * (1f - flash));
                GlowMesh.ring(buffer, u, v, new Vector3f(foot).add(0, 0.08f, 0), r * 0.6f, r, 32, GOLD_R, GOLD_G, GOLD_B, flash * 0.8f);
            }
        }
    }

    /** A column of light from {@code bottom} above the foot to the pillar's height: a white core in a gold glow. */
    private void column(VertexConsumer buffer, Vector3f foot, float bottom, float width, float alpha) {
        if (width <= 0.01f || alpha <= 0f) {
            return;
        }
        Vector3f a = new Vector3f(foot).add(0, bottom, 0);
        Vector3f b = new Vector3f(foot).add(0, (float) SmiteRules.HEIGHT, 0);
        GlowMesh.bar(buffer, u, v, a, b, width, GOLD_R, GOLD_G, GOLD_B, alpha * 0.3f);
        GlowMesh.bar(buffer, u, v, a, b, width * 0.55f, GOLD_R, 0.9f, 0.6f, alpha * 0.45f);
        GlowMesh.bar(buffer, u, v, a, b, width * 0.25f, 1f, 1f, 0.92f, alpha);
    }

    /** Consecrated ground: a faint ring, pulsing, fading at the end. */
    private void ground(VertexConsumer buffer, Vector3f foot, float time, float life) {
        float alpha = (0.35f + 0.15f * Mth.sin(time * 0.3f)) * Math.min(1f, (1f - life) * 4f);
        Vector3f ground = new Vector3f(foot).add(0, 0.05f, 0);
        GlowMesh.ring(buffer, u, v, ground, radius - 0.1f, radius + 0.05f, 32, GOLD_R, GOLD_G, GOLD_B, alpha);
        GlowMesh.ring(buffer, u, v, ground, 0f, radius - 0.1f, 32, GOLD_R, GOLD_G, GOLD_B, alpha * 0.18f);
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x - radius - 1, y - 1, z - radius - 1, x + radius + 1, y + SmiteRules.HEIGHT + 1, z + radius + 1);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticles.RENDER_TYPE;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SmiteOptions> {
        @Override
        public Particle createParticle(SmiteOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new SmiteParticle(level, x, y, z, options, sprites.get(0, 1));
        }
    }
}
