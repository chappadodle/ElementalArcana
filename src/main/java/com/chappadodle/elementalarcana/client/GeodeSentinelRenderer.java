package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.spell.GeodeSentinel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Crystal's Geode Sentinel: a big cluster of violet crystal (Crystal Spire's six-sided shafts, a
 * tall one in the middle, a ring leaning out round it and stubs at its foot) with three small
 * shards circling its middle. It grows out of the ground over its first half second, glows a little
 * brighter as it pulses, and flashes white when it's hit.
 */
public class GeodeSentinelRenderer extends EntityRenderer<GeodeSentinel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/block/shrine_crystal.png");
    /** Each shaft: where it stands (x, z), how tall and wide it is, and how far it leans out (degrees). */
    private static final float[][] SHAFTS = {
            {0f, 0f, 2.6f, 0.34f, 0f},
            {0.42f, 0.12f, 1.6f, 0.22f, 18f},
            {0.1f, 0.44f, 1.35f, 0.2f, 22f},
            {-0.36f, 0.26f, 1.7f, 0.23f, 16f},
            {-0.4f, -0.2f, 1.25f, 0.19f, 24f},
            {-0.08f, -0.45f, 1.5f, 0.21f, 20f},
            {0.33f, -0.32f, 1.15f, 0.18f, 26f},
            {0.7f, 0.35f, 0.55f, 0.12f, 48f},
            {-0.65f, 0.45f, 0.5f, 0.11f, 52f},
            {-0.7f, -0.4f, 0.6f, 0.12f, 45f},
            {0.55f, -0.6f, 0.45f, 0.1f, 50f}};
    private static final int ORBITERS = 3;

    public GeodeSentinelRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(GeodeSentinel sentinel, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float t = sentinel.tickCount + partialTicks;
        float grown = Mth.clamp(t / 10f, 0f, 1f);
        grown = 1f - (1f - grown) * (1f - grown);
        if (grown <= 0f) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        // Brighter for a moment each second (its pulse), white when it's hit.
        float pulse = 0.88f + 0.12f * Mth.clamp(1f - ((t + 19) % 20) / 6f, 0f, 1f);
        float flash = sentinel.hurtTime > 0 ? (sentinel.hurtTime - partialTicks) / 10f : 0f;
        float red = Mth.lerp(flash, 0.85f * pulse, 1f);
        float green = Mth.lerp(flash, 0.6f * pulse, 1f);
        float blue = Mth.lerp(flash, 1f * pulse, 1f);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-sentinel.getYRot()));
        for (float[] shaft : SHAFTS) {
            poseStack.pushPose();
            poseStack.translate(shaft[0], 0, shaft[1]);
            if (shaft[4] != 0f) {
                float away = (float) Math.atan2(shaft[0], shaft[1]);
                poseStack.mulPose(Axis.YP.rotation(away));
                poseStack.mulPose(Axis.XP.rotationDegrees(shaft[4]));
            }
            CrystalSpireRenderer.shaft(buffer, poseStack.last(), shaft[2] * grown, shaft[3], red, green, blue);
            poseStack.popPose();
        }
        for (int i = 0; i < ORBITERS; i++) {
            float angle = t * 0.08f + Mth.TWO_PI * i / ORBITERS;
            poseStack.pushPose();
            poseStack.translate(Mth.cos(angle) * 1.1f, 1.2f + Mth.sin(t * 0.1f + i) * 0.15f, Mth.sin(angle) * 1.1f);
            poseStack.mulPose(Axis.YP.rotation(-angle));
            poseStack.mulPose(Axis.ZP.rotationDegrees(25f));
            CrystalSpireRenderer.shaft(buffer, poseStack.last(), 0.4f * grown, 0.08f, red, green, blue);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(GeodeSentinel sentinel) {
        return TEXTURE;
    }
}
