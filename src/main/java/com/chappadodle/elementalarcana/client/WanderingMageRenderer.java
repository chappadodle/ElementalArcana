package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WanderingTraderRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.WanderingTrader;

/**
 * A Wandering Mage (see its spec): the wandering trader as Minecraft draws it, with the mage's
 * clothes over it (a midnight-blue robe and wide hat, silver at the band and the hem; drawn by
 * tools/gen_people.py), as a villager's profession clothes go over the villager.
 */
public class WanderingMageRenderer extends WanderingTraderRenderer {
    private static final ResourceLocation CLOTHES = ElementalArcana.id("textures/entity/wandering_mage.png");

    public WanderingMageRenderer(EntityRendererProvider.Context context) {
        super(context);
        addLayer(new RenderLayer<WanderingTrader, VillagerModel<WanderingTrader>>(this) {
            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffers, int light, WanderingTrader trader, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (!trader.isInvisible()) {
                    renderColoredCutoutModel(getParentModel(), CLOTHES, poseStack, buffers, light, trader, -1);
                }
            }
        });
    }
}
