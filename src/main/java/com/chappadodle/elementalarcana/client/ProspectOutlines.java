package com.chappadodle.elementalarcana.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;

/**
 * What a Prospect showed (see the Cantrips spec): each ore found outlined in its colour, seen
 * through the stone, fading in and, at the end, out.
 */
public final class ProspectOutlines {
    private record Ore(BlockPos pos, int colour) {
    }

    /** Lines drawn over everything (no depth test), so the ores show through the rock. */
    private static final RenderType THROUGH = RenderType.create("elementalarcana_prospect", DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES, 4096, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
                    .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.of(2.0)))
                    .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .createCompositeState(false));
    private static final double FADE_IN = 10;
    private static final double FADE_OUT = 30;

    private static List<Ore> ores = List.of();
    private static long since;
    private static long until;

    private ProspectOutlines() {
    }

    public static void show(List<BlockPos> positions, List<Integer> colours, int ticks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<Ore> shown = new ArrayList<>(positions.size());
        for (int i = 0; i < positions.size() && i < colours.size(); i++) {
            shown.add(new Ore(positions.get(i), colours.get(i)));
        }
        ores = shown;
        since = level.getGameTime();
        until = since + ticks;
    }

    static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (ores.isEmpty() || level == null) {
            return;
        }
        double now = level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false);
        if (now >= until || now < since) {
            ores = List.of();
            return;
        }
        float alpha = (float) Math.min(1, Math.min((now - since) / FADE_IN, (until - now) / FADE_OUT)) * 0.9f;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(THROUGH);
        for (Ore ore : ores) {
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(ore.pos()).deflate(0.01), (ore.colour() >> 16 & 0xFF) / 255f,
                    (ore.colour() >> 8 & 0xFF) / 255f, (ore.colour() & 0xFF) / 255f, alpha);
        }
        // The render type's "no depth test" only holds if the test is already off, and other renderers
        // can leave it on: pass every fragment for these lines, then put vanilla's default back.
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        buffers.endBatch(THROUGH);
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        poseStack.popPose();
    }
}
