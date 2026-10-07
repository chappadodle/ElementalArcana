package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.AshWraithEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws an Ash Wraith with the Frost Wraith's body (FrostWraithModel), in ash-grey tatters half
 * seen, its eyes burning soul-blue in its hood. It casts no shadow.
 */
public class AshWraithRenderer extends MobRenderer<AshWraithEntity, FrostWraithModel<AshWraithEntity>> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/wild/ash_wraith.png");
    private static final RenderType EYES = RenderType.eyes(ElementalArcana.id("textures/entity/wild/ash_wraith_eyes.png"));

    public AshWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new FrostWraithModel<>(context.bakeLayer(FrostWraithModel.LAYER)), 0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected int getBlockLightLevel(AshWraithEntity wraith, BlockPos pos) {
        return Math.max(9, super.getBlockLightLevel(wraith, pos));
    }

    @Override
    public ResourceLocation getTextureLocation(AshWraithEntity wraith) {
        return TEXTURE;
    }
}
