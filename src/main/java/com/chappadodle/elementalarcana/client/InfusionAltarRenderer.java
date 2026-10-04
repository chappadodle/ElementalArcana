package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.content.infusion.InfusionAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the item set on an Infusion Altar, floating over its basin and slowly turning, lit a little
 * more by each Essence poured in.
 */
public class InfusionAltarRenderer implements BlockEntityRenderer<InfusionAltarBlockEntity> {
    private final ItemRenderer items;

    public InfusionAltarRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public void render(InfusionAltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        ItemStack stack = altar.item();
        if (stack.isEmpty() || altar.getLevel() == null) {
            return;
        }
        float time = altar.getLevel().getGameTime() + partialTick;
        poseStack.pushPose();
        poseStack.translate(0.5, 1.25 + Mth.sin(time * 0.08f) * 0.06, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.5f));
        poseStack.scale(0.7f, 0.7f, 0.7f);
        int block = Math.min(15, Math.max(LightTexture.block(packedLight), 4 + altar.charge()));
        int light = LightTexture.pack(block, LightTexture.sky(packedLight));
        items.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, buffers, altar.getLevel(), 0);
        poseStack.popPose();
    }
}
