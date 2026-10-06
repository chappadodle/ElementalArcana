package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wonder.GlowmothEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * A glowmoth, in its element's colours, always lit as if by its own glow; and drawn again over
 * itself, added on full bright (as eyes are), so its bright wings shine whatever way they're turned
 * (its dark body adds next to nothing).
 */
public class GlowmothRenderer extends MobRenderer<GlowmothEntity, GlowmothModel> {

    public GlowmothRenderer(EntityRendererProvider.Context context) {
        super(context, new GlowmothModel(context.bakeLayer(GlowmothModel.LAYER)), 0.1f);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(GlowmothEntity moth) {
        return texture(moth);
    }

    private static ResourceLocation texture(GlowmothEntity moth) {
        return ElementalArcana.id("textures/entity/glowmoth/" + moth.element().name().toLowerCase(Locale.ROOT) + ".png");
    }

    @Override
    protected int getBlockLightLevel(GlowmothEntity moth, BlockPos pos) {
        return 15;
    }

    private static class Glow extends RenderLayer<GlowmothEntity, GlowmothModel> {
        Glow(RenderLayerParent<GlowmothEntity, GlowmothModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, GlowmothEntity moth, float limbSwing,
                           float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(texture(moth))), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY);
        }
    }
}
