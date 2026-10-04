package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.drake.DrakeEntity;
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
 * An Elemental Drake (256x128 texture, tools/gen_drakes.py): a wyvern in blocks, about 6 blocks from
 * snout to tail with wings 8 blocks across. A long body with the wings at its shoulders, each in two
 * jointed parts with a membrane behind the bone; a neck and a horned head whose lower jaw drops; a
 * tail in three segments ending in a fin; two legs. In the air its wings beat (slowly while it
 * soars, hard as it climbs) and its legs tuck back; on the ground the wings fold along its sides and
 * it walks. Breathing, its jaw opens wide and its head reaches forward; resting, its head lies low.
 */
public class DrakeModel extends EntityModel<DrakeEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("drake"), "main");

    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightLeg;
    private final ModelPart rightShin;
    private final ModelPart leftLeg;
    private final ModelPart leftShin;

    public DrakeModel(ModelPart root) {
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        jaw = head.getChild("jaw");
        tail1 = body.getChild("tail1");
        tail2 = tail1.getChild("tail2");
        tail3 = tail2.getChild("tail3");
        rightWing = body.getChild("right_wing");
        rightWingTip = rightWing.getChild("right_wing_tip");
        leftWing = body.getChild("left_wing");
        leftWingTip = leftWing.getChild("left_wing_tip");
        rightLeg = body.getChild("right_leg");
        rightShin = rightLeg.getChild("right_shin");
        leftLeg = body.getChild("left_leg");
        leftShin = leftLeg.getChild("left_shin");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-7, -6, -16, 14, 12, 32)
                        // A ridge of spines down its back.
                        .texOffs(96, 50).addBox(-1, -9, -12, 2, 3, 24),
                PartPose.offset(0, 10, 0));

        PartDefinition neck = body.addOrReplaceChild("neck", CubeListBuilder.create()
                .texOffs(0, 48).addBox(-4, -4, -14, 8, 8, 14), PartPose.offsetAndRotation(0, -2, -16, 0.35f, 0, 0));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(48, 48).addBox(-5, -5, -12, 10, 8, 12)
                        .texOffs(200, 40).addBox(-4, -10, -3, 2, 6, 2)
                        .texOffs(200, 40).mirror().addBox(2, -10, -3, 2, 6, 2),
                PartPose.offsetAndRotation(0, 0, -14, -0.35f, 0, 0));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(48, 72).addBox(-4, 0, -10, 8, 3, 10),
                PartPose.offset(0, 3, -2));

        PartDefinition tail1 = body.addOrReplaceChild("tail1", CubeListBuilder.create()
                .texOffs(96, 0).addBox(-5, -4, 0, 10, 8, 16), PartPose.offset(0, -1, 16));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2", CubeListBuilder.create()
                .texOffs(96, 26).addBox(-3, -3, 0, 6, 6, 16), PartPose.offset(0, 0, 16));
        tail2.addOrReplaceChild("tail3", CubeListBuilder.create()
                        .texOffs(150, 0).addBox(-2, -2, 0, 4, 4, 16)
                        .texOffs(150, 22).addBox(-5, -0.5f, 9, 10, 1, 8),
                PartPose.offset(0, 0, 16));

        for (int side = -1; side <= 1; side += 2) {
            String name = side < 0 ? "right" : "left";
            CubeListBuilder wing = CubeListBuilder.create().texOffs(0, 90);
            CubeListBuilder tip = CubeListBuilder.create().texOffs(110, 90);
            if (side > 0) {
                wing.mirror();
                tip.mirror();
            }
            float reach = side < 0 ? -32 : 0;
            float tipReach = side < 0 ? -30 : 0;
            wing.addBox(reach, -2, -2, 32, 3, 4).texOffs(0, 98).addBox(reach, 0, 0, 32, 1, 22);
            tip.addBox(tipReach, -1, -1, 30, 2, 3).texOffs(110, 96).addBox(tipReach, 0, 0, 30, 1, 18);
            PartDefinition wingPart = body.addOrReplaceChild(name + "_wing", wing, PartPose.offset(7 * side, -5, -6));
            wingPart.addOrReplaceChild(name + "_wing_tip", tip, PartPose.offset(32 * side, 0, 0));

            CubeListBuilder thigh = CubeListBuilder.create().texOffs(192, 0);
            CubeListBuilder shin = CubeListBuilder.create().texOffs(192, 12);
            if (side > 0) {
                thigh.mirror();
                shin.mirror();
            }
            PartDefinition leg = body.addOrReplaceChild(name + "_leg", thigh.addBox(-2, 0, -3, 4, 6, 5), PartPose.offset(5 * side, 4, 6));
            leg.addOrReplaceChild(name + "_shin", shin.addBox(-1.5f, 0, -1.5f, 3, 4, 3).texOffs(192, 20).addBox(-2, 2, -4, 4, 2, 5),
                    PartPose.offset(0, 6, 0));
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public void setupAnim(DrakeEntity drake, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float partial = ageInTicks - drake.tickCount;
        boolean flying = drake.isFlyingPose();
        boolean resting = drake.isResting();
        float breath = drake.breathOpen(partial);

        // Wings: beating in the air (hard when climbing), folded along its sides on the ground.
        if (flying) {
            float rate = drake.isClimbing() ? 0.45f : 0.22f;
            float beat = Mth.sin(ageInTicks * rate);
            float depth = drake.isClimbing() ? 0.75f : 0.45f;
            rightWing.zRot = beat * depth;
            leftWing.zRot = -beat * depth;
            rightWing.yRot = 0f;
            leftWing.yRot = 0f;
            float lag = Mth.sin(ageInTicks * rate - 0.9f) * depth * 0.7f;
            rightWingTip.zRot = lag;
            leftWingTip.zRot = -lag;
            rightWingTip.yRot = 0f;
            leftWingTip.yRot = 0f;
            rightLeg.xRot = 1.1f;
            leftLeg.xRot = 1.1f;
            rightShin.xRot = 0.6f;
            leftShin.xRot = 0.6f;
        } else {
            // Swept back along its sides and drooping.
            rightWing.zRot = -0.5f;
            leftWing.zRot = 0.5f;
            rightWing.yRot = 1.2f;
            leftWing.yRot = -1.2f;
            rightWingTip.zRot = 0f;
            leftWingTip.zRot = 0f;
            rightWingTip.yRot = 1.8f;
            leftWingTip.yRot = -1.8f;
            float walk = Mth.cos(limbSwing * 0.5f) * 0.9f * limbSwingAmount;
            rightLeg.xRot = walk;
            leftLeg.xRot = -walk;
            rightShin.xRot = 0f;
            leftShin.xRot = 0f;
        }

        // Neck and head: looking where it looks; reaching forward to breathe; low when resting.
        float lookYaw = Mth.clamp(netHeadYaw, -50f, 50f) * Mth.DEG_TO_RAD;
        neck.yRot = lookYaw * 0.5f;
        head.yRot = lookYaw * 0.5f;
        float neckPitch = resting ? 0.75f : 0.35f - breath * 0.45f;
        neck.xRot = neckPitch + Mth.sin(ageInTicks * 0.06f) * 0.04f;
        head.xRot = (resting ? -0.6f : -0.35f + breath * 0.25f) + headPitch * Mth.DEG_TO_RAD * 0.3f;
        jaw.xRot = 0.05f + breath * 0.75f + (resting ? 0f : Mth.sin(ageInTicks * 0.1f) * 0.02f);

        // The tail sways, more on the ground.
        float sway = Mth.sin(ageInTicks * 0.09f) * (flying ? 0.12f : 0.22f);
        tail1.yRot = sway;
        tail2.yRot = Mth.sin(ageInTicks * 0.09f - 0.6f) * (flying ? 0.16f : 0.28f);
        tail3.yRot = Mth.sin(ageInTicks * 0.09f - 1.2f) * (flying ? 0.2f : 0.34f);
        float droop = flying ? 0.05f : 0.18f;
        tail1.xRot = -droop * 0.5f;
        tail2.xRot = droop;
        tail3.xRot = droop * 0.5f;

        // In flight the body follows its climb and dive (drake.bodyPitch); on the ground it's level.
        body.xRot = flying ? drake.bodyPitch(partial) * Mth.DEG_TO_RAD : 0f;
        body.zRot = flying ? drake.bank(partial) * Mth.DEG_TO_RAD : 0f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
