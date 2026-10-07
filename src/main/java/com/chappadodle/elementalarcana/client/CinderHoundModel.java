package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.CinderHoundEntity;
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
 * A Cinder Hound (64x32 texture, tools/gen_nether.py): a long lean body with a ridge of cinders down
 * its back, a narrow head with a long snout and pointed ears, four long legs and a tail held low. It
 * lopes with its head down; leaping, its legs stretch out fore and aft.
 */
public class CinderHoundModel extends EntityModel<CinderHoundEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("cinder_hound"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart frontRight;
    private final ModelPart frontLeft;
    private final ModelPart backRight;
    private final ModelPart backLeft;

    public CinderHoundModel(ModelPart root) {
        body = root.getChild("body");
        head = root.getChild("head");
        tail = root.getChild("tail");
        frontRight = root.getChild("front_right");
        frontLeft = root.getChild("front_left");
        backRight = root.getChild("back_right");
        backLeft = root.getChild("back_left");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3, -3, -7, 6, 6, 14)
                        .texOffs(40, 0).addBox(-1, -5, -5, 2, 2, 10),
                PartPose.offset(0, 12, 0));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-3, -3, -5, 6, 6, 5)
                        .texOffs(22, 20).addBox(-1.5f, -0.5f, -9, 3, 3, 4)
                        .texOffs(36, 20).addBox(-3, -5, -2, 2, 2, 1)
                        .texOffs(36, 20).mirror().addBox(1, -5, -2, 2, 2, 1),
                PartPose.offset(0, 10, -7));
        root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(50, 12).addBox(-1, 0, -1, 2, 8, 2),
                PartPose.offsetAndRotation(0, 10, 7, 0.9f, 0, 0));
        root.addOrReplaceChild("front_right", CubeListBuilder.create().texOffs(42, 12).addBox(-1, 0, -1, 2, 10, 2),
                PartPose.offset(-2, 14, -5));
        root.addOrReplaceChild("front_left", CubeListBuilder.create().texOffs(42, 12).mirror().addBox(-1, 0, -1, 2, 10, 2),
                PartPose.offset(2, 14, -5));
        root.addOrReplaceChild("back_right", CubeListBuilder.create().texOffs(42, 12).addBox(-1, 0, -1, 2, 10, 2),
                PartPose.offset(-2, 14, 5));
        root.addOrReplaceChild("back_left", CubeListBuilder.create().texOffs(42, 12).mirror().addBox(-1, 0, -1, 2, 10, 2),
                PartPose.offset(2, 14, 5));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(CinderHoundEntity hound, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD + 0.1f;
        if (!hound.onGround()) {
            // A leap: forelegs reaching, hind legs pushing off.
            frontRight.xRot = -1.2f;
            frontLeft.xRot = -1.2f;
            backRight.xRot = 1.1f;
            backLeft.xRot = 1.1f;
            tail.xRot = 1.4f;
            return;
        }
        float stride = Mth.cos(limbSwing * 0.7f) * 1.2f * limbSwingAmount;
        frontRight.xRot = stride;
        frontLeft.xRot = -stride;
        backRight.xRot = -stride;
        backLeft.xRot = stride;
        tail.xRot = 0.9f + Mth.sin(ageInTicks * 0.15f) * 0.1f;
        tail.yRot = Mth.cos(limbSwing * 0.7f) * 0.3f * limbSwingAmount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
        head.render(poseStack, buffer, packedLight, packedOverlay, color);
        tail.render(poseStack, buffer, packedLight, packedOverlay, color);
        frontRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        frontLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
        backRight.render(poseStack, buffer, packedLight, packedOverlay, color);
        backLeft.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
