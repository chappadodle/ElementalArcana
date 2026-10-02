package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * A soft halo of light: a camera-facing quad of the glow sprite, added on top of what's behind it
 * (the "eyes" render type blends additively, like spider eyes), and drawn again into the bloom
 * buffer when Veil's bloom is on. Used by spell projectiles and wisps.
 */
public final class GlowHalo {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/misc/glow.png");

    private GlowHalo() {
    }

    /** Draws a halo {@code size} blocks across at the pose's origin, in {@code red, green, blue} (0-255). */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, Quaternionf cameraOrientation, float size,
                            int red, int green, int blue) {
        poseStack.pushPose();
        poseStack.mulPose(cameraOrientation);
        poseStack.scale(size, size, size);
        PoseStack.Pose pose = poseStack.last();
        quad(buffers.getBuffer(RenderType.eyes(TEXTURE)), pose, red, green, blue);
        RenderType bloom = Bloom.glowType(TEXTURE);
        if (bloom != null) {
            // Again into the bloom buffer, so the light spills around it.
            quad(buffers.getBuffer(bloom), pose, red, green, blue);
        }
        poseStack.popPose();
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, int red, int green, int blue) {
        vertex(consumer, pose, -0.5f, -0.5f, 0f, 1f, red, green, blue);
        vertex(consumer, pose, 0.5f, -0.5f, 1f, 1f, red, green, blue);
        vertex(consumer, pose, 0.5f, 0.5f, 1f, 0f, red, green, blue);
        vertex(consumer, pose, -0.5f, 0.5f, 0f, 0f, red, green, blue);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int red, int green, int blue) {
        consumer.addVertex(pose, x, y, 0f)
                .setColor(red, green, blue, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0f, 1f, 0f);
    }
}
