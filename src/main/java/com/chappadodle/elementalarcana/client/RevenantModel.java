package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.crypt.RevenantEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * A crypt's Revenant (see the Arcane Crypts spec): a skeleton's thin bones (LAYER), and over them
 * a robe and hood a little bigger than a person's (ROBE). Casting, it raises both arms high;
 * otherwise its arms hang a little forward, stiff.
 */
public class RevenantModel extends HumanoidModel<RevenantEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("revenant"), "main");
    public static final ModelLayerLocation ROBE = new ModelLayerLocation(ElementalArcana.id("revenant"), "robe");

    public RevenantModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return SkeletonModel.createBodyLayer();
    }

    public static LayerDefinition createRobeLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.3f), 0f), 64, 32);
    }

    @Override
    public void setupAnim(RevenantEntity revenant, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(revenant, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        if (revenant.isCasting()) {
            float sway = Mth.cos(ageInTicks * 0.6662f) * 0.25f;
            rightArm.xRot = sway - (float) Math.PI * 0.95f;
            leftArm.xRot = -sway - (float) Math.PI * 0.95f;
            rightArm.zRot = 0.35f;
            leftArm.zRot = -0.35f;
            rightArm.yRot = 0f;
            leftArm.yRot = 0f;
        } else {
            rightArm.xRot += -0.35f;
            leftArm.xRot += -0.35f;
        }
    }
}
