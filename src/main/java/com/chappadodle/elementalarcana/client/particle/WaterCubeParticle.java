package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.client.visual.WaterCubes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * A little cube of Minecraft water thrown up by a splash: it tumbles through the air (see
 * TumblingParticle) and, when it lands, bursts into a vanilla splash instead of bouncing.
 */
public class WaterCubeParticle extends TumblingParticle {
    private final int tint;
    private final float uo;
    private final float vo;

    public WaterCubeParticle(ClientLevel level, Vec3 at, Vec3 velocity, float size, int tint) {
        super(level, at, velocity, size, 25 + level.random.nextInt(15));
        this.tint = tint;
        this.uo = random.nextFloat() * 0.5f;
        this.vo = random.nextFloat() * 0.5f;
    }

    @Override
    public void tick() {
        super.tick();
        if (!removed && onGround) {
            level.addParticle(ParticleTypes.SPLASH, x, y + 0.05, z, 0, 0, 0);
            remove();
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float scale = fade(partialTicks);
        if (scale <= 0f) {
            return;
        }
        Vector3f center = center(camera.getPosition(), partialTicks);
        int light = getLightColor(partialTicks);
        light = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));
        WaterCubes.cube(buffer, null, WaterCubes.water(), center.x(), center.y(), center.z(), rotation(partialTicks),
                size / 2f * scale, uo, vo, tint, 230, light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
