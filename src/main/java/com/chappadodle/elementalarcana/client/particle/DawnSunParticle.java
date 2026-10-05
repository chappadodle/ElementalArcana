package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.client.DynamicLights;
import com.chappadodle.elementalarcana.content.DawnbreakOptions;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.Dawnbreaks;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dawnbreak's sun, drawn on this client where it was called for as long as it shines (the server
 * sends one particle, see DawnbreakOptions). It's a block of a sun, as Minecraft's own sun is a
 * square: a glowing glowstone core turning slowly in a halo of light, gold rays turning round it in
 * the plane facing the camera, motes of light drifting down from it. It rises over its first
 * second and a half to its height (Dawnbreaks'), and sinks and dims over its last second. With a
 * dynamic lights mod it lights up the ground under it too.
 */
public class DawnSunParticle extends Particle {
    private static final ResourceLocation CORE = ResourceLocation.withDefaultNamespace("block/glowstone");
    private static final ResourceLocation PLAIN = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final int FADE_TICKS = 20;
    private static final int RAYS = 10;
    /** A unit cube's corners, and its faces wound to face outward (as in StormcloudParticle). */
    private static final float[][] CORNERS = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    private static final int[][] FACES = {
            {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {3, 7, 6, 2}, {0, 1, 5, 4}};

    private final double baseY;

    protected DawnSunParticle(ClientLevel level, double x, double y, double z, DawnbreakOptions options) {
        super(level, x, y, z);
        this.baseY = y;
        this.lifetime = options.duration();
        this.gravity = 0;
        this.hasPhysics = false;
        setPos(x, y + rise(0), z);
    }

    /** How high over the place it was called the sun is, {@code t} ticks after it was called. */
    private double rise(float t) {
        float up = Mth.clamp(t / Dawnbreaks.RISE_TICKS, 0f, 1f);
        float sink = Mth.clamp((lifetime - t) / FADE_TICKS, 0f, 1f);
        float eased = 1f - (1f - up) * (1f - up);
        return 1.5 + (Dawnbreaks.HEIGHT - 1.5) * eased * (0.6f + 0.4f * sink);
    }

    /** How bright and big it is: full but for its last second. */
    private float strength(float t) {
        return Mth.clamp((lifetime - t) / FADE_TICKS, 0f, 1f) * Mth.clamp(t / 6f, 0.3f, 1f);
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
        setPos(x, baseY + rise(age), z);
        // The halo: a big soft glow kept alight at the sun's middle.
        level.addParticle(GlowParticleOptions.of(ModContent.FLARE.get(), 0xFFF2B0, 0xFFC050, 2.6f * strength(age), 3), x, y, z, 0, 0, 0);
        if (age % 2 == 0) {
            level.addParticle(GlowParticleOptions.of(ModContent.FLARE.get(), 0xFFE9A0, 0xFFB040, 0.35f, 14),
                    x + (random.nextDouble() - 0.5) * 2.4, y + (random.nextDouble() - 0.5) * 2.4, z + (random.nextDouble() - 0.5) * 2.4,
                    0, 0, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x + (random.nextDouble() - 0.5) * 6, y - 0.8, z + (random.nextDouble() - 0.5) * 6,
                    0, -0.05, 0);
        }
        if (age % 20 == 1) {
            DynamicLights.flash(new Vec3(x, y, z), 15, 25);
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float t = age + partialTicks;
        float strength = strength(t);
        if (strength <= 0f) {
            return;
        }
        Vec3 camPos = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTicks, xo, x) - camPos.x);
        float cy = (float) (Mth.lerp(partialTicks, yo, y) - camPos.y);
        float cz = (float) (Mth.lerp(partialTicks, zo, z) - camPos.z);
        TextureAtlasSprite core = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(CORE);
        TextureAtlasSprite plain = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(PLAIN);
        float pulse = 1f + 0.06f * Mth.sin(t * 0.25f);
        Quaternionf turn = new Quaternionf().rotationY(t * 0.03f).rotateX(0.5f).rotateZ(0.3f);
        cube(buffer, core, cx, cy, cz, turn, 0.7f * pulse * strength, 255, 245, 200, 255);
        rays(buffer, plain, camera, cx, cy, cz, t, strength);
    }

    /** Rays round the sun, in the plane facing the camera, turning slowly: long and short by turns, pale gold at the root, deep gold at the tip. */
    private static void rays(VertexConsumer buffer, TextureAtlasSprite sprite, Camera camera, float cx, float cy, float cz, float t, float strength) {
        Vector3f right = new Vector3f(1, 0, 0).rotate(camera.rotation());
        Vector3f up = new Vector3f(0, 1, 0).rotate(camera.rotation());
        for (int i = 0; i < RAYS; i++) {
            float angle = t * 0.02f + Mth.TWO_PI * i / RAYS;
            float length = (i % 2 == 0 ? 3.2f : 2.3f) * strength;
            float width = 0.11f * strength;
            Vector3f along = new Vector3f(right).mul(Mth.cos(angle)).add(new Vector3f(up).mul(Mth.sin(angle)));
            Vector3f across = new Vector3f(right).mul(-Mth.sin(angle)).add(new Vector3f(up).mul(Mth.cos(angle))).mul(width);
            Vector3f start = new Vector3f(along).mul(1.1f * strength).add(cx, cy, cz);
            Vector3f end = new Vector3f(along).mul(1.1f * strength + length).add(cx, cy, cz);
            quad(buffer, sprite, start, end, across);
        }
    }

    /** A flat ray from {@code start} to {@code end}, {@code across} wide each way, both windings so it shows from either side. */
    private static void quad(VertexConsumer buffer, TextureAtlasSprite sprite, Vector3f start, Vector3f end, Vector3f across) {
        Vector3f[] corners = {new Vector3f(start).sub(across), new Vector3f(start).add(across), new Vector3f(end).add(across), new Vector3f(end).sub(across)};
        float[][] uv = {{sprite.getU0(), sprite.getV1()}, {sprite.getU0(), sprite.getV0()}, {sprite.getU1(), sprite.getV0()}, {sprite.getU1(), sprite.getV1()}};
        int[][] orders = {{0, 1, 2, 3}, {3, 2, 1, 0}};
        for (int[] order : orders) {
            for (int k : order) {
                Vector3f corner = corners[k];
                buffer.addVertex(corner.x(), corner.y(), corner.z()).setUv(uv[k][0], uv[k][1])
                        .setColor(255, k < 2 ? 238 : 196, k < 2 ? 170 : 72, 255)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            }
        }
    }

    private static void cube(VertexConsumer buffer, TextureAtlasSprite sprite, float x, float y, float z, Quaternionf turn, float half,
                             int red, int green, int blue, int alpha) {
        float[][] uv = {{sprite.getU0(), sprite.getV1()}, {sprite.getU0(), sprite.getV0()}, {sprite.getU1(), sprite.getV0()}, {sprite.getU1(), sprite.getV1()}};
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = turn.transform(new Vector3f(CORNERS[i][0] * half, CORNERS[i][1] * half, CORNERS[i][2] * half));
        }
        for (int[] face : FACES) {
            for (int k = 0; k < 4; k++) {
                Vector3f corner = corners[face[k]];
                buffer.addVertex(x + corner.x(), y + corner.y(), z + corner.z()).setUv(uv[k][0], uv[k][1])
                        .setColor(red, green, blue, alpha).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x - 5, y - 5, z - 5, x + 5, y + 5, z + 5);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static class Provider implements ParticleProvider<DawnbreakOptions> {
        @Override
        public Particle createParticle(DawnbreakOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new DawnSunParticle(level, x, y, z, options);
        }
    }
}
