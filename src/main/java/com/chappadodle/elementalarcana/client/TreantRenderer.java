package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.TreantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.EnumMap;
import java.util.Map;

/**
 * Draws a Thornwood Treant in its forest's bark and leaves. Asleep it passes for a tree: no shadow,
 * no eyes; awake, the knotholes in its face glow green.
 */
public class TreantRenderer extends MobRenderer<TreantEntity, TreantModel> {
    private static final Map<TreantEntity.Wood, ResourceLocation> TEXTURES = new EnumMap<>(TreantEntity.Wood.class);
    private static final RenderType EYES = RenderType.eyes(ElementalArcana.id("textures/entity/wild/treant_eyes.png"));
    private static final float SHADOW = 0.9f;

    static {
        for (TreantEntity.Wood wood : TreantEntity.Wood.values()) {
            TEXTURES.put(wood, ElementalArcana.id("textures/entity/wild/treant_" + wood.id() + ".png"));
        }
    }

    public TreantRenderer(EntityRendererProvider.Context context) {
        super(context, new TreantModel(context.bakeLayer(TreantModel.LAYER)), SHADOW);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, TreantEntity treant, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (treant.isAwake()) {
                    getParentModel().renderToBuffer(poseStack, buffers.getBuffer(EYES), 0xF00000, OverlayTexture.NO_OVERLAY);
                }
            }
        });
    }

    @Override
    public void render(TreantEntity treant, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        shadowRadius = treant.isAwake() ? SHADOW : 0f;
        super.render(treant, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(TreantEntity treant) {
        return TEXTURES.get(treant.wood());
    }
}
