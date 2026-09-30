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
 * An explosion's ball of fire as a real 3D sphere: wrapped in a fireball's animated fire texture,
 * it swells quickly, slowly turns, shifts from its colour to its fade colour and fades out. It's
 * added on top of the scene and drawn from both sides, so it's densest through its middle and
 * reads as a solid ball from any angle.
 */
public class FireSphereParticle extends Particle {
    private static final int SLICES = 16;
    private static final int STACKS = 8;
    /** How many times the texture wraps around, and from pole to pole. */
    private static final int TILES_AROUND = 4;
    private static final int TILES_DOWN = 2;

    private final TextureAtlasSprite sprite;
    private final float maxRadius;
    private final float startFraction;
    private final float startRed, startGreen, startBlue;
    private final float endRed, endGreen, endBlue;
    private final float intensity;
    private final float spin;

    /**
     * {@code texture} is a block-atlas texture; the sphere starts at {@code startFraction} of
     * {@code radius} and reaches it with an ease-out.
     */
    public FireSphereParticle(ClientLevel level, Vec3 at, ResourceLocation texture, float radius, float startFraction,
                              int lifetime, int color, int fadeColor, float intensity) {
        super(level, at.x, at.y, at.z);
        this.sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        this.maxRadius = radius;
        this.startFraction = startFraction;
        this.lifetime = lifetime;
        this.startRed = (color >> 16 & 0xFF) / 255f;
        this.startGreen = (color >> 8 & 0xFF) / 255f;
        this.startBlue = (color & 0xFF) / 255f;
        this.endRed = (fadeColor >> 16 & 0xFF) / 255f;
        this.endGreen = (fadeColor >> 8 & 0xFF) / 255f;
        this.endBlue = (fadeColor & 0xFF) / 255f;
        this.intensity = intensity;
        this.spin = (random.nextFloat() - 0.5f) * 0.12f;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.xd = this.yd = this.zd = 0;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float life = Mth.clamp((age + partialTicks) / lifetime, 0f, 1f);
        float grown = 1f - (1f - life) * (1f - life) * (1f - life);
        float radius = maxRadius * (startFraction + (1f - startFraction) * grown);
        float shift = Mth.sqrt(life);
        float alpha = intensity * (1f - life) * Mth.sqrt(1f - life);
        float red = Mth.lerp(shift, startRed, endRed);
        float green = Mth.lerp(shift, startGreen, endGreen);
        float blue = Mth.lerp(shift, startBlue, endBlue);
        Vec3 cameraPos = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTicks, xo, x) - cameraPos.x);
        float cy = (float) (Mth.lerp(partialTicks, yo, y) - cameraPos.y);
        float cz = (float) (Mth.lerp(partialTicks, zo, z) - cameraPos.z);
        float turn = spin * (age + partialTicks);
        int perTileAround = SLICES / TILES_AROUND;
        int perTileDown = STACKS / TILES_DOWN;

        for (int i = 0; i < SLICES; i++) {
            float theta0 = turn + Mth.TWO_PI * i / SLICES;
            float theta1 = turn + Mth.TWO_PI * (i + 1) / SLICES;
            float u0 = sprite.getU((float) (i % perTileAround) / perTileAround);
            float u1 = sprite.getU((float) (i % perTileAround + 1) / perTileAround);
            for (int j = 0; j < STACKS; j++) {
                float phi0 = Mth.PI * j / STACKS;
                float phi1 = Mth.PI * (j + 1) / STACKS;
                float v0 = sprite.getV((float) (j % perTileDown) / perTileDown);
                float v1 = sprite.getV((float) (j % perTileDown + 1) / perTileDown);
                vertex(buffer, cx, cy, cz, radius, theta0, phi0, u0, v0, red, green, blue, alpha);
                vertex(buffer, cx, cy, cz, radius, theta0, phi1, u0, v1, red, green, blue, alpha);
                vertex(buffer, cx, cy, cz, radius, theta1, phi1, u1, v1, red, green, blue, alpha);
                vertex(buffer, cx, cy, cz, radius, theta1, phi0, u1, v0, red, green, blue, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer buffer, float cx, float cy, float cz, float radius, float theta, float phi,
                               float u, float v, float red, float green, float blue, float alpha) {
        float ring = Mth.sin(phi) * radius;
        buffer.addVertex(cx + ring * Mth.cos(theta), cy + Mth.cos(phi) * radius, cz + ring * Mth.sin(theta))
                .setUv(u, v)
                .setColor(red, green, blue, alpha)
                .setLight(LightTexture.FULL_BRIGHT);
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x, y, z, x, y, z).inflate(maxRadius);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticles.BLOCKS_RENDER_TYPE;
    }
}
