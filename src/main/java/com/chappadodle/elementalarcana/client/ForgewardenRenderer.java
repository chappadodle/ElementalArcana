package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.creature.GolemEntity;
import com.chappadodle.elementalarcana.content.forge.ForgewardenEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the Forgewarden (see the Cinder Forges spec): an Elemental Golem's body at 1.75 times the
 * size, in its own skin of blackstone and magma (tools/gen_forge.py), its seams, core and eyes
 * glowing on top; molten, a second glow, its seams cracked wide.
 */
public class ForgewardenRenderer extends MobRenderer<GolemEntity, GolemModel> {
    private static final ResourceLocation SKIN = ElementalArcana.id("textures/entity/forgewarden/forgewarden.png");
    private static final RenderType GLOW = RenderType.eyes(ElementalArcana.id("textures/entity/forgewarden/forgewarden_glow.png"));
    private static final RenderType MOLTEN_GLOW = RenderType.eyes(ElementalArcana.id("textures/entity/forgewarden/forgewarden_molten.png"));
    private static final float SCALE = 1.75f;

    public ForgewardenRenderer(EntityRendererProvider.Context context) {
        super(context, new GolemModel(context.bakeLayer(GolemModel.LAYER)), 1.0f * SCALE);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }

            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, GolemEntity golem, float limbSwing,
                               float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                RenderType glow = golem instanceof ForgewardenEntity warden && warden.isMolten() ? MOLTEN_GLOW : GLOW;
                getParentModel().renderToBuffer(poseStack, buffers.getBuffer(glow), 0xF00000, OverlayTexture.NO_OVERLAY);
            }
        });
    }

    @Override
    protected void scale(GolemEntity golem, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(GolemEntity golem) {
        return SKIN;
    }
}
