package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.CinderHoundEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws a Cinder Hound: its hide, and its seams and eyes glowing on top (tools/gen_nether.py). */
public class CinderHoundRenderer extends MobRenderer<CinderHoundEntity, CinderHoundModel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wild/cinder_hound.png");
    private static final RenderType GLOW = RenderType.eyes(ElementalArcana.id("textures/entity/wild/cinder_hound_glow.png"));

    public CinderHoundRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderHoundModel(context.bakeLayer(CinderHoundModel.LAYER)), 0.5f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(CinderHoundEntity hound) {
        return TEXTURE;
    }
}
