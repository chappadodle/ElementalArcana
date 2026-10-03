package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a little textured cube the way blocks look: every face wears the same patch of a texture
 * and is shaded by how much it faces up (tops brightest, bottoms darkest).
 */
final class BlockMesh {
    private static final float[][] CORNERS = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    /** The cube's faces, as corner indices wound around each face. */
    private static final int[][] FACES = {
            {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {3, 7, 6, 2}, {0, 1, 5, 4}};
    private static final float[][] NORMALS = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

    private BlockMesh() {
    }

    /**
     * A cube {@code half} out from {@code center} (camera-relative) each way, turned by
     * {@code rotation}, its faces cut from {@code u0..u1, v0..v1} on the texture sheet.
     */
    static void cube(VertexConsumer buffer, Vector3f center, Quaternionf rotation, float half,
                     float u0, float u1, float v0, float v1, float red, float green, float blue, int light) {
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = rotation.transform(new Vector3f(CORNERS[i][0] * half, CORNERS[i][1] * half, CORNERS[i][2] * half));
        }
        float[][] uv = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
        for (int f = 0; f < 6; f++) {
            Vector3f normal = rotation.transform(new Vector3f(NORMALS[f][0], NORMALS[f][1], NORMALS[f][2]));
            float shade = 0.75f + 0.25f * normal.y();
            for (int k = 0; k < 4; k++) {
                Vector3f corner = corners[FACES[f][k]];
                buffer.addVertex(center.x() + corner.x(), center.y() + corner.y(), center.z() + corner.z())
                        .setUv(uv[k][0], uv[k][1])
                        .setColor(red * shade, green * shade, blue * shade, 1f)
                        .setLight(light);
            }
        }
    }
}
