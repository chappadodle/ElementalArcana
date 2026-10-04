package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.drake.DrakeLike;
import com.chappadodle.elementalarcana.content.drake.TamedDrakeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

import java.util.Locale;

/**
 * An Elemental Drake, wild or raised: the wyvern model in its element's scales (textures from
 * tools/gen_drakes.py), its eyes glowing in the dark; a raised one drawn at its stage's size.
 */
public class DrakeRenderer<T extends Mob & DrakeLike> extends MobRenderer<T, DrakeModel<T>> {
    private static final float SHADOW = 1.6f;

    public DrakeRenderer(EntityRendererProvider.Context context) {
        super(context, new DrakeModel<>(context.bakeLayer(DrakeModel.LAYER)), SHADOW);
        addLayer(new EyesLayer<>(this));
    }

    private static String name(DrakeLike drake) {
        return drake.element().name().toLowerCase(Locale.ROOT);
    }

    @Override
    public ResourceLocation getTextureLocation(T drake) {
        return ElementalArcana.id("textures/entity/drake/" + name(drake) + ".png");
    }

    @Override
    protected void scale(T drake, PoseStack pose, float partialTick) {
        float scale = drake instanceof TamedDrakeEntity tamed ? tamed.renderScale() : 1f;
        pose.scale(scale, scale, scale);
        shadowRadius = SHADOW * scale;
    }

    /** Its eyes, bright whatever the light. */
    private static class EyesLayer<T extends Mob & DrakeLike> extends RenderLayer<T, DrakeModel<T>> {

        EyesLayer(RenderLayerParent<T, DrakeModel<T>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, T drake, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ResourceLocation eyes = ElementalArcana.id("textures/entity/drake/" + name(drake) + "_eyes.png");
            getParentModel().renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(eyes)), 0xF000F0, OverlayTexture.NO_OVERLAY, -1);
        }
    }
}
