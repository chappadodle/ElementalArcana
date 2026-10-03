package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.content.ArcOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * A bolt of lightning (see ArcOptions): a jagged line of thin glowing bars, a white-hot core in a
 * yellow glow, with a branch or two splitting off a long one. It's struck anew every other tick,
 * so it crackles, and flickers out in a few ticks, like vanilla's lightning bolt made small. A bolt
 * from the sky (wide) also flashes the sky.
 */
public class LightningArcParticle extends Particle {
    private static final int LIFE = 6;
    private static final float GLOW = 0.065f;
    private static final float CORE = 0.024f;

    private final Vec3 from;
    private final Vec3 to;
    private final double skip;
    private final float width;
    private final float u;
    private final float v;
    /** The bolt as it is struck this tick: segments, each a start and an end. */
    private final List<Vec3[]> segments = new ArrayList<>();
    /** How many of the segments are the main bolt (the rest are its branches, drawn thinner). */
    private int trunk;

    protected LightningArcParticle(ClientLevel level, double x, double y, double z, ArcOptions options, TextureAtlasSprite white) {
        super(level, x, y, z);
        this.from = new Vec3(x, y, z);
        this.to = from.add(options.dx(), options.dy(), options.dz());
        this.skip = options.skip();
        this.width = options.width();
        this.lifetime = LIFE;
        this.gravity = 0f;
        this.hasPhysics = false;
        // The flare's middle is solid white: the bars are drawn from it, tinted.
        this.u = white.getU(0.5f);
        this.v = white.getV(0.5f);
        strike();
        if (width >= 2f) {
            level.setSkyFlashTime(2);
        }
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        if (age % 2 == 0) {
            strike();
        }
    }

    /** Lays out a new jagged bolt from start to end, with a branch or two off a long one. */
    private void strike() {
        segments.clear();
        Vec3 line = to.subtract(from);
        double length = line.length();
        if (length < 1.0e-3) {
            return;
        }
        double wander = Math.min(0.35, 0.08 + length * 0.05) * Math.max(1f, width);
        List<Vec3> points = jagged(from, to, Math.max(3, (int) Math.round(length / 0.8)), wander);
        for (int i = 0; i + 1 < points.size(); i++) {
            segments.add(new Vec3[]{points.get(i), points.get(i + 1)});
        }
        trunk = segments.size();
        int branches = length > 3 ? 1 + random.nextInt(2) : 0;
        Vec3 dir = line.scale(1 / length);
        for (int b = 0; b < branches; b++) {
            Vec3 start = points.get(1 + random.nextInt(points.size() - 2));
            Vec3 out = dir.add(randomUnit().scale(0.9)).normalize();
            double reach = (0.7 + random.nextDouble() * 1.0) * Math.max(1f, width * 0.6f);
            List<Vec3> fork = jagged(start, start.add(out.scale(reach)), 2 + random.nextInt(2), wander * 0.7);
            for (int i = 0; i + 1 < fork.size(); i++) {
                segments.add(new Vec3[]{fork.get(i), fork.get(i + 1)});
            }
        }
    }

    /** {@code kinks} points from a to b, the inner ones knocked up to {@code wander} off the line. */
    private List<Vec3> jagged(Vec3 a, Vec3 b, int kinks, double wander) {
        List<Vec3> points = new ArrayList<>(kinks + 1);
        for (int i = 0; i <= kinks; i++) {
            Vec3 point = a.lerp(b, i / (double) kinks);
            if (i > 0 && i < kinks) {
                point = point.add(randomUnit().scale(wander * (0.4 + 0.6 * random.nextDouble())));
            }
            points.add(point);
        }
        return points;
    }

    private Vec3 randomUnit() {
        Vec3 r = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian());
        return r.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : r.normalize();
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float life = (age + partialTicks) / lifetime;
        float alpha = life < 0.34f ? 1f : Math.max(0f, 1f - (life - 0.34f) / 0.66f);
        if (age % 2 == 1) {
            alpha *= 0.7f;
        }
        if (alpha <= 0f) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        for (int i = 0; i < segments.size(); i++) {
            Vec3[] segment = segments.get(i);
            if (segment[0].distanceTo(from) < skip) {
                continue;
            }
            // Branches are thinner than the bolt (and no thicker than a spell's own bolt), and so is
            // what passes right by the eye (a caster's own bolt would fill their view).
            float thickness = i < trunk ? width : Math.min(1f, width * 0.6f);
            Vector3f a = segment[0].subtract(cameraPos).toVector3f();
            Vector3f b = segment[1].subtract(cameraPos).toVector3f();
            thickness *= Mth.clamp(new Vector3f(a).add(b).mul(0.5f).length() / 4f, 0.35f, 1f);
            GlowMesh.bar(buffer, u, v, a, b, GLOW * thickness, 1f, 0.8f, 0.28f, alpha * 0.55f);
            GlowMesh.bar(buffer, u, v, a, b, CORE * thickness, 1f, 1f, 0.92f, alpha);
        }
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(from, to).inflate(1 + width);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticles.RENDER_TYPE;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<ArcOptions> {
        @Override
        public Particle createParticle(ArcOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new LightningArcParticle(level, x, y, z, options, sprites.get(0, 1));
        }
    }
}
