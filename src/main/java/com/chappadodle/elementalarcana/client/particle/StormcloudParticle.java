package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.content.StormcloudOptions;
import com.chappadodle.elementalarcana.content.spell.Stormcalls;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Stormcall's cloud, drawn on this client over its caster for as long as it lasts (the server sends
 * one particle when it's called, see StormcloudOptions). It's built like Minecraft's own clouds, of
 * flat-shaded boxes (lit on top, darker on the sides, darkest underneath), but storm-grey and
 * heaped: a broad middle, puffs round it, a few on top, each bobbing a little. It swells in, fades
 * out, flashes white when the server's strikes fall (on the same beat), and rains from its
 * underside: thin streaks falling to the caster's feet, as Minecraft's rain does, and now and then a
 * drop that splashes where it lands. Its height follows the caster's roof as Stormcalls' does.
 */
public class StormcloudParticle extends Particle {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final int GROW_TICKS = 10;
    private static final int FADE_TICKS = 12;
    private static final int BASE = 0x56566A;
    private static final int FLASH = 0xD8DCF4;
    /** A unit box's corners, and its faces (north, south, west, east, up, down) wound to face outward. */
    private static final float[][] CORNERS = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    private static final int[][] FACES = {
            {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {3, 7, 6, 2}, {0, 1, 5, 4}};
    /** How lit each face is, as Minecraft shades its clouds. */
    private static final float[] SHADE = {0.8f, 0.8f, 0.88f, 0.88f, 1.0f, 0.62f};

    private static final int RAIN = 0xA8B8DC;
    private static final int STREAKS = 26;
    /** Rain's fall, in blocks a tick, and each streak's length and half-width. */
    private static final float RAIN_SPEED = 0.9f;
    private static final float STREAK = 1.1f;
    private static final float STREAK_HALF = 0.03f;

    /** One box of the cloud: its middle (from the cloud's), its half-sizes, and its bob's phase. */
    private record Box(float x, float y, float z, float hx, float hy, float hz, float phase) {
    }

    /** One streak of rain: where it falls (from under the cloud's middle) and how far into its fall it starts. */
    private record Streak(float x, float z, float phase) {
    }

    @Nullable
    private final Entity caster;
    private final List<Box> boxes = new ArrayList<>();
    private final List<Streak> streaks = new ArrayList<>();
    private double height;
    private double targetHeight;
    private int flash;

    protected StormcloudParticle(ClientLevel level, double x, double y, double z, StormcloudOptions options) {
        super(level, x, y, z);
        this.caster = level.getEntity(options.caster());
        this.lifetime = options.duration();
        this.gravity = 0;
        this.hasPhysics = false;
        RandomSource shape = RandomSource.create(options.caster() * 31L + 7);
        boxes.add(new Box(0, 0, 0, 2.6f, 0.55f, 2.1f, 0));
        for (int i = 0; i < 7; i++) {
            double angle = Math.PI * 2 * i / 7 + shape.nextDouble() * 0.5;
            double out = 1.7 + shape.nextDouble() * 1.0;
            boxes.add(new Box((float) (Math.cos(angle) * out), (float) (shape.nextDouble() * 0.5 - 0.2), (float) (Math.sin(angle) * out),
                    0.9f + shape.nextFloat() * 0.6f, 0.45f + shape.nextFloat() * 0.3f, 0.9f + shape.nextFloat() * 0.5f, shape.nextFloat() * 6f));
        }
        for (int i = 0; i < 3; i++) {
            boxes.add(new Box(shape.nextFloat() * 2.4f - 1.2f, 0.7f + shape.nextFloat() * 0.3f, shape.nextFloat() * 2.0f - 1.0f,
                    0.8f + shape.nextFloat() * 0.4f, 0.4f + shape.nextFloat() * 0.2f, 0.8f + shape.nextFloat() * 0.4f, shape.nextFloat() * 6f));
        }
        for (int i = 0; i < STREAKS; i++) {
            double angle = shape.nextDouble() * Math.PI * 2;
            double out = Math.sqrt(shape.nextDouble()) * 3.0;
            streaks.add(new Streak((float) (Math.cos(angle) * out), (float) (Math.sin(angle) * out), shape.nextFloat()));
        }
        if (caster != null) {
            height = targetHeight = Stormcalls.cloudHeight(level, caster);
            setPos(caster.getX(), caster.getY() + height, caster.getZ());
        }
    }

    @Override
    public void tick() {
        if (caster == null || caster.isRemoved() || !caster.isAlive() || age++ >= lifetime) {
            remove();
            return;
        }
        if (age % 10 == 0) {
            targetHeight = Stormcalls.cloudHeight(level, caster);
        }
        height += (targetHeight - height) * 0.25;
        xo = x;
        yo = y;
        zo = z;
        setPos(caster.getX(), caster.getY() + height, caster.getZ());
        // On the strikes' beat (see Stormcalls), and now and then besides: a flash inside.
        if (age % Stormcalls.STRIKE_GAP_TICKS == Stormcalls.FIRST_STRIKE_TICKS || random.nextInt(45) == 0) {
            flash = 2;
        } else if (flash > 0) {
            flash--;
        }
        if (random.nextInt(3) == 0) {
            // A drop to splash where it lands (the streaks are drawn, below).
            double angle = random.nextDouble() * Math.PI * 2;
            double out = Math.sqrt(random.nextDouble()) * 3.0 * growth(0);
            level.addParticle(ParticleTypes.FALLING_WATER, x + Math.cos(angle) * out, y - 0.7, z + Math.sin(angle) * out, 0, 0, 0);
        }
    }

    /** How grown the cloud is: swelling in over its first half second, fading over its last. */
    private float growth(float partialTicks) {
        float t = age + partialTicks;
        float in = Math.min(1f, t / GROW_TICKS);
        float out = Math.min(1f, Math.max(0f, (lifetime - t) / FADE_TICKS));
        return Math.min(in, out);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float grow = growth(partialTicks);
        if (grow <= 0f) {
            return;
        }
        Vec3 camPos = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTicks, xo, x) - camPos.x);
        float cy = (float) (Mth.lerp(partialTicks, yo, y) - camPos.y);
        float cz = (float) (Mth.lerp(partialTicks, zo, z) - camPos.z);
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        int color = flash > 0 ? FLASH : BASE;
        int light = flash > 0 ? LightTexture.FULL_BRIGHT : getLightColor(partialTicks);
        float t = age + partialTicks;
        for (Box box : boxes) {
            float bob = Mth.sin(t * 0.07f + box.phase()) * 0.08f;
            float drift = Mth.cos(t * 0.05f + box.phase()) * 0.1f;
            box(buffer, sprite, cx + (box.x() + drift) * grow, cy + (box.y() + bob) * grow, cz + box.z() * grow,
                    box.hx() * grow, box.hy() * grow, box.hz() * grow, color, light);
        }
        rain(buffer, sprite, camera, cx, cy, cz, t, grow, getLightColor(partialTicks));
    }

    /** The rain: thin streaks falling from the cloud's underside to the caster's feet, turned to face the camera. */
    private void rain(VertexConsumer buffer, TextureAtlasSprite sprite, Camera camera, float cx, float cy, float cz, float t, float grow, int light) {
        float fall = (float) height - 0.5f;
        if (fall <= STREAK) {
            return;
        }
        float yaw = camera.getYRot() * Mth.DEG_TO_RAD;
        float rx = Mth.cos(yaw) * STREAK_HALF;
        float rz = Mth.sin(yaw) * STREAK_HALF;
        int alpha = (int) (150 * grow);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        for (Streak streak : streaks) {
            float down = ((t * RAIN_SPEED / fall + streak.phase()) % 1f) * fall;
            float top = cy - 0.5f * grow - down;
            float bottom = Math.max(top - STREAK, cy - (float) height);
            float sx = cx + streak.x() * grow;
            float sz = cz + streak.z() * grow;
            // Both windings, so the streak shows from either side.
            quad(buffer, sx - rx, sz - rz, sx + rx, sz + rz, bottom, top, u0, u1, v0, v1, alpha, light);
            quad(buffer, sx + rx, sz + rz, sx - rx, sz - rz, bottom, top, u0, u1, v0, v1, alpha, light);
        }
    }

    private static void quad(VertexConsumer buffer, float x0, float z0, float x1, float z1, float bottom, float top,
                             float u0, float u1, float v0, float v1, int alpha, int light) {
        int red = RAIN >> 16 & 0xFF;
        int green = RAIN >> 8 & 0xFF;
        int blue = RAIN & 0xFF;
        buffer.addVertex(x0, bottom, z0).setUv(u0, v1).setColor(red, green, blue, alpha).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light);
        buffer.addVertex(x0, top, z0).setUv(u0, v0).setColor(red, green, blue, alpha).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light);
        buffer.addVertex(x1, top, z1).setUv(u1, v0).setColor(red, green, blue, alpha).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light);
        buffer.addVertex(x1, bottom, z1).setUv(u1, v1).setColor(red, green, blue, alpha).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light);
    }

    private static void box(VertexConsumer buffer, TextureAtlasSprite sprite, float x, float y, float z, float hx, float hy, float hz,
                            int color, int light) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float[][] uv = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
        for (int f = 0; f < FACES.length; f++) {
            int red = (int) ((color >> 16 & 0xFF) * SHADE[f]);
            int green = (int) ((color >> 8 & 0xFF) * SHADE[f]);
            int blue = (int) ((color & 0xFF) * SHADE[f]);
            for (int k = 0; k < 4; k++) {
                float[] corner = CORNERS[FACES[f][k]];
                buffer.addVertex(x + corner[0] * hx, y + corner[1] * hy, z + corner[2] * hz)
                        .setUv(uv[k][0], uv[k][1])
                        .setColor(red, green, blue, 255)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light);
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(x - 5, y - height - 1, z - 5, x + 5, y + 2.5, z + 5);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static class Provider implements ParticleProvider<StormcloudOptions> {
        @Override
        public Particle createParticle(StormcloudOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new StormcloudParticle(level, x, y, z, options);
        }
    }
}
