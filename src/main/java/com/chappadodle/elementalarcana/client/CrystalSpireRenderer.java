package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.spell.CrystalSpire;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Prism Bolt's Crystal Spire: a cluster of five six-sided shafts of violet crystal (the shrines'
 * crystal texture, tinted), a tall one in the middle and four smaller ones leaning out from it,
 * glowing faintly. It shoots up out of the ground over its first half second.
 */
public class CrystalSpireRenderer extends EntityRenderer<CrystalSpire> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/block/shrine_crystal.png");
    private static final int SIDES = 6;
    /** Each shaft: where it stands (x, z), how tall and wide it is, and how far it leans out (degrees). */
    private static final float[][] SHAFTS = {
            {0f, 0f, 1.8f, 0.22f, 0f},
            {0.32f, 0.1f, 1.1f, 0.14f, 22f},
            {-0.25f, 0.28f, 0.9f, 0.13f, 26f},
            {-0.2f, -0.3f, 1.2f, 0.15f, 18f},
            {0.18f, -0.33f, 0.75f, 0.11f, 30f}};

    public CrystalSpireRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(CrystalSpire spire, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float grown = Mth.clamp((spire.tickCount + partialTicks) / 10f, 0f, 1f);
        grown = 1f - (1f - grown) * (1f - grown);
        if (grown <= 0f) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        float glint = 0.9f + 0.1f * Mth.sin((spire.tickCount + partialTicks) * 0.3f);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-spire.getYRot()));
        for (float[] shaft : SHAFTS) {
            poseStack.pushPose();
            poseStack.translate(shaft[0], 0, shaft[1]);
            if (shaft[4] != 0f) {
                // Lean out, away from the middle.
                float away = (float) Math.atan2(shaft[0], shaft[1]);
                poseStack.mulPose(Axis.YP.rotation(away));
                poseStack.mulPose(Axis.XP.rotationDegrees(shaft[4]));
            }
            shaft(buffer, poseStack.last(), shaft[2] * grown, shaft[3], 0.85f * glint, 0.6f * glint, 1f * glint);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** A six-sided shaft {@code height} tall and {@code radius} wide, its top fifth a point (the Geode Sentinel's too). */
    static void shaft(VertexConsumer buffer, PoseStack.Pose pose, float height, float radius, float red, float green, float blue) {
        float body = height * 0.8f;
        for (int i = 0; i < SIDES; i++) {
            float a0 = Mth.TWO_PI * i / SIDES;
            float a1 = Mth.TWO_PI * (i + 1) / SIDES;
            float x0 = Mth.cos(a0) * radius;
            float z0 = Mth.sin(a0) * radius;
            float x1 = Mth.cos(a1) * radius;
            float z1 = Mth.sin(a1) * radius;
            float u0 = (i % 3) / 3f;
            float u1 = (i % 3 + 1) / 3f;
            // Faces turned away from the light are a little darker, like blocks.
            float shade = 0.8f + 0.2f * Mth.cos((a0 + a1) / 2f);
            vertex(buffer, pose, x0, 0, z0, u0, 1f, red * shade, green * shade, blue * shade);
            vertex(buffer, pose, x0, body, z0, u0, 0.2f, red * shade, green * shade, blue * shade);
            vertex(buffer, pose, x1, body, z1, u1, 0.2f, red * shade, green * shade, blue * shade);
            vertex(buffer, pose, x1, 0, z1, u1, 1f, red * shade, green * shade, blue * shade);
            // The point, as a quad with its tip doubled.
            vertex(buffer, pose, x0, body, z0, u0, 0.2f, red, green, blue);
            vertex(buffer, pose, 0, height, 0, (u0 + u1) / 2f, 0f, red, green, blue);
            vertex(buffer, pose, 0, height, 0, (u0 + u1) / 2f, 0f, red, green, blue);
            vertex(buffer, pose, x1, body, z1, u1, 0.2f, red, green, blue);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               float red, float green, float blue) {
        buffer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, 0.9f)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(CrystalSpire spire) {
        return TEXTURE;
    }
}
