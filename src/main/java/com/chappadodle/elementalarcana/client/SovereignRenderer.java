package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.sanctum.SovereignEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;

/**
 * Draws a Sovereign: its model at twice its size, fully lit like a wisp, in a great halo of its
 * element's light, and in its second phase with its eyes and core burning (a second texture).
 */
public class SovereignRenderer extends MobRenderer<SovereignEntity, SovereignModel> {
    private static final float SCALE = 2f;
    private static final float HALO_SIZE = 3.2f;
    private static final float HALO_STRENGTH = 0.6f;

    public SovereignRenderer(EntityRendererProvider.Context context) {
        super(context, new SovereignModel(context.bakeLayer(SovereignModel.LAYER)), 0.9f);
    }

    @Override
    protected void scale(SovereignEntity sovereign, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public void render(SovereignEntity sovereign, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(sovereign, entityYaw, partialTick, poseStack, buffers, packedLight);
        float time = sovereign.tickCount + partialTick;
        // The core's height (SovereignModel: 15 pixels up at half size, 1.2 pixels of bob), doubled.
        float middle = (15f - Mth.sin(time * 0.08f) * 1.2f) / 16f * SCALE;
        float size = HALO_SIZE * (1f + 0.06f * Mth.sin(time * 0.15f)) * (sovereign.isEnraged() ? 1.25f : 1f);
        int color = sovereign.element().color();
        poseStack.pushPose();
        poseStack.translate(0, middle, 0);
        GlowHalo.draw(poseStack, buffers, entityRenderDispatcher.cameraOrientation(), size,
                Math.round((color >> 16 & 0xFF) * HALO_STRENGTH), Math.round((color >> 8 & 0xFF) * HALO_STRENGTH),
                Math.round((color & 0xFF) * HALO_STRENGTH));
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(SovereignEntity sovereign) {
        return texture(sovereign.element(), sovereign.isEnraged());
    }

    private static ResourceLocation texture(Element element, boolean enraged) {
        return ElementalArcana.id("textures/entity/sovereign/" + element.name().toLowerCase(Locale.ROOT) + (enraged ? "_enraged" : "") + ".png");
    }

    @Override
    protected int getBlockLightLevel(SovereignEntity sovereign, BlockPos pos) {
        return 15;
    }

    @Override
    protected float getFlipDegrees(SovereignEntity sovereign) {
        return 0;
    }
}
