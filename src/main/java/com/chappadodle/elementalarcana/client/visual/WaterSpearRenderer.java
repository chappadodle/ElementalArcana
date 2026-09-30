package com.chappadodle.elementalarcana.client.visual;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * The Tsunami Lance, Minecraft style: a spear built of little water cubes (the vanilla water
 * texture, deep ocean blue) pointing along its flight, tapering to a tip, with a glowing core
 * running through it and small cubes spiralling round the shaft. It grows with how long the stream
 * was held (the projectile's visual scale).
 */
public final class WaterSpearRenderer {
    private static final int TINT = 0x1A3A90;
    private static final int CORE = 0x5FA8FF;
    private static final int SHAFT = 7;

    private WaterSpearRenderer() {
    }

    public static void render(SpellProjectile lance, Vec3 direction, float scale, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        float time = lance.tickCount + partialTick;
        float yaw = (float) Mth.atan2(direction.x, direction.z);
        float pitch = (float) -Mth.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        poseStack.scale(scale, scale, scale);
        PoseStack.Pose pose = poseStack.last();
        TextureAtlasSprite water = WaterCubes.water();
        VertexConsumer body = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        VertexConsumer glow = buffers.getBuffer(RenderType.eyes(TextureAtlas.LOCATION_BLOCKS));
        Quaternionf spin = new Quaternionf().rotateZ(time * 0.3f);

        // The shaft, from the back forward, the cubes shrinking toward the tip.
        for (int i = 0; i < SHAFT; i++) {
            float z = -1.1f + i * 0.28f;
            float half = 0.16f * (1f - 0.1f * i);
            WaterCubes.cube(body, pose, water, 0f, 0f, z, spin, half, WaterCubes.hash(i, 1) * 0.5f, WaterCubes.hash(i, 2) * 0.5f,
                    TINT, 235, LightTexture.FULL_BRIGHT);
            WaterCubes.cube(glow, pose, water, 0f, 0f, z, spin, half * 0.5f, 0f, 0f, CORE, 255, LightTexture.FULL_BRIGHT);
        }
        // The tip: two small cubes drawn out to a point.
        WaterCubes.cube(body, pose, water, 0f, 0f, 0.95f, spin, 0.08f, 0.25f, 0.25f, TINT, 235, LightTexture.FULL_BRIGHT);
        WaterCubes.cube(body, pose, water, 0f, 0f, 1.12f, spin, 0.045f, 0.1f, 0.1f, 0x9FD8FF, 235, LightTexture.FULL_BRIGHT);
        // Small cubes spiralling round the shaft.
        for (int i = 0; i < 8; i++) {
            float along = -1.0f + ((i * 0.25f + time * 0.08f) % 2.0f);
            float angle = time * 0.5f + i * Mth.TWO_PI / 4;
            WaterCubes.cube(body, pose, water, Mth.cos(angle) * 0.32f, Mth.sin(angle) * 0.32f, along, spin, 0.06f,
                    WaterCubes.hash(i, 3) * 0.5f, WaterCubes.hash(i, 4) * 0.5f, 0x2E5CC8, 220, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }
}
