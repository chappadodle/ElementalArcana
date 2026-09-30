package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A spike of ice erupting from the ground (Glacial Lance): a tall crystal, its lower half buried,
 * leaning out from the impact. It shoots up in a few ticks, stands, then crumbles, sinking back
 * into the ground and breaking into shards.
 */
public class IceSpikeParticle extends Particle {
    private static final int GROW_TICKS = 4;

    private final TextureAtlasSprite sprite;
    private final ResourceLocation texture;
    private final float height;
    private final float radius;
    private final Quaternionf lean;
    private boolean crumbled;

    /** {@code lean} tilts it from straight up; {@code height} is how tall it stands above the ground. */
    public IceSpikeParticle(ClientLevel level, Vec3 base, ResourceLocation texture, float height, Quaternionf lean, int lifetime) {
        super(level, base.x, base.y, base.z);
        this.texture = texture;
        this.sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        this.height = height;
        this.radius = height * 0.22f;
        this.lean = lean;
        this.lifetime = lifetime;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.xd = this.yd = this.zd = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!crumbled && age >= lifetime * 0.7f) {
            // Crumbling: shards break off as it sinks.
            crumbled = true;
            for (int i = 0; i < 3; i++) {
                Vec3 at = new Vec3(x, y + height * (0.2 + 0.5 * random.nextDouble()), z);
                Vec3 velocity = new Vec3((random.nextDouble() - 0.5) * 0.25, 0.15 + random.nextDouble() * 0.15, (random.nextDouble() - 0.5) * 0.25);
                Minecraft.getInstance().particleEngine.add(new IceCrystalParticle(level, at, velocity, texture, 0.15f + random.nextFloat() * 0.1f));
            }
        }
    }

    /** How much of it stands above the ground this frame: shooting up, standing, then sinking. */
    private float grown(float partialTicks) {
        float time = age + partialTicks;
        if (time < GROW_TICKS) {
            float t = time / GROW_TICKS;
            return 1f - (1f - t) * (1f - t);
        }
        float life = time / lifetime;
        return life < 0.7f ? 1f : Math.max(0f, 1f - (life - 0.7f) / 0.3f);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float grown = grown(partialTicks);
        if (grown <= 0f) {
            return;
        }
        Vec3 cameraPos = camera.getPosition();
        Vector3f base = new Vector3f((float) (x - cameraPos.x), (float) (y - cameraPos.y), (float) (z - cameraPos.z));
        int light = getLightColor(partialTicks);
        light = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));
        // The ring sits a little above the ground; the lower tip is buried.
        float top = height * grown;
        Vector3f ringCenter = new Vector3f(base).add(lean.transform(new Vector3f(0, top * 0.15f, 0)));
        IceMesh.crystal(buffer, ringCenter, lean, top * 0.85f, height * 0.3f, radius * Mth.sqrt(grown),
                sprite, 0.9f, 0.97f, 1f, 0.88f, light);
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x, y, z, x, y, z).inflate(height);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
