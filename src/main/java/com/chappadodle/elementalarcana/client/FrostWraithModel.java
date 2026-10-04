package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.FrostWraithEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * A Frost Wraith (64x64 texture, tools/gen_wild.py): a hooded head with a dark face, a narrow body,
 * a robe widening below the waist and trailing off into ragged tatters, and two long sleeves with
 * no hands. Drawn translucent. It bobs as it floats, its tatters and sleeves stream behind it, and
 * it reaches out with both sleeves when it attacks.
 */
public class FrostWraithModel extends EntityModel<FrostWraithEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("frost_wraith"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart robe;
    private final ModelPart tatters;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private float attack;

    public FrostWraithModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root.getChild("body");
        head = this.root.getChild("head");
        robe = this.root.getChild("robe");
        tatters = robe.getChild("tatters");
        rightArm = this.root.getChild("right_arm");
        leftArm = this.root.getChild("left_arm");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // The body: shoulders at y 0, the waist at y 10 (the whole wraith is 30 px tall, its hood's top at -6).
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-4, 0, -2, 8, 10, 4),
                PartPose.offset(0, -4, 0));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8)
                        .texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8, new CubeDeformation(0.6f)),
                PartPose.offset(0, 0, 0));
        PartDefinition robe = body.addOrReplaceChild("robe", CubeListBuilder.create().texOffs(24, 16).addBox(-5, 0, -3, 10, 8, 6),
                PartPose.offset(0, 9, 0));
        robe.addOrReplaceChild("tatters", CubeListBuilder.create().texOffs(0, 32).addBox(-5, 0, -3, 10, 10, 6),
                PartPose.offset(0, 8, 0));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 32).addBox(-3, -1, -1.5f, 3, 12, 3),
                PartPose.offset(-4, 1, 0));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 32).mirror().addBox(0, -1, -1.5f, 3, 12, 3),
                PartPose.offset(4, 1, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void prepareMobModel(FrostWraithEntity wraith, float limbSwing, float limbSwingAmount, float partialTick) {
        attack = wraith.getAttackAnim(partialTick);
    }

    @Override
    public void setupAnim(FrostWraithEntity wraith, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float bob = Mth.sin(ageInTicks * 0.1f) * 1.2f;
        root.y = -4 + bob;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        // Leaning into its flight; the robe and tatters trail behind.
        float speed = Math.min(1f, limbSwingAmount * 2f);
        root.xRot = 0.15f + speed * 0.25f;
        robe.xRot = 0.1f + speed * 0.3f + Mth.sin(ageInTicks * 0.13f) * 0.05f;
        tatters.xRot = 0.15f + speed * 0.35f + Mth.sin(ageInTicks * 0.17f + 1f) * 0.1f;
        tatters.zRot = Mth.sin(ageInTicks * 0.09f) * 0.06f;
        // Sleeves hang a little forward and drift; they reach out when it strikes or casts.
        float drift = Mth.sin(ageInTicks * 0.08f) * 0.08f;
        float reach = -0.35f - speed * 0.25f;
        if (attack > 0 || wraith.isAggressive()) {
            reach = -1.35f - Mth.sin(attack * Mth.PI) * 0.4f;
        }
        rightArm.xRot = reach + drift;
        leftArm.xRot = reach - drift;
        rightArm.zRot = 0.1f + Mth.cos(ageInTicks * 0.07f) * 0.04f;
        leftArm.zRot = -0.1f - Mth.cos(ageInTicks * 0.07f) * 0.04f;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
