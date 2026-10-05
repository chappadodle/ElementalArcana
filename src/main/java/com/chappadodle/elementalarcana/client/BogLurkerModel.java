package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.BogLurkerEntity;
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
 * A Bog Lurker (128x64 texture, tools/gen_wild.py): a broad flat body, a wide head with eyes bulging
 * on top and a heavy jaw, four short splayed legs and a thick tail in two segments. Its jaw gapes
 * when it bites, holds or spits; its tail sweeps as it swims.
 */
public class BogLurkerModel extends EntityModel<BogLurkerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("bog_lurker"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart[] legs = new ModelPart[4];
    private float attack;

    public BogLurkerModel(ModelPart root) {
        body = root.getChild("body");
        head = body.getChild("head");
        jaw = head.getChild("jaw");
        tail = body.getChild("tail");
        tailTip = tail.getChild("tail_tip");
        for (int i = 0; i < 4; i++) {
            legs[i] = body.getChild("leg_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -2.5f, -8, 12, 5, 16),
                PartPose.offset(0, 19, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-5, -3, -8, 10, 4, 8)
                        .texOffs(92, 0).addBox(-4, -5, -6, 2, 2, 2)
                        .texOffs(92, 0).mirror().addBox(2, -5, -6, 2, 2, 2),
                PartPose.offset(0, -0.5f, -8));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(56, 12).addBox(-5, 0, -8, 10, 2, 8),
                PartPose.offset(0, 1, 0));
        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 21).addBox(-3, -2, 0, 6, 4, 10),
                PartPose.offset(0, -0.5f, 8));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create().texOffs(32, 21).addBox(-2, -1.5f, 0, 4, 3, 8),
                PartPose.offset(0, 0.5f, 10));
        float[][] spots = {{-6, -5}, {6, -5}, {-6, 5}, {6, 5}};
        for (int i = 0; i < 4; i++) {
            boolean right = spots[i][0] < 0;
            CubeListBuilder leg = right ? CubeListBuilder.create().texOffs(92, 4).addBox(-3, 0, -1.5f, 3, 4, 3)
                    : CubeListBuilder.create().texOffs(92, 4).mirror().addBox(0, 0, -1.5f, 3, 4, 3);
            body.addOrReplaceChild("leg_" + i, leg, PartPose.offset(spots[i][0], 1, spots[i][1]));
        }
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void prepareMobModel(BogLurkerEntity lurker, float limbSwing, float limbSwingAmount, float partialTick) {
        attack = lurker.getAttackAnim(partialTick);
    }

    @Override
    public void setupAnim(BogLurkerEntity lurker, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3f;
        float sway = Mth.sin(limbSwing * 0.5f) * 0.35f * limbSwingAmount + Mth.sin(ageInTicks * 0.05f) * 0.08f;
        tail.yRot = sway;
        tailTip.yRot = sway * 1.4f;
        float gape = lurker.isHolding() ? 0.35f : lurker.isSpitting() ? 0.7f : Mth.sin(attack * Mth.PI) * 0.9f;
        jaw.xRot = gape;
        head.xRot -= gape * 0.25f;
        float step = Mth.cos(limbSwing * 0.7f) * 0.7f * limbSwingAmount;
        for (int i = 0; i < 4; i++) {
            boolean right = i % 2 == 0;
            legs[i].zRot = (right ? 1 : -1) * 0.6f;
            legs[i].yRot = (i == 0 || i == 3 ? step : -step);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
