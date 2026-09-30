package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.client.sound.HydroJetSounds;
import com.chappadodle.elementalarcana.client.visual.WaterCubes;
import com.chappadodle.elementalarcana.content.WaterBurstOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * A big water effect drawn out of little Minecraft water cubes (see WaterBurstOptions):
 * <ul>
 *   <li>Whirlpool: three spiral arms of cubes flowing round and into a sunken centre, turning, for
 *   as long as the whirlpool lasts, with splashes and bubbles.</li>
 *   <li>Wave: a ring of cubes crashing outward from where a Tsunami Lance landed, rising and falling
 *   as it spreads, throwing up splashes.</li>
 * </ul>
 */
public class WaterBurstParticle extends Particle {
    private static final int ARM_CUBES = 12;
    private static final int WAVE_CUBES = 28;
    private static final int WHIRLPOOL_TINT = 0x2A8AB0;
    private static final int WAVE_TINT = 0x2E5CC8;

    private final WaterBurstOptions burst;

    protected WaterBurstParticle(ClientLevel level, double x, double y, double z, WaterBurstOptions burst) {
        super(level, x, y, z);
        this.burst = burst;
        this.lifetime = burst.ticks();
        this.hasPhysics = false;
        this.gravity = 0f;
        this.xd = this.yd = this.zd = 0;
        Vec3 at = new Vec3(x, y, z);
        if (burst.kind() == WaterBurstOptions.WAVE) {
            HydroJetSounds.wave(level, at);
        } else {
            HydroJetSounds.whirlpool(level, at, this::isAlive);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) {
            return;
        }
        float radius = burst.radius();
        if (burst.kind() == WaterBurstOptions.WAVE) {
            float r = radius * waveReach(age / (float) lifetime);
            for (int i = 0; i < 3; i++) {
                double angle = random.nextDouble() * Mth.TWO_PI;
                level.addParticle(ParticleTypes.SPLASH, x + Math.cos(angle) * r, y + 0.2, z + Math.sin(angle) * r, 0, 0.1, 0);
            }
        } else if (age % 2 == 0) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = random.nextDouble() * radius;
            level.addParticle(age % 4 == 0 ? ParticleTypes.BUBBLE_POP : ParticleTypes.SPLASH,
                    x + Math.cos(angle) * r, y + 0.15, z + Math.sin(angle) * r, 0, 0.05, 0);
        }
    }

    /** How far out the wave has crashed, 0..1 of its radius: fast at first, then easing out. */
    private static float waveReach(float life) {
        return 1f - (1f - life) * (1f - life);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float time = age + partialTicks;
        float life = Mth.clamp(time / lifetime, 0f, 1f);
        Vec3 cameraPos = camera.getPosition();
        float cx = (float) (x - cameraPos.x);
        float cy = (float) (y - cameraPos.y);
        float cz = (float) (z - cameraPos.z);
        TextureAtlasSprite water = WaterCubes.water();
        int light = getLightColor(partialTicks);
        light = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));
        float radius = burst.radius();
        if (burst.kind() == WaterBurstOptions.WAVE) {
            float r = radius * waveReach(life);
            // Rises as it spreads, then falls away.
            float height = 0.7f * Mth.sin(life * Mth.PI);
            float half = 0.2f * (1f - 0.5f * life);
            for (int i = 0; i < WAVE_CUBES; i++) {
                float angle = Mth.TWO_PI * i / WAVE_CUBES;
                float wobble = WaterCubes.hash(i, 1) * 0.3f;
                Quaternionf turn = new Quaternionf().rotateY(-angle).rotateX(time * 0.1f + wobble);
                WaterCubes.cube(buffer, null, water, cx + Mth.cos(angle) * r, cy + height * (0.8f + wobble), cz + Mth.sin(angle) * r,
                        turn, half, WaterCubes.hash(i, 2) * 0.5f, WaterCubes.hash(i, 3) * 0.5f, WAVE_TINT, 230, light);
            }
            return;
        }
        // Whirlpool: fade in and out over a few ticks at each end.
        float strength = Math.min(1f, time / 5f) * Math.min(1f, (lifetime - time) / 8f);
        if (strength <= 0f) {
            return;
        }
        for (int arm = 0; arm < 3; arm++) {
            for (int k = 0; k < ARM_CUBES; k++) {
                // Each cube flows inward along its arm, then starts again at the rim.
                float along = ((k + time * 0.08f) % ARM_CUBES) / ARM_CUBES;
                float r = radius * (1f - along) * 0.95f;
                float angle = time * 0.25f + arm * Mth.TWO_PI / 3 + along * 3.5f;
                // Sinking toward the centre, like a drain.
                float dip = -0.5f * along * along;
                float half = (0.1f + 0.08f * (1f - along)) * strength;
                long id = arm * 100L + k;
                Quaternionf turn = new Quaternionf().rotateY(-angle).rotateZ(0.3f);
                WaterCubes.cube(buffer, null, water, cx + Mth.cos(angle) * r, cy + 0.1f + dip, cz + Mth.sin(angle) * r,
                        turn, half, WaterCubes.hash(id, 2) * 0.5f, WaterCubes.hash(id, 3) * 0.5f, WHIRLPOOL_TINT, 230, light);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x, y, z, x, y, z).inflate(burst.radius() + 1, 1.5, burst.radius() + 1);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static class Provider implements ParticleProvider<WaterBurstOptions> {
        @Override
        public Particle createParticle(WaterBurstOptions burst, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new WaterBurstParticle(level, x, y, z, burst);
        }
    }
}
