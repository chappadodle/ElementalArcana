package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.sanctum.SovereignEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/** Draws the Hollow: a Sovereign's model gone black at three times its size, burning in its form's colour. */
public class HollowRenderer extends SovereignRenderer {

    public HollowRenderer(EntityRendererProvider.Context context) {
        super(context, 3f, 1.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(SovereignEntity hollow) {
        return ElementalArcana.id("textures/entity/sovereign/hollow_" + hollow.element().name().toLowerCase(Locale.ROOT) + ".png");
    }
}
