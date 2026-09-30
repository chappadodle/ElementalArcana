package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.client.visual.WaterCubes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * A Bubble Prison cast: the server sends one particle whose velocity is the line from the caster's
 * eyes to the target, and a quick line of little water cubes zips along it (no gravity), gone as
 * they reach the target.
 */
public class BubbleCastEmitter extends NoRenderParticle {
    private static final int CUBES = 8;
    private static final int WATER = 0x5FB4FF;
    /** Blocks a tick. */
    private static final float SPEED = 2.5f;

    protected BubbleCastEmitter(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z);
        Vec3 start = new Vec3(x, y, z);
        Vec3 line = new Vec3(xd, yd, zd);
        float duration = Math.max(3f, (float) line.length() / SPEED);
        Vec3 dir = line.lengthSqr() > 1.0e-6 ? line.normalize() : new Vec3(0, 1, 0);
        Vec3 reference = Math.abs(dir.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = dir.cross(reference).normalize();
        Vec3 up = right.cross(dir).normalize();
        for (int i = 0; i < CUBES; i++) {
            Vec3 jitter = right.scale((random.nextDouble() - 0.5) * 0.14).add(up.scale((random.nextDouble() - 0.5) * 0.14));
            float size = 0.07f + random.nextFloat() * 0.05f - i * 0.004f;
            Minecraft.getInstance().particleEngine.add(new ZipCube(level, start.add(jitter), line, i * 0.6f, duration, size));
        }
        remove();
    }

    /** One little water cube zipping from {@code start} along {@code line}, {@code delay} ticks after the first. */
    private static class ZipCube extends Particle {
        private final Vec3 start;
        private final Vec3 line;
        private final float delay;
        private final float duration;
        private final float half;
        private final float uo;
        private final float vo;

        ZipCube(ClientLevel level, Vec3 start, Vec3 line, float delay, float duration, float size) {
            super(level, start.x, start.y, start.z);
            this.start = start;
            this.line = line;
            this.delay = delay;
            this.duration = duration;
            this.half = size / 2;
            this.lifetime = Mth.ceil(delay + duration) + 1;
            this.gravity = 0;
            this.uo = random.nextFloat() * 0.5f;
            this.vo = random.nextFloat() * 0.5f;
        }

        private float progress(float partialTicks) {
            return (age + partialTicks - delay) / duration;
        }

        @Override
        public void tick() {
            if (age++ >= lifetime) {
                remove();
                return;
            }
            float progress = progress(0);
            // Keep the particle where the cube is, so it isn't culled with the start of the line.
            Vec3 at = start.add(line.scale(Mth.clamp(progress, 0f, 1f)));
            setPos(at.x, at.y, at.z);
            if (progress >= 1f) {
                Vec3 end = start.add(line);
                level.addParticle(ParticleTypes.SPLASH, end.x, end.y, end.z, 0, 0.05, 0);
                remove();
            }
        }

        @Override
        public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
            float progress = progress(partialTicks);
            if (progress <= 0f || progress >= 1f) {
                return;
            }
            Vec3 at = start.add(line.scale(progress)).subtract(camera.getPosition());
            int light = getLightColor(partialTicks);
            light = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));
            float time = age + partialTicks;
            WaterCubes.cube(buffer, null, WaterCubes.water(), (float) at.x, (float) at.y, (float) at.z,
                    new Quaternionf().rotationXYZ(time * 0.5f, time * 0.4f, 0), half, uo, vo, WATER, 220, light);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.TERRAIN_SHEET;
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new BubbleCastEmitter(level, x, y, z, xd, yd, zd);
        }
    }
}
