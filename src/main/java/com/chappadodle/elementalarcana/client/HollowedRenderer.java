package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.hollowed.HollowedEntity;
import net.minecraft.client.model.IllagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * One of the Hollowed (see their spec): the vanilla illager model (as the evoker's) in their robes
 * (textures from tools/gen_hollowed.py), hooded or not, with eyes that glow violet in the dark.
 * Devourers and Heralds are drawn bigger by their scale.
 */
public class HollowedRenderer<T extends HollowedEntity> extends IllagerRenderer<T> {
    private static final RenderType EYES = RenderType.eyes(ElementalArcana.id("textures/entity/hollowed/eyes.png"));

    private final ResourceLocation texture;

    public HollowedRenderer(EntityRendererProvider.Context context, String name, boolean hooded) {
        super(context, new IllagerModel<>(context.bakeLayer(ModelLayers.EVOKER)), 0.5f);
        this.texture = ElementalArcana.id("textures/entity/hollowed/" + name + ".png");
        model.getHat().visible = hooded;
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
