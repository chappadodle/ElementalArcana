package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.end.StargazerEntity;
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
 * A Stargazer (64x64 texture, tools/gen_end.py): tall and thin, nearly three blocks, a long robe
 * flaring at the hem, long thin arms hanging still at its sides, and a head with a face of
 * starlight. It barely moves: a slow sway of its arms and robe as it drifts.
 */
public class StargazerModel extends EntityModel<StargazerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("stargazer"), "main");

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart hem;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public StargazerModel(ModelPart root) {
        head = root.getChild("head");
        body = root.getChild("body");
        hem = root.getChild("hem");
        rightArm = root.getChild("right_arm");
        leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, -14, 0));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4.5f, 0, -2.5f, 9, 26, 5), PartPose.offset(0, -14, 0));
        root.addOrReplaceChild("hem", CubeListBuilder.create().texOffs(28, 16).addBox(-5.5f, 0, -3.5f, 11, 12, 7), PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(28, 35).addBox(-1.5f, 0, -1.5f, 3, 22, 3),
                PartPose.offset(-6, -13, 0));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(28, 35).mirror().addBox(-1.5f, 0, -1.5f, 3, 22, 3),
                PartPose.offset(6, -13, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(StargazerEntity gazer, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        float sway = Mth.sin(ageInTicks * 0.05f) * 0.06f;
        rightArm.xRot = sway;
        leftArm.xRot = -sway;
        rightArm.zRot = 0.06f;
        leftArm.zRot = -0.06f;
        if (gazer.isAggressive()) {
            // Angered: arms raised a little toward its foe.
            rightArm.xRot = -0.5f + sway;
            leftArm.xRot = -0.5f - sway;
        }
        hem.xRot = Mth.sin(ageInTicks * 0.04f) * 0.03f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        head.render(poseStack, buffer, packedLight, packedOverlay, color);
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
        hem.render(poseStack, buffer, packedLight, packedOverlay, color);
        rightArm.render(poseStack, buffer, packedLight, packedOverlay, color);
        leftArm.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
