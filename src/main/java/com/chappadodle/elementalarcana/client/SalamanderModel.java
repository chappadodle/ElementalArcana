package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.SalamanderEntity;
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
 * An Ember Salamander (64x32 texture, tools/gen_wild.py): a long low body, a flat head with a jaw
 * that drops open, a tail in two segments and four legs splayed out to the sides. It scurries like
 * a lizard (legs in diagonal pairs, its body and tail swinging side to side), and gapes when it
 * spits (SalamanderEntity#isSpitting) or bites.
 */
public class SalamanderModel extends EntityModel<SalamanderEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("ember_salamander"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart rightFront;
    private final ModelPart leftFront;
    private final ModelPart rightHind;
    private final ModelPart leftHind;
    private float attack;

    public SalamanderModel(ModelPart root) {
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        tail = body.getChild("tail");
        tailTip = tail.getChild("tail_tip");
        rightFront = body.getChild("right_front_leg");
        leftFront = body.getChild("left_front_leg");
        rightHind = body.getChild("right_hind_leg");
        leftHind = body.getChild("left_hind_leg");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The body: 6 wide, 4 tall, 14 long, its belly 3 px off the ground; it faces -z.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -2, -7, 6, 4, 14),
                PartPose.offset(0, 19, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(40, 0).addBox(-2.5f, -2, -5, 5, 3, 5),
                PartPose.offset(0, -0.5f, -7));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(40, 8).addBox(-2.5f, 0, -4, 5, 1, 4),
                PartPose.offset(0, 1, -1));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 18).addBox(-2, -1.5f, 0, 4, 3, 8),
                PartPose.offset(0, -0.5f, 7));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(24, 18).addBox(-1, -1, 0, 2, 2, 8),
                PartPose.offset(0, 0.5f, 8));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(44, 18).addBox(-1, 0, -1, 2, 5, 2);
        CubeListBuilder mirrored = CubeListBuilder.create().texOffs(44, 18).mirror().addBox(-1, 0, -1, 2, 5, 2);
        body.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.5f, 1, -4.5f));
        body.addOrReplaceChild("left_front_leg", mirrored, PartPose.offset(3.5f, 1, -4.5f));
        body.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.5f, 1, 4.5f));
        body.addOrReplaceChild("left_hind_leg", mirrored, PartPose.offset(3.5f, 1, 4.5f));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void prepareMobModel(SalamanderEntity salamander, float limbSwing, float limbSwingAmount, float partialTick) {
        attack = salamander.getAttackAnim(partialTick);
    }

    @Override
    public void setupAnim(SalamanderEntity salamander, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float stride = limbSwing * 0.9f;
        float step = Mth.cos(stride) * 0.9f * limbSwingAmount;
        // Legs splay out sideways; diagonal pairs step together.
        rightFront.zRot = 0.7f;
        leftFront.zRot = -0.7f;
        rightHind.zRot = 0.7f;
        leftHind.zRot = -0.7f;
        rightFront.yRot = step;
        leftHind.yRot = step;
        leftFront.yRot = step;
        rightHind.yRot = step;
        rightFront.xRot = -step * 0.5f;
        leftHind.xRot = step * 0.5f;
        leftFront.xRot = step * 0.5f;
        rightHind.xRot = -step * 0.5f;
        // The body swings side to side as it runs; the tail answers it, and idly sways.
        float swing = Mth.sin(stride) * 0.25f * limbSwingAmount;
        body.yRot = swing;
        float idle = Mth.sin(ageInTicks * 0.08f) * 0.15f;
        tail.yRot = -swing * 1.6f + idle;
        tailTip.yRot = -swing * 1.4f + idle * 1.5f;
        tail.xRot = -0.08f;
        tailTip.xRot = 0.1f;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.8f - swing;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.6f;
        float gape = salamander.isSpitting() ? 0.7f : Mth.sin(attack * Mth.PI) * 0.8f;
        jaw.xRot = gape;
        head.xRot -= gape * 0.3f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
