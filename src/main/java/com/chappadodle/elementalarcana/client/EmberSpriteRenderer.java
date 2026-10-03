package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.spell.EmberSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * An Ember Sprite: a fire wisp in miniature (the wisp model at half its size, the fire wisp's
 * texture), fully lit, bobbing and spinning in a glow of firelight.
 */
public class EmberSpriteRenderer extends EntityRenderer<EmberSprite> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wisp/fire.png");
    private static final float SCALE = 0.5f;

    private final ModelPart core;
    private final ModelPart shell;

    public EmberSpriteRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(WispModel.LAYER).getChild("root");
        core = root.getChild("core");
        shell = root.getChild("shell");
    }

    @Override
    public void render(EmberSprite sprite, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float time = sprite.age() + partialTick;
        poseStack.pushPose();
        poseStack.translate(0, 0.25 + Mth.sin(time * 0.15f) * 0.06, 0);
        GlowHalo.draw(poseStack, buffers, entityRenderDispatcher.cameraOrientation(), 0.9f * (1f + 0.1f * Mth.sin(time * 0.3f)), 150, 70, 20);
        poseStack.scale(-SCALE, -SCALE, SCALE);
        core.yRot = -time * 0.2f;
        core.xRot = time * 0.1f;
        shell.yRot = time * 0.08f;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        int light = 0xF000F0;
        core.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        shell.render(poseStack, buffer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(sprite, yaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EmberSprite sprite) {
        return TEXTURE;
    }
}
