package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.SalamanderEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws an Ember Salamander, its eyes, the glowing seams between its scales and the inside of its
 * mouth (seen when it gapes) lit from within.
 */
public class SalamanderRenderer extends MobRenderer<SalamanderEntity, SalamanderModel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wild/ember_salamander.png");
    private static final RenderType GLOW = RenderType.eyes(ElementalArcana.id("textures/entity/wild/ember_salamander_glow.png"));

    public SalamanderRenderer(EntityRendererProvider.Context context) {
        super(context, new SalamanderModel(context.bakeLayer(SalamanderModel.LAYER)), 0.5f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SalamanderEntity salamander) {
        return TEXTURE;
    }
}
