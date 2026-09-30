package com.chappadodle.elementalarcana.client.decal;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.client.Bloom;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Marks painted onto the world: burn scorches and glowing lava cracks left by fireball blasts, and
 * frost left by shattering ice, which melts inward from its edges.
 * <p>
 * A mark isn't one flat square: every frame it's projected onto the real, exposed faces of the
 * blocks around where it landed (on the ground, a wall or a ceiling), cut to each face. So it
 * follows steps and slabs, stops at a ledge instead of hanging over it, and when a block breaks,
 * its part of the mark goes with it. Vanilla's block-breaking cracks are drawn onto blocks the same
 * way.
 */
public final class Decals {
    public enum Kind { SCORCH, CRACKS, FROST }

    private static final ResourceLocation SCORCH_TEXTURE = ElementalArcana.id("textures/misc/scorch.png");
    private static final ResourceLocation CRACKS_TEXTURE = ElementalArcana.id("textures/misc/cracks.png");
    private static final ResourceLocation FROST_TEXTURE = ElementalArcana.id("textures/misc/frost.png");
    /** How far off the surface a mark sits, against flicker (on top of the polygon offset). */
    private static final float LIFT = 0.004f;
    /** How far above or below the mark's own surface other surfaces can still take it. */
    private static final double REACH = 1.5;

    /** A burn mark: blended normally and lit by the world, like the blocks under it. */
    private static final RenderType SCORCH = markType("elementalarcana_scorch", SCORCH_TEXTURE);
    /** Frost: pale crystals, blended normally and lit by the world. */
    private static final RenderType FROST = markType("elementalarcana_frost", FROST_TEXTURE);
    /** Glowing cracks: added on top, full bright. */
    private static final RenderType CRACKS = RenderType.create("elementalarcana_cracks", DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS, 1024, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(CRACKS_TEXTURE, false, false))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false));

    /** A mark blended normally over the blocks and lit by the world, like the blocks under it. */
    private static RenderType markType(String name, ResourceLocation texture) {
        return RenderType.create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1024, false, true,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setCullState(RenderStateShard.NO_CULL)
                        .setLightmapState(RenderStateShard.LIGHTMAP)
                        .setOverlayState(RenderStateShard.OVERLAY)
                        .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                        .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                        .createCompositeState(false));
    }

    /**
     * One mark: its centre on the surface it hit, which way that surface faces, its radius, when it
     * was made and for how long (ticks), its tint (0xRRGGBB), and whether its texture is mirrored
     * (so marks don't all look alike).
     */
    private record Decal(Kind kind, Vec3 center, Direction face, float radius, long born, int lifetime, int color,
                         boolean flipU, boolean flipV, ClientLevel level) {
    }

    private static final List<Decal> DECALS = new ArrayList<>();

    private Decals() {
    }

    /** Leaves a mark centred on {@code center}, on a surface facing {@code face}. */
    public static void add(Kind kind, Vec3 center, Direction face, float radius, int lifetime, int color) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        DECALS.add(new Decal(kind, center, face, radius, level.getGameTime(), lifetime, color,
                level.random.nextBoolean(), level.random.nextBoolean(), level));
    }

    /** Draws every live mark (after the cutout blocks, before water and other see-through blocks). */
    public static void render(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (DECALS.isEmpty() || level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double now = level.getGameTime() + partialTick;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        PoseStack.Pose pose = poseStack.last();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        @Nullable RenderType bloom = Bloom.glowType(CRACKS_TEXTURE);

        for (Iterator<Decal> it = DECALS.iterator(); it.hasNext(); ) {
            Decal decal = it.next();
            float life = (float) ((now - decal.born()) / decal.lifetime());
            if (decal.level() != level || life >= 1f) {
                it.remove();
                continue;
            }
            if (decal.kind() == Kind.SCORCH) {
                float alpha = life < 0.6f ? 0.9f : 0.9f * (1f - (life - 0.6f) / 0.4f);
                project(level, decal, decal.radius(), pose, buffers.getBuffer(SCORCH), alpha, false);
            } else if (decal.kind() == Kind.FROST) {
                // Frost melts inward from its edges over the second half of its life.
                float melt = life < 0.5f ? 1f : 1f - (life - 0.5f) / 0.5f * 0.85f;
                float alpha = life < 0.5f ? 0.85f : 0.85f * (1f - (life - 0.5f) / 0.5f * 0.6f);
                project(level, decal, decal.radius() * melt, pose, buffers.getBuffer(FROST), alpha, false);
            } else {
                // Cracks cool: they pulse, dim, and fade out.
                float heat = (1f - life) * (0.75f + 0.25f * Mth.sin((float) now * 0.4f));
                project(level, decal, decal.radius(), pose, buffers.getBuffer(CRACKS), heat, true);
                if (bloom != null) {
                    project(level, decal, decal.radius(), pose, buffers.getBuffer(bloom), heat, true);
                }
            }
        }
        buffers.endBatch(SCORCH);
        buffers.endBatch(FROST);
        buffers.endBatch(CRACKS);
        if (bloom != null) {
            buffers.endBatch(bloom);
        }
        poseStack.popPose();
    }

    /**
     * Paints one mark onto the exposed faces around it. For every block column across the mark, it
     * takes the exposed face (facing the mark's way) nearest the mark's own surface, and draws the
     * part of the mark that lies over that face.
     */
    private static void project(ClientLevel level, Decal decal, double r, PoseStack.Pose pose, VertexConsumer consumer, float strength, boolean glowing) {
        Direction face = decal.face();
        Direction.Axis normalAxis = face.getAxis();
        Direction.Axis uAxis = normalAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis vAxis = normalAxis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        double cu = decal.center().get(uAxis);
        double cv = decal.center().get(vAxis);
        double plane = decal.center().get(normalAxis);
        int sign = face.getAxisDirection().getStep();
        // The block the mark's surface belongs to (just behind the surface).
        BlockPos origin = BlockPos.containing(decal.center().subtract(Vec3.atLowerCornerOf(face.getNormal()).scale(0.01)));
        int span = Mth.ceil(r);
        int red = decal.color() >> 16 & 0xFF;
        int green = decal.color() >> 8 & 0xFF;
        int blue = decal.color() & 0xFF;

        for (int du = -span; du <= span; du++) {
            for (int dv = -span; dv <= span; dv++) {
                BlockPos column = origin.relative(uAxis, du).relative(vAxis, dv);
                // Skip columns that don't reach into the mark's circle at all.
                double nearestU = Mth.clamp(cu, column.get(uAxis), column.get(uAxis) + 1);
                double nearestV = Mth.clamp(cv, column.get(vAxis), column.get(vAxis) + 1);
                if ((nearestU - cu) * (nearestU - cu) + (nearestV - cv) * (nearestV - cv) > r * r) {
                    continue;
                }
                Surface surface = nearestSurface(level, column, face, normalAxis, plane);
                if (surface == null) {
                    continue;
                }
                double u0 = Math.max(surface.box().min(uAxis), cu - r);
                double u1 = Math.min(surface.box().max(uAxis), cu + r);
                double v0 = Math.max(surface.box().min(vAxis), cv - r);
                double v1 = Math.min(surface.box().max(vAxis), cv + r);
                if (u0 >= u1 || v0 >= v1) {
                    continue;
                }
                // Fade out marks on surfaces further from where it hit.
                float offset = (float) Math.abs(surface.level() - plane);
                int alpha = Math.round(255 * strength * (1f - 0.5f * offset / (float) REACH));
                int light = glowing ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, surface.pos().relative(face));
                double lifted = surface.level() + sign * LIFT;
                float[][] corners = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
                for (float[] corner : corners) {
                    double u = corner[0] == 0 ? u0 : u1;
                    double v = corner[1] == 0 ? v0 : v1;
                    float texU = (float) ((u - (cu - r)) / (2 * r));
                    float texV = (float) ((v - (cv - r)) / (2 * r));
                    Vec3 point = point(normalAxis, uAxis, vAxis, lifted, u, v);
                    consumer.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                            .setColor(red, green, blue, Mth.clamp(alpha, 0, 255))
                            .setUv(decal.flipU() ? 1 - texU : texU, decal.flipV() ? 1 - texV : texV)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(light)
                            .setNormal(pose, face.getStepX(), face.getStepY(), face.getStepZ());
                }
            }
        }
    }

    /** An exposed face in a block column: the block, where the face lies along the normal, and its extent. */
    private record Surface(BlockPos pos, double level, AABB box) {
    }

    /** The exposed face in this column (facing {@code face}) closest to the mark's surface, if any is close enough. */
    @Nullable
    private static Surface nearestSurface(ClientLevel level, BlockPos column, Direction face, Direction.Axis axis, double plane) {
        Surface best = null;
        double bestDistance = REACH;
        for (int step = -2; step <= 1; step++) {
            BlockPos pos = column.relative(face, step);
            BlockState state = level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty()) {
                continue;
            }
            BlockPos next = pos.relative(face);
            if (level.getBlockState(next).isFaceSturdy(level, next, face.getOpposite())) {
                continue;
            }
            AABB box = shape.bounds().move(pos);
            double surfaceLevel = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
            double distance = Math.abs(surfaceLevel - plane);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = new Surface(pos, surfaceLevel, box);
            }
        }
        return best;
    }

    private static Vec3 point(Direction.Axis normalAxis, Direction.Axis uAxis, Direction.Axis vAxis, double n, double u, double v) {
        double[] xyz = new double[3];
        xyz[normalAxis.ordinal()] = n;
        xyz[uAxis.ordinal()] = u;
        xyz[vAxis.ordinal()] = v;
        return new Vec3(xyz[0], xyz[1], xyz[2]);
    }
}
