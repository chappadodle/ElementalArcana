package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.crypt.RevenantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

/**
 * A crypt's Revenant: bleached bones with a crown (textures from tools/gen_crypts.py), a robe and
 * hood in its element's colour, eyes burning in it, and its element's Master Staff in hand. While it
 * rises it climbs up out of its tomb.
 */
public class RevenantRenderer extends HumanoidMobRenderer<RevenantEntity, RevenantModel> {
    private static final ResourceLocation BODY = ElementalArcana.id("textures/entity/revenant/revenant.png");
    private static final ResourceLocation ROBE = ElementalArcana.id("textures/entity/revenant/robe.png");
    private static final ResourceLocation EYES = ElementalArcana.id("textures/entity/revenant/eyes.png");
    private static final float SUNK = 1.8f;

    public RevenantRenderer(EntityRendererProvider.Context context) {
        super(context, new RevenantModel(context.bakeLayer(RevenantModel.LAYER)), 0.6f);
        addLayer(new RobeLayer(this, new RevenantModel(context.bakeLayer(RevenantModel.ROBE))));
        addLayer(new EyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RevenantEntity revenant) {
        return BODY;
    }

    @Override
    public void render(RevenantEntity revenant, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float rise = revenant.riseProgress(partialTick);
        pose.pushPose();
        pose.translate(0, -(1f - rise) * SUNK * revenant.getScale(), 0);
        super.render(revenant, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }

    /** The robe and hood, in the element's colour. */
    private static class RobeLayer extends RenderLayer<RevenantEntity, RevenantModel> {
        private final RevenantModel robe;

        RobeLayer(RenderLayerParent<RevenantEntity, RevenantModel> parent, RevenantModel robe) {
            super(parent);
            this.robe = robe;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, RevenantEntity revenant, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            coloredCutoutModelCopyLayerRender(getParentModel(), robe, ROBE, pose, buffers, light, revenant, limbSwing, limbSwingAmount,
                    ageInTicks, netHeadYaw, headPitch, partialTick, FastColor.ARGB32.opaque(revenant.element().color()));
        }
    }

    /** Eyes burning in the element's colour, bright in the dark. */
    private static class EyesLayer extends RenderLayer<RevenantEntity, RevenantModel> {

        EyesLayer(RenderLayerParent<RevenantEntity, RevenantModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, RevenantEntity revenant, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(EYES)), 0xF000F0, OverlayTexture.NO_OVERLAY,
                    FastColor.ARGB32.opaque(revenant.element().color()));
        }
    }
}
