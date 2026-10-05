package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.BogLurkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Draws a Bog Lurker in its mottled swamp hide. */
public class BogLurkerRenderer extends MobRenderer<BogLurkerEntity, BogLurkerModel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wild/bog_lurker.png");

    public BogLurkerRenderer(EntityRendererProvider.Context context) {
        super(context, new BogLurkerModel(context.bakeLayer(BogLurkerModel.LAYER)), 0.9f);
    }

    @Override
    public ResourceLocation getTextureLocation(BogLurkerEntity lurker) {
        return TEXTURE;
    }
}
