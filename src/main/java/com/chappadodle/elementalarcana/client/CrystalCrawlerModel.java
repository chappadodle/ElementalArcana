package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.CrystalCrawlerEntity;
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
 * A Crystal Crawler (64x32 texture, tools/gen_wild.py): a low body with three crystals standing up
 * along its back, a head with mandibles, and six long legs bent like a spider's. It scuttles in
 * alternating threes; for a volley it rears its front and its crystals tilt forward.
 */
public class CrystalCrawlerModel extends EntityModel<CrystalCrawlerEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("crystal_crawler"), "main");
    private static final int LEGS = 6;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[LEGS];
    private final ModelPart[] crystals = new ModelPart[3];

    public CrystalCrawlerModel(ModelPart root) {
        body = root.getChild("body");
        head = body.getChild("head");
        for (int i = 0; i < LEGS; i++) {
            legs[i] = body.getChild("leg_" + i);
        }
        for (int i = 0; i < crystals.length; i++) {
            crystals[i] = body.getChild("crystal_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -2.5f, -5, 8, 5, 10),
                PartPose.offset(0, 18, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-3, -2, -5, 6, 4, 5)
                        .texOffs(8, 16).addBox(-2.5f, 1, -7, 1, 1, 3)
                        .texOffs(8, 16).mirror().addBox(1.5f, 1, -7, 1, 1, 3),
                PartPose.offset(0, 0, -5));
        // Legs: three a side, front to back, angled out and down like a spider's.
        for (int i = 0; i < LEGS; i++) {
            boolean right = i % 2 == 0;
            float z = -3.5f + (i / 2) * 3.5f;
            CubeListBuilder leg = right ? CubeListBuilder.create().texOffs(36, 10).addBox(-12, -1, -1, 12, 2, 2)
                    : CubeListBuilder.create().texOffs(36, 10).mirror().addBox(0, -1, -1, 12, 2, 2);
            body.addOrReplaceChild("leg_" + i, leg, PartPose.offset(right ? -4 : 4, 1, z));
        }
        // Three crystals up its back, the middle one tallest.
        float[][] crystalPoses = {{0, -3.5f, -2.5f, -0.25f, 0.2f}, {0, -2.5f, 1, 0.1f, -0.15f}, {0, -2, 3.5f, 0.35f, 0.25f}};
        for (int i = 0; i < 3; i++) {
            float[] p = crystalPoses[i];
            body.addOrReplaceChild("crystal_" + i, CubeListBuilder.create().texOffs(0, 16).addBox(-1, -8, -1, 2, 8, 2),
                    PartPose.offsetAndRotation(p[0], p[1], p[2], p[3], 0.6f * i, p[4]));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(CrystalCrawlerEntity crawler, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5f;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;
        float stride = limbSwing * 1.2f;
        for (int i = 0; i < LEGS; i++) {
            boolean right = i % 2 == 0;
            // Alternating threes: front-right, middle-left, back-right step together.
            float phase = (i / 2 + (right ? 0 : 1)) % 2 == 0 ? 0 : Mth.PI;
            float step = Mth.cos(stride + phase) * 0.4f * limbSwingAmount;
            float lift = Math.max(0, Mth.sin(stride + phase)) * 0.4f * limbSwingAmount;
            legs[i].yRot = (right ? 1 : -1) * ((i / 2 - 1) * -0.45f + step);
            legs[i].zRot = (right ? 1 : -1) * (-0.6f + lift);
        }
        float rear = crawler.isHunched() ? -0.35f : 0f;
        body.xRot = rear;
        for (int i = 0; i < crystals.length; i++) {
            crystals[i].xRot = (i == 0 ? -0.25f : i == 1 ? 0.1f : 0.35f) + (crawler.isHunched() ? -0.4f : 0f)
                    + Mth.sin(ageInTicks * 0.05f + i) * 0.03f;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
