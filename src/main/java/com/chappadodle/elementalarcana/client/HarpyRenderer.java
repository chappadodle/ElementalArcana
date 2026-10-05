package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.HarpyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Draws a Gale Harpy in its storm-grey feathers. */
public class HarpyRenderer extends MobRenderer<HarpyEntity, HarpyModel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wild/gale_harpy.png");

    public HarpyRenderer(EntityRendererProvider.Context context) {
        super(context, new HarpyModel(context.bakeLayer(HarpyModel.LAYER)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(HarpyEntity harpy) {
        return TEXTURE;
    }
}
