package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.rift.RiftEntity;
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
import net.minecraft.world.phys.Vec3;

/**
 * An Elemental Rift: a jagged tear standing in the air, always turned to face whoever looks at it.
 * Its inside is a dark void with the element churning in it, its edge burns in the element's colour
 * with a white-hot rim, and both flicker; a faint pillar of the element's light rises over it, so
 * it can be found from afar. It splits open over its first two seconds and pinches shut when it
 * closes or fades. (The motes drifting into it are RiftEntity's own.)
 */
public class RiftRenderer extends EntityRenderer<RiftEntity> {
    private static final ResourceLocation VOID = ElementalArcana.id("textures/entity/rift_void.png");
    private static final ResourceLocation EDGE = ElementalArcana.id("textures/entity/rift_edge.png");
    private static final ResourceLocation SWIRL = ElementalArcana.id("textures/entity/rift_swirl.png");
    private static final ResourceLocation BEAM = ElementalArcana.id("textures/entity/rift_beam.png");
    private static final float WIDTH = 2.6f;
    private static final float HEIGHT = 4.0f;
    private static final float BEAM_HEIGHT = 28f;
    private static final float BEAM_WIDTH = 1.4f;

    public RiftRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RiftEntity rift, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float open = rift.openness(partialTicks);
        if (open <= 0f) {
            return;
        }
        float time = rift.tickCount + partialTicks;
        int color = rift.element().color();
        float red = (color >> 16 & 0xFF) / 255f;
        float green = (color >> 8 & 0xFF) / 255f;
        float blue = (color & 0xFF) / 255f;
        // It splits open: tall first, then wide.
        float height = HEIGHT * Mth.clamp(open * 1.6f, 0f, 1f);
        float width = WIDTH * open * open;
        poseStack.pushPose();
        poseStack.translate(0, RiftEntity.MIDDLE, 0);
        Vec3 eye = entityRenderDispatcher.camera.getPosition();
        poseStack.mulPose(Axis.YP.rotation((float) Mth.atan2(eye.x - rift.getX(), eye.z - rift.getZ())));
        PoseStack.Pose pose = poseStack.last();
        float flicker = 0.85f + 0.15f * Mth.sin(time * 0.9f) * Mth.sin(time * 0.37f + 1f);
        quad(buffers.getBuffer(RenderType.entityTranslucent(VOID)), pose, width, height, 0f, 1f, 1f, 1f, 0.95f);
        // The churn inside: a swirl of the element, turning, smaller than the tear so it stays in it.
        poseStack.pushPose();
        poseStack.translate(0, 0, 0.01f);
        poseStack.mulPose(Axis.ZP.rotation(time * 0.06f));
        float swirl = Math.min(width, height) * 0.55f;
        quad(buffers.getBuffer(RenderType.eyes(SWIRL)), poseStack.last(), swirl, swirl, 0f, red * 0.7f * flicker, green * 0.7f * flicker,
                blue * 0.7f * flicker, 1f);
        poseStack.popPose();
        // The burning edge, and a white-hot rim just inside it.
        VertexConsumer edge = buffers.getBuffer(RenderType.eyes(EDGE));
        quad(edge, pose, width * 1.06f, height * 1.03f, 0.02f, red * flicker, green * flicker, blue * flicker, 1f);
        float rim = 0.45f * flicker;
        quad(edge, pose, width, height, 0.03f, rim, rim, rim, 1f);
        // The pillar of light, from the top of the tear up.
        poseStack.pushPose();
        poseStack.translate(0, height / 2f + BEAM_HEIGHT / 2f - 0.5f, -0.02f);
        float beam = 0.45f * open * flicker;
        quad(buffers.getBuffer(RenderType.eyes(BEAM)), poseStack.last(), BEAM_WIDTH, BEAM_HEIGHT, 0f, red * beam, green * beam, blue * beam, 1f);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** A quad facing the camera (local +z), {@code width} by {@code height} around the middle, {@code z} in front of it. */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float width, float height, float z,
                             float red, float green, float blue, float alpha) {
        float w = width / 2f;
        float h = height / 2f;
        vertex(buffer, pose, -w, -h, z, 0f, 1f, red, green, blue, alpha);
        vertex(buffer, pose, w, -h, z, 1f, 1f, red, green, blue, alpha);
        vertex(buffer, pose, w, h, z, 1f, 0f, red, green, blue, alpha);
        vertex(buffer, pose, -w, h, z, 0f, 0f, red, green, blue, alpha);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               float red, float green, float blue, float alpha) {
        buffer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 0, 1);
    }

    @Override
    public boolean shouldRender(RiftEntity rift, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        return frustum.isVisible(rift.getBoundingBox().expandTowards(0, BEAM_HEIGHT, 0).inflate(2));
    }

    @Override
    public ResourceLocation getTextureLocation(RiftEntity rift) {
        return VOID;
    }
}
