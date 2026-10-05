package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.HarpyEntity;
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
 * A Gale Harpy (64x64 texture, tools/gen_wild.py): a feathered body, a head with a swept-back crest,
 * wings for arms (a bone and a broad feathered vane), a bird's legs with talons and a fan of tail
 * feathers. In flight its wings beat and its legs tuck back; diving or carrying, its talons reach
 * down; for a gust, its wings spread wide and sweep forward.
 */
public class HarpyModel extends EntityModel<HarpyEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("gale_harpy"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tail;

    public HarpyModel(ModelPart root) {
        body = root.getChild("body");
        head = body.getChild("head");
        rightWing = body.getChild("right_wing");
        leftWing = body.getChild("left_wing");
        rightLeg = body.getChild("right_leg");
        leftLeg = body.getChild("left_leg");
        tail = body.getChild("tail");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The body: shoulders at y 0 of the part; its talons reach the ground (y 24).
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 12).addBox(-3, 0, -2, 6, 9, 4),
                PartPose.offset(0, 7, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3, -6, -3, 6, 6, 6)
                        .texOffs(24, 0).addBox(-1, -8, -1, 2, 5, 6),
                PartPose.offset(0, 0, 0));
        // Each wing: its bone down the arm, the feathered vane behind it.
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                        .texOffs(40, 0).addBox(-2, -1, -1, 2, 9, 2)
                        .texOffs(0, 26).mirror().addBox(-1.5f, 1, 1, 1, 12, 6),
                PartPose.offset(-3, 1, 0));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                        .texOffs(40, 0).mirror().addBox(0, -1, -1, 2, 9, 2)
                        .texOffs(0, 26).addBox(0.5f, 1, 1, 1, 12, 6),
                PartPose.offset(3, 1, 0));
        body.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(20, 12).addBox(-1, 0, -1, 2, 7, 2)
                        .texOffs(28, 12).addBox(-1.5f, 7, -3, 3, 1, 4),
                PartPose.offset(-1.5f, 9, 0));
        body.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(20, 12).mirror().addBox(-1, 0, -1, 2, 7, 2)
                        .texOffs(28, 12).mirror().addBox(-1.5f, 7, -3, 3, 1, 4),
                PartPose.offset(1.5f, 9, 0));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(20, 26).addBox(-2.5f, 0, 0, 5, 1, 5),
                PartPose.offsetAndRotation(0, 8, 2, 0.5f, 0, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HarpyEntity harpy, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        boolean flying = !harpy.onGround();
        float speed = Math.min(1f, limbSwingAmount * 2f);
        body.xRot = flying ? 0.2f + speed * 0.35f : 0.05f;
        int gust = harpy.gustTicks();
        float beat;
        if (gust > 0) {
            // Spread wide through the windup, then a sweep forward.
            float t = Math.min(1f, gust / 12f);
            beat = 1.35f - t * 0.2f;
            rightWing.xRot = -t * 0.9f;
            leftWing.xRot = -t * 0.9f;
        } else if (flying) {
            beat = 0.45f + Mth.sin(ageInTicks * 0.9f) * 0.85f;
            rightWing.xRot = 0.1f;
            leftWing.xRot = 0.1f;
        } else {
            beat = 0.12f;
            rightWing.xRot = 0.05f;
            leftWing.xRot = 0.05f;
        }
        rightWing.zRot = beat;
        leftWing.zRot = -beat;
        // Legs: tucked back in flight, reaching down to grab when diving or carrying.
        float legs;
        if (harpy.isCarrying()) {
            legs = -0.35f;
        } else if (flying) {
            legs = harpy.isAggressive() && speed > 0.6f ? -0.2f : 0.9f;
        } else {
            legs = Mth.cos(limbSwing * 0.8f) * 0.6f * limbSwingAmount;
        }
        rightLeg.xRot = flying ? legs : legs;
        leftLeg.xRot = flying ? legs : -legs;
        tail.xRot = 0.5f + (flying ? Mth.sin(ageInTicks * 0.2f) * 0.08f : 0f);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
