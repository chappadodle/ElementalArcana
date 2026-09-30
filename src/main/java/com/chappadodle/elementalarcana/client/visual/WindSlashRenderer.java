package com.chappadodle.elementalarcana.client.visual;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.Bloom;
import com.chappadodle.elementalarcana.content.spell.WindBladeSpell;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * The Wind Blade: a slash of moving air rather than a solid object. A crescent ribbon lying flat,
 * its arch leading the way (the curved cutting edge forward, the tips trailing back); a bright edge
 * fading to clear behind, with wind streaks flowing along it. It never spins (a fanned set holds each
 * blade at a fixed angle), except the Boomerang, which turns as it flies. Drawn additively from both
 * sides; bright looks and the Storm Scythe also bloom. See
 * docs/superpowers/specs/2026-09-30-wind-blade-vfx-design.md.
 */
public final class WindSlashRenderer {
    private static final int SEGMENTS = 20;
    /** Half the arc's sweep, in radians (a 140 degree crescent). */
    private static final float HALF_ARC = Mth.DEG_TO_RAD * 70f;
    private static final float RADIUS = 0.6f;
    private static final float WIDTH = 0.26f;

    /** How a look is drawn: its texture, edge and body colours, brightness, and ribbon width. */
    private record Style(String texture, int edge, int body, float intensity, float width) {
    }

    // Indexed by look, in WindBladeSpell's order.
    private static final List<Style> STYLES = List.of(
            new Style("soft", 0xE8FFF4, 0x9FD8C0, 0.55f, 1.0f),
            new Style("soft", 0xF0FFF8, 0x6FE0B0, 0.7f, 1.0f),
            new Style("sharp", 0xFFFFFF, 0x3FD69A, 0.8f, 0.9f),
            new Style("sharp", 0xFFFFFF, 0x5FFFC8, 0.95f, 0.9f),
            new Style("sharp", 0xF4FFF0, 0x7FE8A8, 0.8f, 0.8f),
            new Style("storm", 0xD8F4F0, 0x3A8A90, 0.8f, 1.1f),
            new Style("lines", 0xFFFFF0, 0xC8F0D8, 0.9f, 0.45f),
            new Style("storm", 0xE0F0FF, 0x506A90, 1.0f, 1.2f));

    private static final Function<ResourceLocation, RenderType> SLASH = Util.memoize(texture -> RenderType.create(
            "elementalarcana_wind_slash", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)));

    private WindSlashRenderer() {
    }

    private static ResourceLocation texture(Style style) {
        return ElementalArcana.id("textures/misc/wind_slash_" + style.texture() + ".png");
    }

    public static void render(SpellProjectile blade, Vec3 direction, float scale, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        int look = WindBladeSpell.look(blade.variant());
        boolean bright = (blade.variant() & WindBladeSpell.BRIGHT) != 0;
        Style style = STYLES.get(look);
        float time = blade.tickCount + partialTick;
        float yaw = (float) Mth.atan2(direction.x, direction.z);
        float pitch = (float) -Mth.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(yaw));
        poseStack.mulPose(Axis.XP.rotation(pitch));
        poseStack.mulPose(Axis.ZP.rotation(WindBladeSpell.fanAngle(blade.formationSlot(), blade.formationCount())));
        if (look == WindBladeSpell.LOOK_BOOMERANG && !blade.isHeld()) {
            // The Boomerang turns as it flies.
            poseStack.mulPose(Axis.ZP.rotation(time * 0.6f));
        }
        poseStack.scale(scale, scale, scale);
        PoseStack.Pose pose = poseStack.last();

        ResourceLocation texture = texture(style);
        VertexConsumer slash = buffers.getBuffer(SLASH.apply(texture));
        @Nullable RenderType bloomType = bright || look == WindBladeSpell.LOOK_SCYTHE ? Bloom.glowType(texture) : null;
        float intensity = style.intensity() * (bright ? 1.2f : 1f);
        float flow = time * 0.06f;

        if (look == WindBladeSpell.LOOK_THOUSAND_CUTS) {
            // Three razor-thin slashes, one behind another.
            for (int i = 0; i < 3; i++) {
                ribbon(slash, pose, style, RADIUS * (1f - 0.1f * i), WIDTH * style.width(), -0.22f * i, intensity * (1f - 0.2f * i), flow + i * 0.3f, time);
            }
        } else {
            ribbon(slash, pose, style, RADIUS, WIDTH * style.width(), 0f, intensity, flow, time);
            // A wider, fainter copy: the air around the edge.
            ribbon(slash, pose, style, RADIUS * 1.08f, WIDTH * style.width() * 1.8f, -0.03f, intensity * 0.3f, flow * 0.7f, time);
            if (look == WindBladeSpell.LOOK_BOOMERANG) {
                // Its twin crescent, back to back.
                poseStack.pushPose();
                poseStack.mulPose(Axis.ZP.rotation(Mth.PI));
                ribbon(slash, poseStack.last(), style, RADIUS, WIDTH * style.width(), 0f, intensity, flow, time);
                poseStack.popPose();
            }
        }
        if (bloomType != null) {
            ribbon(buffers.getBuffer(bloomType), pose, style, RADIUS, WIDTH * style.width(), 0f, intensity * 0.8f, flow, time);
        }
        if (look == WindBladeSpell.LOOK_SCYTHE) {
            lightning(slash, pose, blade.tickCount);
            if (bloomType != null) {
                lightning(buffers.getBuffer(bloomType), pose, blade.tickCount);
            }
        }
        poseStack.popPose();
    }

    /**
     * One crescent: an arc lying flat with its convex side forward (the way it flies), tapering to
     * points at both tips. The texture runs along the arc (scrolling with {@code flow}) and across it
     * (edge on the outside); it ripples a little over {@code time}. {@code back} moves it backward.
     */
    private static void ribbon(VertexConsumer consumer, PoseStack.Pose pose, Style style, float radius, float width,
                               float back, float intensity, float flow, float time) {
        // Centred so the middle of the edge is just ahead of the blade's position.
        float centreZ = -radius * 0.55f;
        for (int i = 0; i < SEGMENTS; i++) {
            float t0 = i / (float) SEGMENTS;
            float t1 = (i + 1) / (float) SEGMENTS;
            corner(consumer, pose, style, radius, width, back, intensity, flow, time, centreZ, t0, true);
            corner(consumer, pose, style, radius, width, back, intensity, flow, time, centreZ, t1, true);
            corner(consumer, pose, style, radius, width, back, intensity, flow, time, centreZ, t1, false);
            corner(consumer, pose, style, radius, width, back, intensity, flow, time, centreZ, t0, false);
        }
    }

    private static void corner(VertexConsumer consumer, PoseStack.Pose pose, Style style, float radius, float width,
                               float back, float intensity, float flow, float time, float centreZ, float t, boolean outer) {
        float angle = Mth.lerp(t, -HALF_ARC, HALF_ARC);
        float along = angle / HALF_ARC;
        // Full width in the middle, tapering to a point at each tip.
        float taper = (float) Math.pow(Math.max(0f, 1f - along * along), 0.7);
        float r = outer ? radius : radius - width * taper;
        float ripple = 0.02f * Mth.sin(time * 0.5f + t * 9f);
        float x = Mth.sin(angle) * r;
        float y = ripple;
        float z = centreZ + Mth.cos(angle) * r + back;
        int color = outer ? style.edge() : style.body();
        // Additive: fade by darkening, strongest in the middle of the arc.
        float strength = intensity * (0.35f + 0.65f * taper);
        consumer.addVertex(pose, x, y, z)
                .setColor(Math.round((color >> 16 & 0xFF) * strength), Math.round((color >> 8 & 0xFF) * strength),
                        Math.round((color & 0xFF) * strength), 255)
                .setUv(t * 2f + flow, outer ? 0f : 1f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0f, 1f, 0f);
    }

    /** Storm Scythe: a jagged line of lightning crackling along its edge, redrawn every tick. */
    private static void lightning(VertexConsumer consumer, PoseStack.Pose pose, int tick) {
        RandomSource random = RandomSource.create(tick * 31L);
        float centreZ = -RADIUS * 0.55f;
        int points = 12;
        float[] xs = new float[points + 1];
        float[] zs = new float[points + 1];
        for (int i = 0; i <= points; i++) {
            float angle = Mth.lerp(i / (float) points, -HALF_ARC * 0.9f, HALF_ARC * 0.9f);
            float r = RADIUS + (random.nextFloat() - 0.5f) * 0.12f;
            xs[i] = Mth.sin(angle) * r;
            zs[i] = centreZ + Mth.cos(angle) * r;
        }
        float thick = 0.018f;
        for (int i = 0; i < points; i++) {
            float dx = xs[i + 1] - xs[i];
            float dz = zs[i + 1] - zs[i];
            float length = Mth.sqrt(dx * dx + dz * dz);
            float nx = -dz / length * thick;
            float nz = dx / length * thick;
            bolt(consumer, pose, xs[i] + nx, zs[i] + nz);
            bolt(consumer, pose, xs[i + 1] + nx, zs[i + 1] + nz);
            bolt(consumer, pose, xs[i + 1] - nx, zs[i + 1] - nz);
            bolt(consumer, pose, xs[i] - nx, zs[i] - nz);
        }
    }

    private static void bolt(VertexConsumer consumer, PoseStack.Pose pose, float x, float z) {
        // The edge row of the texture is solid, so the bolt samples it for a bright, even line.
        consumer.addVertex(pose, x, 0.01f, z)
                .setColor(210, 230, 255, 255)
                .setUv(0.5f, 0f)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0f, 1f, 0f);
    }
}
