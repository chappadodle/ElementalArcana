package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An explosion's shockwave in 3D, wrapped in fire. As a wall, it's a ring of fire that rolls out
 * along the ground, bright at its foot and fading toward its top, sinking as it spreads. As a
 * dome, it's a swelling half-sphere that glows at its edges (where you see through more of it) and
 * is nearly clear where you look straight through it (Sunfire).
 */
public class FireWaveParticle extends Particle {
    private static final int SEGMENTS = 32;
    private static final int DOME_STACKS = 6;
    private static final int TILES_AROUND = 8;

    private final TextureAtlasSprite sprite;
    private final float maxRadius;
    private final float height;
    private final boolean dome;
    private final float red, green, blue;
    private final float intensity;

    public FireWaveParticle(ClientLevel level, Vec3 base, ResourceLocation texture, float radius, float height,
                            boolean dome, int lifetime, int color, float intensity) {
        super(level, base.x, base.y, base.z);
        this.sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        this.maxRadius = radius;
        this.height = height;
        this.dome = dome;
        this.lifetime = lifetime;
        this.red = (color >> 16 & 0xFF) / 255f;
        this.green = (color >> 8 & 0xFF) / 255f;
        this.blue = (color & 0xFF) / 255f;
        this.intensity = intensity;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.xd = this.yd = this.zd = 0;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float life = Mth.clamp((age + partialTicks) / lifetime, 0f, 1f);
        float grown = 1f - (1f - life) * (1f - life);
        float radius = maxRadius * (0.1f + 0.9f * grown);
        float alpha = intensity * (1f - life);
        Vec3 cameraPos = camera.getPosition();
        float cx = (float) (x - cameraPos.x);
        float cy = (float) (y - cameraPos.y);
        float cz = (float) (z - cameraPos.z);
        int perTile = SEGMENTS / TILES_AROUND;

        for (int i = 0; i < SEGMENTS; i++) {
            float a0 = Mth.TWO_PI * i / SEGMENTS;
            float a1 = Mth.TWO_PI * (i + 1) / SEGMENTS;
            float u0 = sprite.getU((float) (i % perTile) / perTile);
            float u1 = sprite.getU((float) (i % perTile + 1) / perTile);
            if (dome) {
                for (int j = 0; j < DOME_STACKS; j++) {
                    float p0 = Mth.HALF_PI * j / DOME_STACKS;
                    float p1 = Mth.HALF_PI * (j + 1) / DOME_STACKS;
                    float v0 = sprite.getV((float) j / DOME_STACKS);
                    float v1 = sprite.getV((float) (j + 1) / DOME_STACKS);
                    domeVertex(buffer, cx, cy, cz, radius, a0, p0, u0, v0, alpha);
                    domeVertex(buffer, cx, cy, cz, radius, a0, p1, u0, v1, alpha);
                    domeVertex(buffer, cx, cy, cz, radius, a1, p1, u1, v1, alpha);
                    domeVertex(buffer, cx, cy, cz, radius, a1, p0, u1, v0, alpha);
                }
            } else {
                // Sinks as it spreads.
                float top = height * (1f - life);
                wallVertex(buffer, cx, cy, cz, radius, a0, 0.05f, u0, sprite.getV1(), alpha);
                wallVertex(buffer, cx, cy, cz, radius, a0, top, u0, sprite.getV0(), 0f);
                wallVertex(buffer, cx, cy, cz, radius, a1, top, u1, sprite.getV0(), 0f);
                wallVertex(buffer, cx, cy, cz, radius, a1, 0.05f, u1, sprite.getV1(), alpha);
            }
        }
    }

    private void wallVertex(VertexConsumer buffer, float cx, float cy, float cz, float radius, float angle, float up,
                            float u, float v, float alpha) {
        buffer.addVertex(cx + radius * Mth.cos(angle), cy + up, cz + radius * Mth.sin(angle))
                .setUv(u, v)
                .setColor(red, green, blue, alpha)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    /** A dome point; the more edge-on the dome is seen there, the brighter (a glowing rim). */
    private void domeVertex(VertexConsumer buffer, float cx, float cy, float cz, float radius, float angle, float lift,
                            float u, float v, float alpha) {
        float nx = Mth.cos(lift) * Mth.cos(angle);
        float ny = Mth.sin(lift);
        float nz = Mth.cos(lift) * Mth.sin(angle);
        float px = cx + radius * nx;
        float py = cy + radius * ny;
        float pz = cz + radius * nz;
        float distance = Mth.sqrt(px * px + py * py + pz * pz);
        float facing = distance < 1.0e-3f ? 0f : Math.abs((px * nx + py * ny + pz * nz) / distance);
        float rim = (1f - facing) * (1f - facing);
        buffer.addVertex(px, py, pz)
                .setUv(u, v)
                .setColor(red, green, blue, alpha * (0.15f + 0.85f * rim))
                .setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x, y, z, x, y, z).inflate(maxRadius, Math.max(height, maxRadius), maxRadius);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticles.BLOCKS_RENDER_TYPE;
    }
}
