package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.ModContent;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Tempest Edge whirlwind as a real 3D funnel: a twisting cone of wind, narrow at the ground and
 * widening as it rises, turning fast, fading in and out over its life, and throwing off wind curls
 * as it spins. Drawn additively from both sides (the pull itself is WindVortex, on the server).
 */
public class WindFunnelParticle extends Particle {
    private static final int RINGS = 8;
    private static final int SEGMENTS = 16;
    private static final int TILES_AROUND = 4;
    private static final float HEIGHT = 2.4f;
    private static final int TINT = 0x9ADCD8;

    private final TextureAtlasSprite sprite;

    public WindFunnelParticle(ClientLevel level, Vec3 base, int lifetime) {
        super(level, base.x, base.y, base.z);
        this.sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ElementalArcana.id("block/wind_funnel"));
        this.lifetime = lifetime;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.xd = this.yd = this.zd = 0;
    }

    /** How wide the funnel is at height fraction {@code t} (0 ground, 1 top). */
    private static float radius(float t) {
        return 0.25f + 1.2f * (float) Math.pow(t, 1.3);
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed) {
            // Curls flung off its rim.
            float t = random.nextFloat();
            float angle = age * 0.9f + random.nextFloat() * Mth.TWO_PI;
            float r = radius(t);
            Vec3 at = new Vec3(x + Mth.cos(angle) * r, y + t * HEIGHT, z + Mth.sin(angle) * r);
            Vec3 velocity = new Vec3(-Mth.sin(angle), 0.05, Mth.cos(angle)).scale(0.2);
            level.addParticle(ColorParticleOption.create(ModContent.SWIRL.get(), 0xFF000000 | TINT), at.x, at.y, at.z, velocity.x, velocity.y, velocity.z);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float time = age + partialTicks;
        float life = time / lifetime;
        // Fade in quickly, out over the last third.
        float strength = Math.min(1f, time / 3f) * (life < 0.66f ? 1f : Math.max(0f, 1f - (life - 0.66f) / 0.34f));
        if (strength <= 0f) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        float cx = (float) (x - cameraPos.x);
        float cy = (float) (y - cameraPos.y);
        float cz = (float) (z - cameraPos.z);
        float spin = time * 0.5f;
        int perTile = SEGMENTS / TILES_AROUND;
        for (int k = 0; k < RINGS; k++) {
            float t0 = k / (float) RINGS;
            float t1 = (k + 1) / (float) RINGS;
            // Brighter low down, where the wind is densest.
            float b0 = strength * (0.9f - 0.5f * t0);
            float b1 = strength * (0.9f - 0.5f * t1);
            float v0 = sprite.getV((k % 2) / 2f);
            float v1 = sprite.getV((k % 2 + 1) / 2f);
            for (int i = 0; i < SEGMENTS; i++) {
                // Each ring twists further round than the one below.
                float a0 = spin + Mth.TWO_PI * i / SEGMENTS;
                float a1 = spin + Mth.TWO_PI * (i + 1) / SEGMENTS;
                float u0 = sprite.getU((float) (i % perTile) / perTile);
                float u1 = sprite.getU((float) (i % perTile + 1) / perTile);
                vertex(buffer, cx, cy, cz, t0, a0 + t0 * 1.2f, u0, v0, b0);
                vertex(buffer, cx, cy, cz, t1, a0 + t1 * 1.2f, u0, v1, b1);
                vertex(buffer, cx, cy, cz, t1, a1 + t1 * 1.2f, u1, v1, b1);
                vertex(buffer, cx, cy, cz, t0, a1 + t0 * 1.2f, u1, v0, b0);
            }
        }
    }

    private void vertex(VertexConsumer buffer, float cx, float cy, float cz, float t, float angle, float u, float v, float strength) {
        float r = radius(t);
        buffer.addVertex(cx + Mth.cos(angle) * r, cy + t * HEIGHT, cz + Mth.sin(angle) * r)
                .setUv(u, v)
                .setColor((TINT >> 16 & 0xFF) / 255f * strength, (TINT >> 8 & 0xFF) / 255f * strength, (TINT & 0xFF) / 255f * strength, 1f)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x - 1.5, y, z - 1.5, x + 1.5, y + HEIGHT, z + 1.5);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticles.BLOCKS_RENDER_TYPE;
    }
}
