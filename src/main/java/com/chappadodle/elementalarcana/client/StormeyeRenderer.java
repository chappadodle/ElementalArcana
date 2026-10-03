package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.spell.StormeyeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A Stormeye tornado: the Tempest Edge funnel (see WindFunnelParticle) grown to the tornado's
 * size and drawn additively from both sides: a twisting cone of wind, narrow at the ground and
 * wide at the top, turning fast and swaying, brightest low down, in the wind's pale colour or the
 * element it took. It rises over its first ticks and fades as it blows itself out. (The ground's
 * pieces and the wind curls round it are thrown by StormeyeEntity itself.)
 */
public class StormeyeRenderer extends EntityRenderer<StormeyeEntity> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/block/wind_funnel.png");
    private static final int RINGS = 14;
    private static final int SEGMENTS = 24;
    private static final int TILES_AROUND = 6;
    /** How far each ring is turned past the one below it, over the whole height. */
    private static final float TWIST = 2.2f;
    private static final float BASE_RADIUS = 0.4f;

    public StormeyeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(StormeyeEntity storm, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float time = storm.age() + partialTicks;
        float strength = Mth.clamp(time / 8f, 0f, 1f) * Mth.clamp((storm.lifetime() - time) / 10f, 0f, 1f);
        if (strength <= 0f) {
            return;
        }
        Funnel funnel = new Funnel(time, storm.height() * (0.4f + 0.6f * Mth.clamp(time / 8f, 0f, 1f)), storm.reach() * 0.55f, storm.tint());
        VertexConsumer buffer = buffers.getBuffer(RenderType.eyes(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        float spin = time * 0.45f;
        int perTile = SEGMENTS / TILES_AROUND;
        for (int k = 0; k < RINGS; k++) {
            float t0 = k / (float) RINGS;
            float t1 = (k + 1) / (float) RINGS;
            // Brighter low down, where the wind is densest.
            float b0 = strength * (0.85f - 0.55f * t0);
            float b1 = strength * (0.85f - 0.55f * t1);
            float v0 = (k % 2) / 2f;
            float v1 = (k % 2 + 1) / 2f;
            for (int i = 0; i < SEGMENTS; i++) {
                float a0 = spin + Mth.TWO_PI * i / SEGMENTS;
                float a1 = spin + Mth.TWO_PI * (i + 1) / SEGMENTS;
                float u0 = (float) (i % perTile) / perTile;
                float u1 = (float) (i % perTile + 1) / perTile;
                // Outside and inside, so it shows from within too.
                funnel.vertex(buffer, pose, t0, a0, u0, v0, b0);
                funnel.vertex(buffer, pose, t1, a0, u0, v1, b1);
                funnel.vertex(buffer, pose, t1, a1, u1, v1, b1);
                funnel.vertex(buffer, pose, t0, a1, u1, v0, b0);
                funnel.vertex(buffer, pose, t0, a1, u1, v0, b0);
                funnel.vertex(buffer, pose, t1, a1, u1, v1, b1);
                funnel.vertex(buffer, pose, t1, a0, u0, v1, b1);
                funnel.vertex(buffer, pose, t0, a0, u0, v0, b0);
            }
        }
    }

    /** The funnel at one moment: how tall, how wide at the top, its colour. */
    private record Funnel(float time, float height, float top, int tint) {
        float radius(float t) {
            return BASE_RADIUS + (top - BASE_RADIUS) * (float) Math.pow(t, 1.4);
        }

        void vertex(VertexConsumer buffer, PoseStack.Pose pose, float t, float angle, float u, float v, float brightness) {
            float radius = radius(t);
            float turn = angle + t * TWIST;
            // It sways, more toward the top.
            float x = Mth.sin(time * 0.11f + t * 2.5f) * 0.35f * t + Mth.cos(turn) * radius;
            float z = Mth.cos(time * 0.09f + t * 2.1f) * 0.35f * t + Mth.sin(turn) * radius;
            buffer.addVertex(pose, x, t * height, z)
                    .setColor((tint >> 16 & 0xFF) / 255f * brightness, (tint >> 8 & 0xFF) / 255f * brightness, (tint & 0xFF) / 255f * brightness, 1f)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose, 0, 1, 0);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(StormeyeEntity storm) {
        return TEXTURE;
    }
}
