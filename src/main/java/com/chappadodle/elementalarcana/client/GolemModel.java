package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.GolemRules;
import com.chappadodle.elementalarcana.content.creature.GolemEntity;
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
 * An Elemental Golem's body (128x64 texture, tools/gen_creatures.py): short thick legs, a broad
 * chest with a core set in its front, long arms hanging nearly to the ground and a small head sunk
 * between the shoulders; about 2.3 blocks tall. It walks with a heavy swing; for a slam it raises
 * both arms over its head through the warning, then brings them down (GolemEntity#swingTicks).
 */
public class GolemModel extends EntityModel<GolemEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("golem"), "main");
    /** How far the arms swing up for a slam (radians), and how far past straight down they strike. */
    private static final float RAISED = -2.8f;
    private static final float STRUCK = 0.5f;
    private static final float BLOW_TICKS = 3f;
    private static final float RECOVER_TICKS = 8f;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public GolemModel(ModelPart root) {
        body = root.getChild("body");
        head = body.getChild("head");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The body pivots at the hips (y 10), so it can lean into a slam.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-7, -17, -4, 14, 17, 8)
                        .texOffs(32, 0).addBox(-2, -12, -5, 4, 4, 1),
                PartPose.offset(0, 10, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -6, -4, 8, 8, 8),
                PartPose.offset(0, -17, -1));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(44, 16).addBox(-6, -2, -3, 6, 20, 6),
                PartPose.offset(-7, -15, 0));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(44, 16).mirror().addBox(0, -2, -3, 6, 20, 6),
                PartPose.offset(7, -15, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 41).addBox(-3, 0, -3, 6, 14, 6),
                PartPose.offset(-4, 10, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 41).mirror().addBox(-3, 0, -3, 6, 14, 6),
                PartPose.offset(4, 10, 0));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(GolemEntity golem, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6f;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6f;
        float walk = Mth.cos(limbSwing * 0.45f) * 1.1f * limbSwingAmount;
        rightLeg.xRot = walk;
        leftLeg.xRot = -walk;
        // Arms: a heavy sway as it walks, unless it's slamming.
        float sway = Mth.sin(ageInTicks * 0.05f) * 0.05f;
        float arms = -walk * 0.6f;
        float lean = 0f;
        float swing = golem.swingTicks(ageInTicks - golem.tickCount);
        float windup = GolemRules.WINDUP_TICKS;
        if (swing < windup) {
            // Rising over the warning.
            float t = swing / windup;
            arms = RAISED * (1f - (1f - t) * (1f - t));
            lean = -0.15f * t;
        } else if (swing < windup + BLOW_TICKS) {
            // The blow.
            float t = (swing - windup) / BLOW_TICKS;
            arms = Mth.lerp(t, RAISED, STRUCK);
            lean = Mth.lerp(t, -0.15f, 0.35f);
        } else if (swing < windup + BLOW_TICKS + RECOVER_TICKS) {
            float t = (swing - windup - BLOW_TICKS) / RECOVER_TICKS;
            arms = Mth.lerp(t, STRUCK, 0f);
            lean = Mth.lerp(t, 0.35f, 0f);
        }
        rightArm.xRot = arms + sway;
        leftArm.xRot = arms - sway;
        rightArm.zRot = 0.06f;
        leftArm.zRot = -0.06f;
        body.xRot = lean;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
        rightLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
        leftLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
