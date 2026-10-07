package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.FrostWraithEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws a Frost Wraith half seen (its texture is translucent, FrostWraithModel), with a faint glow
 * of its own and cold blue eyes burning in its hood. It casts no shadow.
 */
public class FrostWraithRenderer extends MobRenderer<FrostWraithEntity, FrostWraithModel<FrostWraithEntity>> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wild/frost_wraith.png");
    private static final RenderType EYES = RenderType.eyes(ElementalArcana.id("textures/entity/wild/frost_wraith_eyes.png"));

    public FrostWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new FrostWraithModel<>(context.bakeLayer(FrostWraithModel.LAYER)), 0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected int getBlockLightLevel(FrostWraithEntity wraith, BlockPos pos) {
        return Math.max(10, super.getBlockLightLevel(wraith, pos));
    }

    @Override
    public ResourceLocation getTextureLocation(FrostWraithEntity wraith) {
        return TEXTURE;
    }
}
