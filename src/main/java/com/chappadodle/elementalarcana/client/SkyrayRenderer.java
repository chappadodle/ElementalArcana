package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wonder.SkyrayEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A skyray: deep blue above with bands of spots that glow (a layer drawn full bright), pitched to
 * its climb and banking into its turns. It casts no shadow: it's always far above the ground.
 */
public class SkyrayRenderer extends MobRenderer<SkyrayEntity, SkyrayModel> {
    private static final ResourceLocation TEXTURE = ElementalArcana.id("textures/entity/skyray.png");
    private static final RenderType GLOW = RenderType.eyes(ElementalArcana.id("textures/entity/skyray_glow.png"));

    public SkyrayRenderer(EntityRendererProvider.Context context) {
        super(context, new SkyrayModel(context.bakeLayer(SkyrayModel.LAYER)), 0f);
        addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(SkyrayEntity skyray) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(SkyrayEntity skyray, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(skyray, poseStack, bob, yBodyRot, partialTick, scale);
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, skyray.xRotO, skyray.getXRot())));
        float turn = Mth.clamp(Mth.wrapDegrees(skyray.yBodyRot - skyray.yBodyRotO) * 8f, -25f, 25f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(turn));
    }
}
