package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.api.TsunamiRules;
import com.chappadodle.elementalarcana.client.visual.WaterCubes;
import com.chappadodle.elementalarcana.content.TsunamiOptions;
import com.chappadodle.elementalarcana.content.spell.Tsunamis;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.Arrays;

/**
 * A Tsunami's wave, drawn on this client as it rolls (the server sends it as one particle when it
 * rises, see TsunamiOptions; the sweeping is the server's, see Tsunamis). Every frame it's built
 * where its front is along its path out of the Hydro Jet's little water cubes (WaterCubes): a
 * dozen columns across, tallest in the middle, each three layers deep (the face, and two lower
 * behind it, so the wave's back slopes away), deep blue below and lighter above, the face's top
 * cubes leaning forward into the curl, pale foam along the crest. It rises over its first ticks
 * and slumps as it breaks (the crash itself is the Tsunami Lance's ring, sent by the server),
 * throwing spray off its crest and leaving wet ground behind.
 */
public class TsunamiParticle extends Particle {
    private static final int COLUMNS = 13;
    private static final int LAYERS = 3;
    /** Blocks between a layer and the one behind it. */
    private static final float LAYER_GAP = 0.45f;
    /** Blocks between the cubes stacked up a column. */
    private static final float STACK = 0.36f;
    private static final int RISE_TICKS = 4;
    private static final int SLUMP_TICKS = 5;
    private static final int DEEP = 0x1E50B4;
    private static final int LIGHT = 0x5AA6EE;
    private static final int FOAM = 0xE6F4FF;

    private final Vec3 origin;
    private final Vec3 dir;
    private final Vec3 side;
    private final float yaw;
    private final int[] heights;
    private final int last;

    protected TsunamiParticle(ClientLevel level, double x, double y, double z, TsunamiOptions options) {
        super(level, x, y, z);
        this.origin = new Vec3(x, y, z);
        Vec3 dir = new Vec3(options.dx(), 0, options.dz());
        this.dir = dir.lengthSqr() > 1.0e-6 ? dir.normalize() : new Vec3(0, 0, 1);
        this.side = new Vec3(-this.dir.z, 0, this.dir.x);
        this.yaw = (float) Math.atan2(this.dir.x, this.dir.z);
        int foot = Mth.floor(y);
        int[] heights = options.rise().stream().mapToInt(rise -> foot + rise).toArray();
        this.heights = heights.length > 0 ? heights : new int[] {foot};
        this.last = this.heights.length - 1;
        this.lifetime = last + SLUMP_TICKS;
        this.hasPhysics = false;
        this.gravity = 0f;
        this.xd = this.yd = this.zd = 0;
    }

    /** How far along its path the front is, in points of it, {@code time} ticks after it rose. */
    private float along(float time) {
        return Mth.clamp(time, 0f, last);
    }

    private Vec3 frontAt(float along) {
        return origin.add(dir.scale(along * TsunamiRules.STEP));
    }

    private double footAt(float along) {
        int i = Mth.floor(along);
        return Mth.lerp(along - i, heights[i], heights[Math.min(i + 1, last)]);
    }

    /** How much of the wave stands: rising over its first ticks, slumping once it has broken. */
    private float strength(float time) {
        float rise = Mth.clamp((time + 1f) / RISE_TICKS, 0f, 1f);
        float slump = time <= last ? 1f : Mth.clamp(1f - (time - last) / SLUMP_TICKS, 0f, 1f);
        return rise * slump;
    }

    /** The wave's height across its front, as a share of its full height: tallest in the middle. */
    private static float profile(float across) {
        return 0.55f + 0.45f * Mth.cos((float) (Math.PI * across / (2 * Tsunamis.HALF_WIDTH)));
    }

    /** How far forward a cube of the face leans, by how far down from its top it is: the top two curl over. */
    private static float curl(int fromTop) {
        return fromTop == 0 ? 0.45f : fromTop == 1 ? 0.22f : 0f;
    }

    @Override
    public void tick() {
        if (age++ >= lifetime) {
            remove();
            return;
        }
        float along = along(age);
        Vec3 front = frontAt(along);
        double foot = footAt(along);
        float height = (float) Tsunamis.HEIGHT * strength(age);
        for (int i = 0; i < 4; i++) {
            // Spray thrown up and forward off the crest.
            float across = (random.nextFloat() * 2 - 1) * (float) Tsunamis.HALF_WIDTH;
            Vec3 at = front.add(side.scale(across));
            level.addParticle(ParticleTypes.SPLASH, at.x, foot + height * profile(across), at.z, dir.x * 0.3, 0.2, dir.z * 0.3);
        }
        if (age <= last) {
            for (int i = 0; i < 2; i++) {
                // The ground it has passed over, wet.
                float across = (random.nextFloat() * 2 - 1) * (float) Tsunamis.HALF_WIDTH;
                Vec3 at = front.add(side.scale(across)).subtract(dir.scale(1.5 + random.nextFloat()));
                level.addParticle(ParticleTypes.RAIN, at.x, foot + 0.05, at.z, 0, 0, 0);
            }
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float time = age + partialTicks;
        float strength = strength(time);
        if (strength <= 0f) {
            return;
        }
        float along = along(time);
        Vec3 front = frontAt(along);
        double foot = footAt(along);
        Vec3 cameraPos = camera.getPosition();
        TextureAtlasSprite water = WaterCubes.water();
        int light = LevelRenderer.getLightColor(level, BlockPos.containing(front.x, foot + 1, front.z));
        light = LightTexture.pack(Math.max(LightTexture.block(light), 10), LightTexture.sky(light));
        float full = (float) Tsunamis.HEIGHT * strength;
        for (int column = 0; column < COLUMNS; column++) {
            float across = (float) Tsunamis.HALF_WIDTH * ((column + 0.5f) * 2f / COLUMNS - 1f);
            float columnHeight = full * profile(across);
            for (int layer = 0; layer < LAYERS; layer++) {
                float layerHeight = columnHeight * (1f - 0.3f * layer);
                int cubes = Math.max(1, Math.round(layerHeight / STACK));
                for (int k = 0; k < cubes; k++) {
                    long id = column * 1000L + layer * 100L + k;
                    float jitter = WaterCubes.hash(id, 1);
                    float up = (k + 0.5f) * layerHeight / cubes;
                    float lean = layer == 0 ? curl(cubes - 1 - k) * strength : 0f;
                    float sway = Mth.sin(time * 0.5f + jitter * 6.3f) * 0.06f;
                    Vec3 at = front.add(side.scale(across + (jitter - 0.5f) * 0.15f)).add(dir.scale(lean - layer * LAYER_GAP + sway));
                    int tint = mix(DEEP, LIGHT, Mth.clamp(up / (float) Tsunamis.HEIGHT, 0f, 1f));
                    Quaternionf turn = new Quaternionf().rotateY(yaw).rotateX(time * 0.2f + jitter * 4f);
                    WaterCubes.cube(buffer, null, water, (float) (at.x - cameraPos.x), (float) (foot + up - cameraPos.y),
                            (float) (at.z - cameraPos.z), turn, 0.2f + 0.05f * jitter, WaterCubes.hash(id, 2) * 0.5f,
                            WaterCubes.hash(id, 3) * 0.5f, tint, 225, light);
                }
            }
            // Foam along the crest, out over the curl.
            long id = column * 1000L + 999L;
            float jitter = WaterCubes.hash(id, 1);
            Vec3 at = front.add(side.scale(across)).add(dir.scale(0.55f * strength + Mth.sin(time * 0.7f + jitter * 6.3f) * 0.08f));
            WaterCubes.cube(buffer, null, water, (float) (at.x - cameraPos.x), (float) (foot + columnHeight + 0.05f - cameraPos.y),
                    (float) (at.z - cameraPos.z), new Quaternionf().rotateY(yaw).rotateZ(time * 0.3f + jitter * 4f), 0.13f,
                    WaterCubes.hash(id, 2) * 0.5f, WaterCubes.hash(id, 3) * 0.5f, FOAM, 235, light);
        }
    }

    private static int mix(int from, int to, float t) {
        int r = Math.round(Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF));
        int g = Math.round(Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF));
        int b = Math.round(Mth.lerp(t, from & 0xFF, to & 0xFF));
        return r << 16 | g << 8 | b;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        Vec3 end = frontAt(last);
        int low = Arrays.stream(heights).min().orElse(0);
        int high = Arrays.stream(heights).max().orElse(0);
        return new AABB(origin.x, low, origin.z, end.x, high + Tsunamis.HEIGHT, end.z)
                .inflate(Tsunamis.HALF_WIDTH + 1.5, 1.5, Tsunamis.HALF_WIDTH + 1.5);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    public static class Provider implements ParticleProvider<TsunamiOptions> {
        @Override
        public Particle createParticle(TsunamiOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new TsunamiParticle(level, x, y, z, options);
        }
    }
}
