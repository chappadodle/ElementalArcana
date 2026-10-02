package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
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
 * A wisp, Minecraft style: a 4-pixel core spinning inside a 7-pixel shell that turns the other way,
 * with four 2-pixel motes circling it, the whole thing bobbing gently. Translucent (the shell is
 * see-through in the texture, tools/gen_creatures.py).
 */
public class WispModel extends EntityModel<WispEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ElementalArcana.id("wisp"), "main");
    private static final int MOTES = 4;
    private static final float MIDDLE_Y = 19f;

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart shell;
    private final ModelPart[] motes = new ModelPart[MOTES];

    public WispModel(ModelPart model) {
        super(RenderType::entityTranslucent);
        root = model.getChild("root");
        core = root.getChild("core");
        shell = root.getChild("shell");
        for (int i = 0; i < MOTES; i++) {
            motes[i] = root.getChild("mote" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0, MIDDLE_Y, 0));
        root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-2, -2, -2, 4, 4, 4), PartPose.ZERO);
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 8).addBox(-3.5f, -3.5f, -3.5f, 7, 7, 7), PartPose.ZERO);
        for (int i = 0; i < MOTES; i++) {
            root.addOrReplaceChild("mote" + i, CubeListBuilder.create().texOffs(16, 0).addBox(-1, -1, -1, 2, 2, 2), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(WispEntity wisp, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.y = MIDDLE_Y + Mth.sin(ageInTicks * 0.1f) * 1.2f;
        core.yRot = -ageInTicks * 0.09f;
        core.xRot = ageInTicks * 0.05f;
        shell.yRot = ageInTicks * 0.04f;
        shell.zRot = ageInTicks * 0.025f;
        for (int i = 0; i < MOTES; i++) {
            float angle = ageInTicks * 0.14f + i * Mth.HALF_PI;
            motes[i].x = Mth.cos(angle) * 6.5f;
            motes[i].z = Mth.sin(angle) * 6.5f;
            motes[i].y = Mth.sin(angle * 2 + i) * 1.5f;
        }
    }

    // The core and motes first, then the shell, so the core shows through it.
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        poseStack.pushPose();
        root.translateAndRotate(poseStack);
        core.render(poseStack, buffer, packedLight, packedOverlay, color);
        for (ModelPart mote : motes) {
            mote.render(poseStack, buffer, packedLight, packedOverlay, color);
        }
        shell.render(poseStack, buffer, packedLight, packedOverlay, color);
        poseStack.popPose();
    }
}
