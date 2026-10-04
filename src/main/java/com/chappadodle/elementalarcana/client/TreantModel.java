package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.WildRules;
import com.chappadodle.elementalarcana.content.wild.TreantEntity;
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
 * A Thornwood Treant (128x64 texture, tools/gen_wild.py): a bark trunk with a face in it, a wide
 * crown of leaves, two long branch-arms with twigs and two stumpy root-legs; three blocks tall.
 * Asleep its arms are raised into the crown like boughs and nothing moves but the leaves in the
 * wind. Awake it lumbers, swings its arms to grasp, and for a stamp lifts both arms and brings them
 * down as it stamps (TreantEntity#stampTicks).
 */
public class TreantModel extends EntityModel<TreantEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("thornwood_treant"), "main");
    /** Asleep, the arms reach up and out beside the crown as boughs. */
    private static final float BOUGH = 2.6f;

    private final ModelPart trunk;
    private final ModelPart crown;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private float attack;

    public TreantModel(ModelPart root) {
        trunk = root.getChild("trunk");
        crown = trunk.getChild("crown");
        rightArm = trunk.getChild("right_arm");
        leftArm = trunk.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The trunk pivots at the hips (y 10): 12 wide, 20 tall, 10 deep.
        PartDefinition trunk = root.addOrReplaceChild("trunk", CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-6, -20, -5, 12, 20, 10),
                PartPose.offset(0, 10, 0));
        // The crown sits on top and hangs over the shoulders.
        trunk.addOrReplaceChild("crown", CubeListBuilder.create().texOffs(0, 0).addBox(-9, -14, -9, 18, 14, 18),
                PartPose.offset(0, -18, 0));
        // Arms hang from the shoulders, each with a twig near its end.
        trunk.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-4, -2, -2, 4, 20, 4)
                        .texOffs(40, 32).addBox(-6, 10, -1, 2, 6, 2),
                PartPose.offset(-6, -17, 0));
        trunk.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 32).mirror().addBox(0, -2, -2, 4, 20, 4)
                        .texOffs(40, 32).mirror().addBox(4, 10, -1, 2, 6, 2),
                PartPose.offset(6, -17, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(16, 32).addBox(-3, 0, -3, 6, 14, 6),
                PartPose.offset(-3, 10, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 32).mirror().addBox(-3, 0, -3, 6, 14, 6),
                PartPose.offset(3, 10, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void prepareMobModel(TreantEntity treant, float limbSwing, float limbSwingAmount, float partialTick) {
        attack = treant.getAttackAnim(partialTick);
    }

    @Override
    public void setupAnim(TreantEntity treant, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // The crown stirs in the wind either way.
        crown.zRot = Mth.sin(ageInTicks * 0.04f + treant.getId()) * 0.02f;
        crown.xRot = Mth.cos(ageInTicks * 0.03f + treant.getId()) * 0.015f;
        if (!treant.isAwake()) {
            trunk.xRot = 0;
            trunk.yRot = 0;
            rightArm.xRot = 0;
            leftArm.xRot = 0;
            rightArm.zRot = BOUGH;
            leftArm.zRot = -BOUGH;
            rightLeg.xRot = 0;
            leftLeg.xRot = 0;
            return;
        }
        float walk = Mth.cos(limbSwing * 0.35f) * 0.9f * limbSwingAmount;
        rightLeg.xRot = walk;
        leftLeg.xRot = -walk;
        trunk.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.3f;
        trunk.xRot = 0.05f;
        float arms = -walk * 0.7f;
        rightArm.zRot = 0.12f;
        leftArm.zRot = -0.12f;
        // The stamp: arms rise over the windup and come down as it strikes.
        int stamp = treant.stampTicks();
        if (stamp > 0) {
            float t = Math.min(1f, stamp / (float) WildRules.ROOT_WINDUP_TICKS);
            arms = -2.7f * t;
            trunk.xRot = -0.2f * t;
            rightLeg.xRot = -0.6f * t;
            leftLeg.xRot = 0;
        } else if (attack > 0) {
            // A grasp: both arms sweep down and in from overhead.
            float swing = Mth.sin(attack * Mth.PI);
            arms = -2.0f * swing + attack * 0.4f;
            rightArm.zRot = 0.12f + swing * 0.5f;
            leftArm.zRot = -0.12f - swing * 0.5f;
            trunk.xRot = 0.05f + swing * 0.25f;
        }
        float sway = Mth.sin(ageInTicks * 0.06f) * 0.05f;
        rightArm.xRot = arms + sway;
        leftArm.xRot = (stamp > 0 || attack > 0 ? arms : -arms) - sway;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        trunk.render(poseStack, buffer, packedLight, packedOverlay, color);
        rightLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
        leftLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
