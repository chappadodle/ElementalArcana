package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.end.StargazerEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws a Stargazer: its robe, and its face and the stars in its robe glowing on top (tools/gen_end.py). */
public class StargazerRenderer extends MobRenderer<StargazerEntity, StargazerModel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/end/stargazer.png");
    private static final RenderType GLOW = RenderType.eyes(ElementalArcana.id("textures/entity/end/stargazer_glow.png"));

    public StargazerRenderer(EntityRendererProvider.Context context) {
        super(context, new StargazerModel(context.bakeLayer(StargazerModel.LAYER)), 0.4f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(StargazerEntity gazer) {
        return TEXTURE;
    }
}
