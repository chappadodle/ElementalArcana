package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wonder.SkyrayEntity;
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
 * A skyray (128x128 texture, tools/gen_wonders.py): a broad flat body with two horn-like fins in
 * front, each wing in two parts (so a slow wave runs out along it as it beats) and a long thin tail.
 * Five blocks from wingtip to wingtip.
 */
public class SkyrayModel extends EntityModel<SkyrayEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("skyray"), "main");

    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart rightTip;
    private final ModelPart leftWing;
    private final ModelPart leftTip;
    private final ModelPart tail;

    public SkyrayModel(ModelPart root) {
        body = root.getChild("body");
        rightWing = body.getChild("right_wing");
        rightTip = rightWing.getChild("tip");
        leftWing = body.getChild("left_wing");
        leftTip = leftWing.getChild("tip");
        tail = body.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-10, -2, -14, 20, 4, 28)
                        .texOffs(80, 32).addBox(-8, -1, -20, 2, 3, 6)
                        .texOffs(80, 32).mirror().addBox(6, -1, -20, 2, 3, 6),
                PartPose.offset(0, 21, 0));
        PartDefinition right = body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 32).mirror()
                .addBox(-16, -1, -10, 16, 2, 20), PartPose.offset(-10, 0, 0));
        right.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 56).mirror().addBox(-14, -0.5f, -8, 14, 1, 16),
                PartPose.offset(-16, 0, 1));
        PartDefinition left = body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 32)
                .addBox(0, -1, -10, 16, 2, 20), PartPose.offset(10, 0, 0));
        left.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 56).addBox(0, -0.5f, -8, 14, 1, 16),
                PartPose.offset(16, 0, 1));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 80).addBox(-0.5f, -0.5f, 0, 1, 1, 36),
                PartPose.offset(0, 0, 14));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(SkyrayEntity skyray, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float t = ageInTicks * 0.12f + skyray.getId();
        float inner = Mth.sin(t) * 0.28f;
        float outer = Mth.sin(t - 0.7f) * 0.38f;
        leftWing.zRot = -inner;
        leftTip.zRot = -outer;
        rightWing.zRot = inner;
        rightTip.zRot = outer;
        tail.xRot = Mth.sin(t * 0.8f) * 0.12f;
        tail.yRot = Mth.sin(t * 0.5f) * 0.1f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
