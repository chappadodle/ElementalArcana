package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A spike of rock heaved up out of the ground (Tremor): a stack of three stone cubes, each smaller
 * than the one below and turned a little, leaning the way {@code lean} tilts it. It shoots up out
 * of the ground in a few ticks, stands, then sinks back into it, shedding a few chips of stone.
 */
public class StoneSpikeParticle extends Particle {
    private static final int RISE_TICKS = 3;
    /** Each cube's size, as a share of the spike's height, from the bottom up. */
    private static final float[] CUBES = {0.55f, 0.4f, 0.27f};
    /** How much of the bottom cube stays buried when the spike stands. */
    private static final float BURIED = 0.12f;

    private final TextureAtlasSprite sprite;
    private final float height;
    private final Quaternionf lean;
    private final Quaternionf[] turns = new Quaternionf[CUBES.length];
    private final float[][] patches = new float[CUBES.length][];
    private boolean crumbled;

    public StoneSpikeParticle(ClientLevel level, Vec3 base, TextureAtlasSprite sprite, float height, Quaternionf lean, int lifetime) {
        super(level, base.x, base.y, base.z);
        this.sprite = sprite;
        this.height = height;
        this.lean = lean;
        this.lifetime = lifetime;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.xd = this.yd = this.zd = 0;
        for (int i = 0; i < CUBES.length; i++) {
            // Each cube turned about the spike's axis a little, and wearing its own half of the texture.
            turns[i] = new Quaternionf(lean).rotateY((random.nextFloat() - 0.5f) * 0.7f);
            float uo = random.nextFloat() * 0.5f;
            float vo = random.nextFloat() * 0.5f;
            patches[i] = new float[]{sprite.getU(uo), sprite.getU(uo + 0.5f), sprite.getV(vo), sprite.getV(vo + 0.5f)};
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!crumbled && age >= lifetime * 0.7f) {
            // Sinking: chips break off it.
            crumbled = true;
            for (int i = 0; i < 3; i++) {
                Vec3 at = new Vec3(x, y + height * (0.2 + 0.4 * random.nextDouble()), z);
                Vec3 velocity = new Vec3((random.nextDouble() - 0.5) * 0.2, 0.1 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.2);
                Minecraft.getInstance().particleEngine.add(new DebrisParticle(level, at, velocity, sprite, 0.08f + random.nextFloat() * 0.08f));
            }
        }
    }

    /** How far out of the ground it is this frame: shooting up, standing, then sinking. */
    private float risen(float partialTicks) {
        float time = age + partialTicks;
        if (time < RISE_TICKS) {
            float t = time / RISE_TICKS;
            return 1f - (1f - t) * (1f - t);
        }
        float life = time / lifetime;
        return life < 0.7f ? 1f : Math.max(0f, 1f - (life - 0.7f) / 0.3f);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float risen = risen(partialTicks);
        if (risen <= 0f) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        Vector3f base = new Vector3f((float) (x - cameraPos.x), (float) (y - cameraPos.y), (float) (z - cameraPos.z));
        int light = getLightColor(partialTicks);
        // Up from the bottom of the stack; below the ground's surface, the ground hides it.
        float along = -BURIED * height - (1f - risen) * height;
        for (int i = 0; i < CUBES.length; i++) {
            float size = CUBES[i] * height;
            Vector3f center = new Vector3f(base).add(lean.transform(new Vector3f(0, along + size / 2f, 0)));
            float[] patch = patches[i];
            BlockMesh.cube(buffer, center, turns[i], size / 2f, patch[0], patch[1], patch[2], patch[3], 1f, 1f, 1f, light);
            along += size * 0.92f;
        }
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x, y, z, x, y, z).inflate(height + 0.5f);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    /** A tilt of {@code degrees} from straight up toward {@code (dx, dz)} (a horizontal unit vector). */
    public static Quaternionf leaning(double dx, double dz, float degrees) {
        return new Quaternionf().rotateAxis((float) Math.toRadians(degrees), (float) dz, 0f, (float) -dx);
    }
}
