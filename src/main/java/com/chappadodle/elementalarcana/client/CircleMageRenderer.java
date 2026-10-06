package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.circle.CircleMageEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

/**
 * A Circle Mage or the Archmagister (see the Circle spec, part 1): the villager as Minecraft draws
 * it, with the Circle's white robes over it, trimmed with the mage's element's colour (the
 * Archmagister's with gold; drawn by tools/gen_circle.py), as a profession's clothes go over a
 * villager.
 */
public class CircleMageRenderer extends MobRenderer<CircleMageEntity, VillagerModel<CircleMageEntity>> {
    private static final ResourceLocation SKIN = ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public CircleMageRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5f);
        model.hatVisible(true);
        addLayer(new RenderLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffers, int light, CircleMageEntity mage, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (!mage.isInvisible()) {
                    renderColoredCutoutModel(getParentModel(), clothes(mage), poseStack, buffers, light, mage, -1);
                }
            }
        });
    }

    private static ResourceLocation clothes(CircleMageEntity mage) {
        String name = mage.isArchmagister() ? "archmagister" : "mage_" + mage.element().name().toLowerCase(Locale.ROOT);
        return ElementalArcana.id("textures/entity/circle/" + name + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(CircleMageEntity mage) {
        return SKIN;
    }
}
