package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.sanctum.SovereignEntity;
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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * A Sovereign, Minecraft style, drawn at half size (SovereignRenderer doubles it): a mask over a
 * see-through shell with its element's core spinning inside, a crown of five shards over the mask,
 * two floating hands that rise when it casts, four plates of its element circling it and a tail
 * tapering away below. Translucent (tools/gen_sovereigns.py draws the texture).
 */
public class SovereignModel extends EntityModel<SovereignEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("sovereign"), "main");
    private static final float MIDDLE_Y = 9f;
    private static final int SHARDS = 5;
    private static final int PLATES = 4;

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart shell;
    private final ModelPart mask;
    private final ModelPart[] shards = new ModelPart[SHARDS];
    private final ModelPart leftHand;
    private final ModelPart rightHand;
    private final ModelPart[] plates = new ModelPart[PLATES];
    private final ModelPart tail;
    private final ModelPart tailTip;
    private boolean casting;

    public SovereignModel(ModelPart model) {
        super(RenderType::entityTranslucent);
        root = model.getChild("root");
        core = root.getChild("core");
        shell = root.getChild("shell");
        mask = root.getChild("mask");
        for (int i = 0; i < SHARDS; i++) {
            shards[i] = mask.getChild("shard" + i);
        }
        leftHand = root.getChild("left_hand");
        rightHand = root.getChild("right_hand");
        for (int i = 0; i < PLATES; i++) {
            plates[i] = root.getChild("plate" + i);
        }
        tail = root.getChild("tail");
        tailTip = tail.getChild("tip");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, MIDDLE_Y, 0));
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -5, -5, 10, 10, 10), PartPose.ZERO);
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(40, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.ZERO);
        PartDefinition mask = root.addOrReplaceChild("mask", CubeListBuilder.create().texOffs(0, 20).addBox(-5, -7, -2, 10, 12, 2),
                PartPose.offset(0, 0, -5.5f));
        for (int i = 0; i < SHARDS; i++) {
            float x = (i - 2) * 2.5f;
            float height = i == 2 ? 5 : i == 1 || i == 3 ? 4 : 3;
            mask.addOrReplaceChild("shard" + i, CubeListBuilder.create().texOffs(24, 28).addBox(-0.5f, -height, -0.5f, 1, height, 1),
                    PartPose.offset(x, -8, -1));
        }
        root.addOrReplaceChild("left_hand", CubeListBuilder.create().texOffs(24, 20).addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(9, 2, -2));
        root.addOrReplaceChild("right_hand", CubeListBuilder.create().texOffs(24, 20).mirror().addBox(-2, -2, -2, 4, 4, 4), PartPose.offset(-9, 2, -2));
        for (int i = 0; i < PLATES; i++) {
            root.addOrReplaceChild("plate" + i, CubeListBuilder.create().texOffs(28, 28).addBox(-2, -2, -0.5f, 4, 4, 1), PartPose.ZERO);
        }
        PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(40, 20).addBox(-2, 0, -2, 4, 4, 4),
                PartPose.offset(0, 5.5f, 0));
        tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(40, 28).addBox(-1, 0, -1, 2, 3, 2), PartPose.offset(0, 4.5f, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void prepareMobModel(SovereignEntity sovereign, float limbSwing, float limbSwingAmount, float partialTick) {
        casting = sovereign.isCasting();
    }

    @Override
    public void setupAnim(SovereignEntity sovereign, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.y = MIDDLE_Y + Mth.sin(ageInTicks * 0.08f) * 1.2f;
        core.yRot = -ageInTicks * 0.07f;
        core.xRot = ageInTicks * 0.04f;
        shell.yRot = ageInTicks * 0.02f;
        mask.xRot = headPitch * Mth.DEG_TO_RAD * 0.5f;
        for (int i = 0; i < SHARDS; i++) {
            shards[i].y = -8 - Mth.sin(ageInTicks * 0.1f + i * 1.3f) * 0.6f;
        }
        // Hands: low at its sides, raised and forward while it casts.
        float lift = casting ? -8 : Mth.sin(ageInTicks * 0.09f) * 0.8f;
        float reach = casting ? -6 : -2;
        float spread = casting ? 7.5f : 9;
        leftHand.setPos(spread, 2 + lift, reach);
        rightHand.setPos(-spread, 2 + lift + (casting ? 0 : Mth.sin(ageInTicks * 0.09f + 1.5f) * 0.8f), reach);
        leftHand.yRot = ageInTicks * (casting ? 0.3f : 0.03f);
        rightHand.yRot = -leftHand.yRot;
        for (int i = 0; i < PLATES; i++) {
            float angle = ageInTicks * 0.05f + i * Mth.HALF_PI;
            plates[i].setPos(Mth.cos(angle) * 11, Mth.sin(angle * 2 + i) * 1.5f, Mth.sin(angle) * 11);
            plates[i].yRot = -angle + Mth.HALF_PI;
        }
        tail.xRot = Mth.sin(ageInTicks * 0.07f) * 0.15f;
        tail.zRot = Mth.cos(ageInTicks * 0.05f) * 0.12f;
        tailTip.xRot = Mth.sin(ageInTicks * 0.07f + 1) * 0.2f;
    }

    // Everything solid first, the shell last, so the core shows through it.
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        poseStack.pushPose();
        root.translateAndRotate(poseStack);
        core.render(poseStack, buffer, packedLight, packedOverlay, color);
        mask.render(poseStack, buffer, packedLight, packedOverlay, color);
        leftHand.render(poseStack, buffer, packedLight, packedOverlay, color);
        rightHand.render(poseStack, buffer, packedLight, packedOverlay, color);
        for (ModelPart plate : plates) {
            plate.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        tail.render(poseStack, buffer, packedLight, packedOverlay, color);
        shell.render(poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
