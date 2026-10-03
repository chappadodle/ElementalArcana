package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shapes of light for additive particles (AdditiveParticles), drawn from one white texel of the
 * particle sheet and tinted: thin square bars (a lightning bolt's, a pillar's) and flat rings and
 * discs on the ground. Points are camera-relative.
 */
final class GlowMesh {
    private GlowMesh() {
    }

    /** A square bar from a to b, {@code half} out from its axis each way, its faces drawn from both sides. */
    static void bar(VertexConsumer buffer, float u, float v, Vector3f a, Vector3f b, float half,
                    float red, float green, float blue, float alpha) {
        Vector3f axis = new Vector3f(b).sub(a);
        if (axis.lengthSquared() < 1.0e-8f) {
            return;
        }
        axis.normalize();
        Vector3f side = Math.abs(axis.y()) > 0.9f ? new Vector3f(1, 0, 0).cross(axis) : new Vector3f(0, 1, 0).cross(axis);
        side.normalize().mul(half);
        Vector3f up = new Vector3f(axis).cross(side).normalize().mul(half);
        Vector3f[] around = {new Vector3f(side).add(up), new Vector3f(up).sub(side), new Vector3f(side).add(up).negate(), new Vector3f(side).sub(up)};
        for (int k = 0; k < 4; k++) {
            Vector3f p = around[k];
            Vector3f q = around[(k + 1) % 4];
            Vector3f[] quad = {new Vector3f(a).add(p), new Vector3f(a).add(q), new Vector3f(b).add(q), new Vector3f(b).add(p)};
            for (int i = 0; i < 4; i++) {
                vertex(buffer, u, v, quad[i], red, green, blue, alpha);
            }
            for (int i = 3; i >= 0; i--) {
                vertex(buffer, u, v, quad[i], red, green, blue, alpha);
            }
        }
    }

    /** A flat ring lying at {@code center}, between {@code inner} and {@code outer} from it, seen from above and below. */
    static void ring(VertexConsumer buffer, float u, float v, Vector3f center, float inner, float outer, int segments,
                     float red, float green, float blue, float alpha) {
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments;
            float a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f[] quad = {
                    new Vector3f(center).add(Mth.cos(a0) * inner, 0, Mth.sin(a0) * inner),
                    new Vector3f(center).add(Mth.cos(a0) * outer, 0, Mth.sin(a0) * outer),
                    new Vector3f(center).add(Mth.cos(a1) * outer, 0, Mth.sin(a1) * outer),
                    new Vector3f(center).add(Mth.cos(a1) * inner, 0, Mth.sin(a1) * inner)};
            for (int k = 0; k < 4; k++) {
                vertex(buffer, u, v, quad[k], red, green, blue, alpha);
            }
            for (int k = 3; k >= 0; k--) {
                vertex(buffer, u, v, quad[k], red, green, blue, alpha);
            }
        }
    }

    private static void vertex(VertexConsumer buffer, float u, float v, Vector3f at, float red, float green, float blue, float alpha) {
        buffer.addVertex(at.x(), at.y(), at.z()).setUv(u, v).setColor(red, green, blue, alpha).setLight(LightTexture.FULL_BRIGHT);
    }
}
