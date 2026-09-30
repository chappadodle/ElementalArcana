package com.chappadodle.elementalarcana.client.visual;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Little cubes of Minecraft water, the Hydro Jet's building block: a cube wearing the game's own
 * animated water texture (a patch of it), tinted like biome water. Used by the stream
 * (WaterBeams), its splashes, the whirlpool and the Tsunami Lance spear. Works for both entity-style
 * buffers (with a pose) and particle buffers (camera-relative, no pose).
 */
public final class WaterCubes {
    private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("block/water_still");

    /** A unit cube's corners, and its faces as corner indices wound to face outward. */
    private static final float[][] CORNERS = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    private static final int[][] FACES = {
            {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {3, 7, 6, 2}, {0, 1, 5, 4}};

    private WaterCubes() {
    }

    /** The vanilla water texture in the block atlas. */
    public static TextureAtlasSprite water() {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WATER);
    }

    /**
     * One cube of water centred on {@code (x, y, z)}: {@code half} its edge, turned by
     * {@code rotation}, each face showing the half-width patch of texture at {@code (uo, vo)}.
     * With a pose, the position is in that pose's space; without one, it's camera-relative.
     */
    public static void cube(VertexConsumer consumer, @Nullable PoseStack.Pose pose, TextureAtlasSprite water,
                            float x, float y, float z, Quaternionf rotation, float half, float uo, float vo,
                            int color, int alpha, int light) {
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        float u0 = water.getU(uo);
        float u1 = water.getU(uo + 0.5f);
        float v0 = water.getV(vo);
        float v1 = water.getV(vo + 0.5f);
        float[][] uv = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = rotation.transform(new Vector3f(CORNERS[i][0] * half, CORNERS[i][1] * half, CORNERS[i][2] * half));
        }
        for (int[] face : FACES) {
            for (int k = 0; k < 4; k++) {
                Vector3f corner = corners[face[k]];
                float px = x + corner.x();
                float py = y + corner.y();
                float pz = z + corner.z();
                VertexConsumer vertex = pose != null ? consumer.addVertex(pose, px, py, pz) : consumer.addVertex(px, py, pz);
                vertex.setColor(red, green, blue, alpha)
                        .setUv(uv[k][0], uv[k][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light);
                if (pose != null) {
                    vertex.setNormal(pose, 0f, 1f, 0f);
                } else {
                    vertex.setNormal(0f, 1f, 0f);
                }
            }
        }
    }

    /** A steady random number in [0, 1) for an id and a salt. */
    public static float hash(long id, int salt) {
        double value = Math.sin(id * 12.9898 + salt * 78.233) * 43758.5453;
        return (float) (value - Math.floor(value));
    }
}
