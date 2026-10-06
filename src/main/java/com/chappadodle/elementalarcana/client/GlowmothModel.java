package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wonder.GlowmothEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A glowmoth (32x32 texture per element, tools/gen_wonders.py): a slender body, two feathery
 * antennae, and two broad wings that beat fast.
 */
public class GlowmothModel extends EntityModel<GlowmothEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("glowmoth"), "main");

    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;

    public GlowmothModel(ModelPart root) {
        body = root.getChild("body");
        rightWing = body.getChild("right_wing");
        leftWing = body.getChild("left_wing");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-0.5f, -0.5f, -2, 1, 1, 4)
                        .texOffs(0, 6).addBox(-1.5f, -2.5f, -3, 1, 2, 0)
                        .texOffs(0, 6).mirror().addBox(0.5f, -2.5f, -3, 1, 2, 0),
                PartPose.offset(0, 21, 0));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-5, 0, -2, 5, 0, 5),
                PartPose.offset(-0.5f, -0.5f, 0));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 16).addBox(0, 0, -2, 5, 0, 5),
                PartPose.offset(0.5f, -0.5f, 0));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(GlowmothEntity moth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float beat = Mth.sin(ageInTicks * 1.9f + moth.getId()) * 0.9f + 0.2f;
        leftWing.zRot = -beat;
        rightWing.zRot = beat;
        body.xRot = 0.15f + Mth.sin(ageInTicks * 0.3f) * 0.05f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
