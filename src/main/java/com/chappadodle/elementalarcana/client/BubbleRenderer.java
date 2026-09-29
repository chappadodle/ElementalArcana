package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.spell.BubblePrisonSpell;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws a Bubble Prison: a see-through water sphere around the trapped creature, sized to it,
 * slowly turning and wobbling like a real bubble.
 */
final class BubbleRenderer {
    // How much bigger than the creature the bubble is.
    private static final float MARGIN = 1.3f;

    private BubbleRenderer() {
    }

    static void render(LivingEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        if (!BubblePrisons.isTrapped(entity)) {
            return;
        }
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation.standalone(BubblePrisonSpell.BUBBLE_MODEL));
        float time = entity.tickCount + partialTick;
        float size = Math.max(entity.getBbWidth(), entity.getBbHeight()) * MARGIN;
        // Squash and stretch a little, out of step on each axis.
        float wobbleX = 1f + 0.05f * Mth.sin(time * 0.35f);
        float wobbleY = 1f + 0.05f * Mth.sin(time * 0.35f + 2.1f);
        poseStack.pushPose();
        poseStack.translate(0, entity.getBbHeight() / 2, 0);
        poseStack.mulPose(Axis.YP.rotation(time * 0.03f));
        poseStack.scale(size * wobbleX, size * wobbleY, size * wobbleX);
        poseStack.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poseStack.last(),
                buffers.getBuffer(Sheets.translucentCullBlockSheet()), null, model, 1f, 1f, 1f,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
