package com.chappadodle.elementalarcana.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Starfall on the client (see its spec): a falling star's streak, a bright head with a fading tail
 * drawn across the sky (wide enough to see from hundreds of blocks off), and over each fallen star a
 * pale gold pillar of light like a beacon's, until it's mined or dawn comes.
 */
public final class StarfallClient {
    private record Streak(Vec3 from, Vec3 to, long start, int ticks) {
    }

    private static final List<Streak> STREAKS = new ArrayList<>();
    private static List<BlockPos> pillars = List.of();
    private static final int PILLAR_COLOUR = 0xFFF2D27A;
    /** How much of the path the tail covers behind the head. */
    private static final double TAIL = 0.3;

    private StarfallClient() {
    }

    public static void streak(Vector3f from, Vector3f to, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            STREAKS.add(new Streak(new Vec3(from), new Vec3(to), level.getGameTime(), ticks));
        }
    }

    public static void pillars(List<BlockPos> stars) {
        pillars = List.copyOf(stars);
    }

    static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || STREAKS.isEmpty() && pillars.isEmpty()) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        for (BlockPos pos : pillars) {
            poseStack.pushPose();
            poseStack.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            BeaconRenderer.renderBeaconBeam(poseStack, buffers, BeaconRenderer.BEAM_LOCATION, partialTick, 1f, level.getGameTime(),
                    0, 320, PILLAR_COLOUR, 0.35f, 0.6f);
            poseStack.popPose();
        }
        if (!STREAKS.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(-camera.x, -camera.y, -camera.z);
            VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
            for (Iterator<Streak> it = STREAKS.iterator(); it.hasNext(); ) {
                Streak streak = it.next();
                double t = (now - streak.start()) / streak.ticks();
                if (t > 1 + TAIL) {
                    it.remove();
                    continue;
                }
                Vec3 head = streak.from().lerp(streak.to(), Math.min(1, t));
                Vec3 tail = streak.from().lerp(streak.to(), Math.max(0, t - TAIL));
                drawStreak(consumer, poseStack.last(), camera, tail, head, t > 1 ? (float) (1 - (t - 1) / TAIL) : 1f);
                if (t < 1 && head.distanceToSqr(camera) < 96 * 96) {
                    level.addParticle(ParticleTypes.END_ROD, head.x, head.y, head.z, 0, 0, 0);
                }
            }
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    /** A quad from tail to head, turned to the camera: wide and white-gold at the head, thin and clear at the tail. */
    private static void drawStreak(VertexConsumer consumer, PoseStack.Pose pose, Vec3 camera, Vec3 tail, Vec3 head, float fade) {
        Vec3 along = head.subtract(tail);
        if (along.lengthSqr() < 1.0e-4) {
            return;
        }
        Vec3 toCamera = camera.subtract(head);
        Vec3 side = along.cross(toCamera);
        if (side.lengthSqr() < 1.0e-6) {
            return;
        }
        double width = 0.6 + head.distanceTo(camera) * 0.006;
        side = side.normalize().scale(width);
        Vec3 tailSide = side.scale(0.15);
        int alpha = Math.round(230 * fade);
        // Both windings: the render type culls back faces, and which side faces the camera varies.
        vertex(consumer, pose, tail.add(tailSide), 255, 210, 140, 0);
        vertex(consumer, pose, tail.subtract(tailSide), 255, 210, 140, 0);
        vertex(consumer, pose, head.subtract(side), 255, 248, 220, alpha);
        vertex(consumer, pose, head.add(side), 255, 248, 220, alpha);
        vertex(consumer, pose, head.add(side), 255, 248, 220, alpha);
        vertex(consumer, pose, head.subtract(side), 255, 248, 220, alpha);
        vertex(consumer, pose, tail.subtract(tailSide), 255, 210, 140, 0);
        vertex(consumer, pose, tail.add(tailSide), 255, 210, 140, 0);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 at, int red, int green, int blue, int alpha) {
        consumer.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(red, green, blue, alpha);
    }
}
