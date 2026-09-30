package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Bubble;
import com.chappadodle.elementalarcana.client.visual.WaterCubes;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.spell.BubblePrisonEffects;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Draws a Bubble Prison, Minecraft style: a hollow sphere of little see-through water cubes around
 * the trapped creature, sized to it, slowly turning, wobbling and shimmering, with a white shine on
 * its upper front and tiny pale cubes floating up inside. When it's cast the cubes rush in from all
 * around and snap into place; when it ends (popped, run out, or the creature died) it bursts (see
 * BubblePrisonEffects#pop). See docs/superpowers/specs/2026-09-30-bubble-prison-vfx-design.md.
 */
final class BubbleRenderer {
    // How much bigger than the creature the bubble is.
    private static final float MARGIN = 1.3f;
    /** The shell's cubes: a 10-cube-wide voxel sphere, as unit directions (radius 1). */
    private static final List<Vector3f> SHELL = new ArrayList<>();
    /** The shell's grid spacing, as a fraction of its radius. */
    private static final float CELL;
    private static final float FORM_TICKS = 6f;
    private static final int WATER = 0x4FA8FF;
    private static final int SHINE = 0xEAF6FF;
    private static final int RISERS = 5;

    static {
        // Grid points inside the sphere with a neighbour outside it: a watertight one-cube shell.
        int n = 10;
        float radius = n / 2f - 0.2f;
        for (int x = 0; x < n; x++) {
            for (int y = 0; y < n; y++) {
                for (int z = 0; z < n; z++) {
                    float cx = x - (n - 1) / 2f;
                    float cy = y - (n - 1) / 2f;
                    float cz = z - (n - 1) / 2f;
                    if (!inside(cx, cy, cz, radius)) {
                        continue;
                    }
                    if (!inside(cx + 1, cy, cz, radius) || !inside(cx - 1, cy, cz, radius) || !inside(cx, cy + 1, cz, radius)
                            || !inside(cx, cy - 1, cz, radius) || !inside(cx, cy, cz + 1, radius) || !inside(cx, cy, cz - 1, radius)) {
                        SHELL.add(new Vector3f(cx, cy, cz).div(radius));
                    }
                }
            }
        }
        CELL = 1f / radius;
    }

    private static boolean inside(float x, float y, float z, float radius) {
        return x * x + y * y + z * z <= radius * radius;
    }

    /** A bubble being drawn: where the creature's feet and the bubble's centre were last, and its radius. */
    private record Drawn(Vec3 feet, Vec3 center, float radius) {
    }

    private static final Map<Integer, Drawn> DRAWN = new HashMap<>();

    private BubbleRenderer() {
    }

    /** After a creature is drawn: its bubble, if it's trapped (the pose is at its feet). */
    static void render(LivingEntity entity, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        if (!BubblePrisons.isTrapped(entity)) {
            return;
        }
        Bubble bubble = entity.getData(MagicAttachments.BUBBLE);
        float time = entity.tickCount + partialTick;
        float radius = Math.max(entity.getBbWidth(), entity.getBbHeight()) * MARGIN / 2;
        float centerY = entity.getBbHeight() / 2;
        Vec3 feet = entity.getPosition(partialTick);
        DRAWN.put(entity.getId(), new Drawn(feet, feet.add(0, centerY, 0), radius));

        float age = entity.level().getGameTime() - bubble.startTick() + partialTick;
        float formed = Mth.clamp(age / FORM_TICKS, 0f, 1f);
        // Squash and stretch, out of step on each axis.
        float wobbleX = 1f + 0.05f * Mth.sin(time * 0.35f);
        float wobbleY = 1f + 0.05f * Mth.sin(time * 0.35f + 2.1f);
        float wobbleZ = 1f + 0.05f * Mth.sin(time * 0.35f + 4.2f);
        Quaternionf spin = new Quaternionf().rotationY(time * 0.03f);

        // The shine sits on the upper front as seen from the camera, whichever way the bubble turns.
        Vec3 toCamera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().subtract(feet.add(0, centerY, 0));
        Vector3f shineDir = new Vector3f((float) toCamera.x, 0, (float) toCamera.z);
        if (shineDir.lengthSquared() < 1.0e-4f) {
            shineDir.set(0, 0, 1);
        }
        shineDir.normalize().mul(0.7f).add(0.25f, 1f, 0f).normalize();

        TextureAtlasSprite water = WaterCubes.water();
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 10), LightTexture.sky(packedLight));
        float half = radius * CELL * 0.46f;

        for (int i = 0; i < SHELL.size(); i++) {
            // Each cube arrives a little after the last, so they rush in rather than zoom as one.
            float delay = WaterCubes.hash(i, 1) * 2f;
            float arrive = Mth.clamp((age - delay) / (FORM_TICKS - 2f), 0f, 1f);
            if (formed < 1f && arrive <= 0f) {
                continue;
            }
            float ease = 1f - (1f - arrive) * (1f - arrive) * (1f - arrive);
            float reach = Mth.lerp(ease, 3.5f, 1f);
            Vector3f at = spin.transform(new Vector3f(SHELL.get(i)));
            Vector3f unit = new Vector3f(at);
            at.mul(radius * reach * wobbleX, radius * reach * wobbleY, radius * reach * wobbleZ);
            // A shimmer running over the shell.
            float shimmer = 0.5f + 0.5f * Mth.sin(time * 0.25f + WaterCubes.hash(i, 2) * Mth.TWO_PI);
            boolean shine = unit.dot(shineDir) > 0.9f;
            int color = shine ? SHINE : WATER;
            int alpha = Math.round((shine ? 200 : 105 + 35 * shimmer) * (0.4f + 0.6f * ease));
            float size = half * (0.35f + 0.65f * ease) * (0.92f + 0.08f * shimmer);
            Quaternionf turn = new Quaternionf(spin).rotateX((1f - ease) * WaterCubes.hash(i, 3) * 4f);
            WaterCubes.cube(consumer, pose, water, at.x(), centerY + at.y(), at.z(), turn, size,
                    WaterCubes.hash(i, 4) * 0.5f, WaterCubes.hash(i, 5) * 0.5f, color, alpha, light);
        }

        // Tiny pale cubes floating up inside, once it's formed.
        if (formed >= 1f) {
            for (int k = 0; k < RISERS; k++) {
                float phase = time * 0.025f + k / (float) RISERS + WaterCubes.hash(entity.getId(), k);
                phase -= Mth.floor(phase);
                float angle = WaterCubes.hash(entity.getId(), k + 10) * Mth.TWO_PI + time * 0.04f;
                float out = radius * 0.45f * WaterCubes.hash(entity.getId(), k + 20);
                float y = radius * (phase * 1.4f - 0.7f);
                int alpha = Math.round(170 * Mth.sin(phase * Mth.PI));
                WaterCubes.cube(consumer, pose, water, Mth.cos(angle) * out, centerY + y, Mth.sin(angle) * out,
                        new Quaternionf().rotationXYZ(time * 0.1f + k, time * 0.13f, 0), half * 0.45f, 0.25f, 0.25f, SHINE, alpha, light);
            }
        }
    }

    /** Every client tick: bubbles whose creature is no longer trapped (or is gone) pop. */
    static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            DRAWN.clear();
            return;
        }
        for (Iterator<Map.Entry<Integer, Drawn>> it = DRAWN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Drawn> entry = it.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity != null && entity.isAlive() && BubblePrisons.isTrapped(entity)) {
                continue;
            }
            it.remove();
            Drawn drawn = entry.getValue();
            BubblePrisonEffects.pop(level, drawn.feet(), drawn.center(), drawn.radius());
        }
    }
}
