package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.drake.DrakeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * An Elemental Drake: the wyvern model in its element's scales (textures from tools/gen_drakes.py),
 * its eyes glowing in the dark.
 */
public class DrakeRenderer extends MobRenderer<DrakeEntity, DrakeModel> {

    public DrakeRenderer(EntityRendererProvider.Context context) {
        super(context, new DrakeModel(context.bakeLayer(DrakeModel.LAYER)), 1.6f);
        addLayer(new EyesLayer(this));
    }

    private static String name(DrakeEntity drake) {
        return drake.element().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public ResourceLocation getTextureLocation(DrakeEntity drake) {
        return ElementalArcana.id("textures/entity/drake/" + name(drake) + ".png");
    }

    /** Its eyes, bright whatever the light. */
    private static class EyesLayer extends RenderLayer<DrakeEntity, DrakeModel> {

        EyesLayer(RenderLayerParent<DrakeEntity, DrakeModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, DrakeEntity drake, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ResourceLocation eyes = ElementalArcana.id("textures/entity/drake/" + name(drake) + "_eyes.png");
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(eyes)), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
