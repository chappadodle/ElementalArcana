package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws a six-sided ice crystal (two hexagonal pyramids joined at a ring: a shard, or a spike when
 * its lower half is buried). Each face is shaded by how much it faces up, like blocks, and drawn
 * from both sides so no face is lost whichever way it tumbles.
 */
final class IceMesh {
    private static final int SIDES = 6;

    private IceMesh() {
    }

    /**
     * {@code top} and {@code bottom} are how far the tips reach along the crystal's axis (its local
     * up), {@code radius} how wide its middle ring is; {@code center} is camera-relative.
     */
    static void crystal(VertexConsumer buffer, Vector3f center, Quaternionf rotation, float top, float bottom, float radius,
                        TextureAtlasSprite sprite, float red, float green, float blue, float alpha, int light) {
        Vector3f tip = rotation.transform(new Vector3f(0, top, 0));
        Vector3f base = rotation.transform(new Vector3f(0, -bottom, 0));
        Vector3f[] ring = new Vector3f[SIDES];
        for (int i = 0; i < SIDES; i++) {
            float a = Mth.TWO_PI * i / SIDES;
            ring[i] = rotation.transform(new Vector3f(Mth.cos(a) * radius, 0, Mth.sin(a) * radius));
        }
        float u0 = sprite.getU(0.25f);
        float u1 = sprite.getU(0.75f);
        float v0 = sprite.getV(0.1f);
        float v1 = sprite.getV(0.9f);
        for (int i = 0; i < SIDES; i++) {
            Vector3f a = ring[i];
            Vector3f b = ring[(i + 1) % SIDES];
            triangle(buffer, center, tip, a, b, u0, u1, v0, v1, red, green, blue, alpha, light);
            triangle(buffer, center, base, a, b, u0, u1, v1, v0, red, green, blue, alpha, light);
        }
    }

    private static void triangle(VertexConsumer buffer, Vector3f center, Vector3f apex, Vector3f a, Vector3f b,
                                 float u0, float u1, float vApex, float vRing,
                                 float red, float green, float blue, float alpha, int light) {
        Vector3f normal = new Vector3f(a).sub(apex).cross(new Vector3f(b).sub(apex)).normalize();
        float shade = 0.75f + 0.25f * Math.abs(normal.y());
        float uMid = (u0 + u1) / 2;
        // Both windings, as a quad with its last corner repeated.
        vertex(buffer, center, apex, uMid, vApex, red, green, blue, alpha, shade, light);
        vertex(buffer, center, a, u0, vRing, red, green, blue, alpha, shade, light);
        vertex(buffer, center, b, u1, vRing, red, green, blue, alpha, shade, light);
        vertex(buffer, center, b, u1, vRing, red, green, blue, alpha, shade, light);
        vertex(buffer, center, apex, uMid, vApex, red, green, blue, alpha, shade, light);
        vertex(buffer, center, b, u1, vRing, red, green, blue, alpha, shade, light);
        vertex(buffer, center, a, u0, vRing, red, green, blue, alpha, shade, light);
        vertex(buffer, center, a, u0, vRing, red, green, blue, alpha, shade, light);
    }

    private static void vertex(VertexConsumer buffer, Vector3f center, Vector3f point, float u, float v,
                               float red, float green, float blue, float alpha, float shade, int light) {
        buffer.addVertex(center.x() + point.x(), center.y() + point.y(), center.z() + point.z())
                .setUv(u, v)
                .setColor(red * shade, green * shade, blue * shade, alpha)
                .setLight(light);
    }
}
