package com.chappadodle.elementalarcana.client.visual;

import com.chappadodle.elementalarcana.client.Bloom;
import com.chappadodle.elementalarcana.content.HydroStreamOptions;
import com.chappadodle.elementalarcana.content.spell.HydroJetSpell;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hydro Jet streams, Minecraft style: a rush of little water blocks from each caster's hand to
 * where its stream lands. Each is a small tumbling cube wearing the game's own animated water
 * texture, tinted per look the way biomes tint water. They emerge small at the palm, grow, and
 * spread slightly as they go, and each keeps its own size and tumble as it travels, so the flow
 * reads as moving water. The brightest looks glow from inside (and bloom). It's only the look: the
 * stream still hits along its whole line at once (see HydroJetSpell). Each stream packet
 * (HydroStreamEmitter) refreshes its caster's stream; one not refreshed for a few ticks is gone.
 * Your own stream is re-aimed from your hand every frame, so it never lags your crosshair. See
 * docs/superpowers/specs/2026-09-30-hydro-jet-vfx-design.md.
 */
public final class WaterBeams {
    private static final int EXPIRE_TICKS = 3;
    private static final int MAX_CUBES = 60;

    /**
     * How a look is drawn: its water tint, cube size (half its edge, in blocks), how fast the water
     * rushes, how strongly it glows from inside (0 = not at all) and in what colour, whether the cubes
     * spiral around the line, and how far they spread out by the end of it.
     */
    private record Style(int tint, float size, float flow, float glow, int glowColor, boolean twist, float spread) {
    }

    // Indexed by look, in HydroJetSpell's order.
    private static final List<Style> STYLES = List.of(
            new Style(0x5FA8F0, 0.11f, 0.25f, 0f, 0xBFE8FF, false, 0.15f),
            new Style(0x3F76E4, 0.12f, 0.3f, 0f, 0xBFE8FF, false, 0.15f),
            new Style(0x2E5CC8, 0.13f, 0.38f, 0.3f, 0xBFE8FF, false, 0.12f),
            new Style(0x1E40A8, 0.14f, 0.45f, 0.6f, 0x7FE8FF, false, 0.12f),
            new Style(0x9FD8FF, 0.05f, 0.8f, 0.4f, 0xFFFFFF, false, 0.02f),
            new Style(0xB8D4FF, 0.22f, 0.35f, 0f, 0xFFFFFF, false, 0.6f),
            new Style(0x2AA89A, 0.13f, 0.4f, 0.3f, 0xB0FFF0, true, 0.1f),
            new Style(0x1A3A90, 0.15f, 0.45f, 0.4f, 0x9FD0FF, false, 0.12f));

    /** A caster's stream as last reported: where it starts, its line, look, pressure and when. */
    private static final class Beam {
        private final ClientLevel level;
        private Vec3 start;
        private Vec3 line;
        private int look;
        private float pressure;
        private long updated;

        private Beam(ClientLevel level) {
            this.level = level;
        }
    }

    private static final Map<Integer, Beam> BEAMS = new HashMap<>();

    private WaterBeams() {
    }

    /** A stream packet for {@code stream.caster()}: its stream now runs from {@code start} along {@code line}. */
    public static void update(ClientLevel level, HydroStreamOptions stream, Vec3 start, Vec3 line) {
        Beam beam = BEAMS.computeIfAbsent(stream.caster(), id -> new Beam(level));
        beam.start = start;
        beam.line = line;
        beam.look = stream.look();
        beam.pressure = stream.pressure();
        beam.updated = level.getGameTime();
    }

    /** Draws every live stream (after the see-through blocks, so water behind it still shows). */
    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (BEAMS.isEmpty()) {
            return;
        }
        if (level == null) {
            BEAMS.clear();
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        TextureAtlasSprite water = WaterCubes.water();
        RenderType glowType = RenderType.eyes(TextureAtlas.LOCATION_BLOCKS);
        @Nullable RenderType bloomType = Bloom.glowType(TextureAtlas.LOCATION_BLOCKS);
        LocalPlayer self = minecraft.player;

        for (Iterator<Map.Entry<Integer, Beam>> it = BEAMS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Beam> entry = it.next();
            Beam beam = entry.getValue();
            if (beam.level != level || level.getGameTime() - beam.updated > EXPIRE_TICKS) {
                it.remove();
                continue;
            }
            Vec3 start = beam.start;
            Vec3 line = beam.line;
            if (self != null && entry.getKey() == self.getId()) {
                // Your own jet: from your hand, where you're looking, this very frame.
                start = handPoint(self, partialTick);
                line = self.getViewVector(partialTick).scale(beam.line.length());
            }
            Style style = STYLES.get(HydroJetSpell.look(beam.look));
            boolean bright = (beam.look & HydroJetSpell.BRIGHT) != 0;
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(start));
            light = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));
            float size = style.size() * (1f + 0.35f * beam.pressure);
            // Pressure Build and Lv 8+ make the inside glow brighter.
            float glow = (style.glow() + 0.4f * beam.pressure) * (bright ? 1.4f : 1f);
            cubes(buffers.getBuffer(Sheets.translucentCullBlockSheet()),
                    glow > 0.05f ? buffers.getBuffer(glowType) : null,
                    glow > 0.05f && bright && bloomType != null ? buffers.getBuffer(bloomType) : null,
                    pose, water, start, line, size, style, glow, time, light);
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(glowType);
        if (bloomType != null) {
            buffers.endBatch(bloomType);
        }
        poseStack.popPose();
    }

    /** Where a player's stream leaves their hand this frame (HydroJetSpell#streamOrigin, smoothed). */
    private static Vec3 handPoint(LocalPlayer player, float partialTick) {
        float yaw = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        return player.getEyePosition(partialTick).add(player.getViewVector(partialTick).scale(0.5)).add(right.scale(0.3)).add(0, -0.2, 0);
    }

    /**
     * The stream's water cubes along {@code line}. They rush from the hand at a speed set by the
     * look's flow; each keeps its own size, tumble and patch of texture (from its number in the
     * stream) as it goes, emerges small at the palm, and drifts a little off the line the further it
     * gets (or spirals round it).
     */
    private static void cubes(VertexConsumer body, @Nullable VertexConsumer glow, @Nullable VertexConsumer bloom, PoseStack.Pose pose,
                              TextureAtlasSprite water, Vec3 start, Vec3 line, float size, Style style, float glowStrength,
                              float time, int light) {
        double length = line.length();
        if (length < 0.05) {
            return;
        }
        Vec3 dir = line.scale(1 / length);
        Vec3 reference = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(reference).normalize();
        Vec3 up = right.cross(dir).normalize();
        double spacing = Math.max(0.12, size * 2.6);
        double speed = 0.6 + style.flow() * 2.5;
        double travelled = time * speed;
        double offset = travelled % spacing;
        long first = (long) Math.floor(travelled / spacing);
        int count = Math.min(MAX_CUBES, (int) Math.ceil(length / spacing));
        int glowColor = scale(style.glowColor(), glowStrength);
        for (int k = 0; k <= count; k++) {
            double d = k * spacing + offset;
            if (d > length) {
                break;
            }
            // Its number in the stream: the same cube keeps it as it travels.
            long id = first - k;
            float half = size * (0.7f + 0.6f * WaterCubes.hash(id, 1)) * Math.min(1f, 0.35f + (float) d * 0.65f);
            double far = d / length;
            Vec3 drift;
            if (style.twist()) {
                // Maelstrom: straight out of the hand, then winding into a spiral from about
                // 1.5 blocks out, at full width by 4.
                double wind = Mth.clamp((d - 1.5) / 2.5, 0, 1);
                double swirl = 0.3 * wind * wind * (3 - 2 * wind);
                double angle = d * 2.0 + time * 0.4;
                drift = right.scale(Math.cos(angle) * swirl).add(up.scale(Math.sin(angle) * swirl));
            } else {
                drift = right.scale((WaterCubes.hash(id, 2) - 0.5) * 2 * style.spread() * far).add(up.scale((WaterCubes.hash(id, 3) - 0.5) * 2 * style.spread() * far));
            }
            Vec3 centre = start.add(dir.scale(d)).add(drift);
            // A slow tumble about its own axis.
            Quaternionf tumble = new Quaternionf().rotateAxis(time * 0.12f + WaterCubes.hash(id, 4) * Mth.TWO_PI,
                    new Vector3f(WaterCubes.hash(id, 5) - 0.5f, WaterCubes.hash(id, 6) - 0.5f, WaterCubes.hash(id, 7) - 0.5f).normalize());
            // A random patch of the water texture, half its width, so cubes don't all match.
            float uo = WaterCubes.hash(id, 8) * 0.5f;
            float vo = WaterCubes.hash(id, 9) * 0.5f;
            float cx = (float) centre.x;
            float cy = (float) centre.y;
            float cz = (float) centre.z;
            WaterCubes.cube(body, pose, water, cx, cy, cz, tumble, half, uo, vo, style.tint(), 235, light);
            if (glow != null) {
                WaterCubes.cube(glow, pose, water, cx, cy, cz, tumble, half * 0.55f, uo, vo, glowColor, 255, LightTexture.FULL_BRIGHT);
            }
            if (bloom != null) {
                WaterCubes.cube(bloom, pose, water, cx, cy, cz, tumble, half * 0.55f, uo, vo, scale(glowColor, 0.7f), 255, LightTexture.FULL_BRIGHT);
            }
        }
    }

    /** A colour darkened (or, above 1, brightened and clamped) by {@code strength}, for additive layers. */
    private static int scale(int color, float strength) {
        int red = Math.min(255, Math.round((color >> 16 & 0xFF) * strength));
        int green = Math.min(255, Math.round((color >> 8 & 0xFF) * strength));
        int blue = Math.min(255, Math.round((color & 0xFF) * strength));
        return red << 16 | green << 8 | blue;
    }
}
