package com.chappadodle.elementalarcana.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * A chunk of the ground blown out by an explosion: a small 3D cube cut from the block's own
 * texture (a random patch of it, tinted like the block, e.g. grass), shaded per face like blocks
 * are, tumbling through the air. It bounces off what it hits (vanilla particles just stop), rolls to
 * a halt and shrinks away.
 */
public class DebrisParticle extends Particle {
    private static final float[][] CORNERS = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}};
    /** The cube's faces, as corner indices wound around each face. */
    private static final int[][] FACES = {
            {0, 3, 2, 1}, {4, 5, 6, 7}, {0, 4, 7, 3}, {1, 2, 6, 5}, {3, 7, 6, 2}, {0, 1, 5, 4}};
    private static final float[][] NORMALS = {{0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}};

    private final TextureAtlasSprite sprite;
    private final float u0, u1, v0, v1;
    private final float size;
    private final Vector3f spinAxis;
    private float spinSpeed;
    private float angle;
    private float oldAngle;

    public DebrisParticle(ClientLevel level, Vec3 at, Vec3 velocity, BlockState state, BlockPos pos, float size) {
        super(level, at.x, at.y, at.z);
        this.sprite = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
        // A random quarter of the texture, like vanilla's block-breaking bits.
        float uo = random.nextFloat() * 3f;
        float vo = random.nextFloat() * 3f;
        this.u0 = sprite.getU(uo / 4f);
        this.u1 = sprite.getU((uo + 1f) / 4f);
        this.v0 = sprite.getV(vo / 4f);
        this.v1 = sprite.getV((vo + 1f) / 4f);
        if (IClientBlockExtensions.of(state).areBreakingParticlesTinted(state, level, pos)) {
            int tint = Minecraft.getInstance().getBlockColors().getColor(state, level, pos, 0);
            rCol = (tint >> 16 & 0xFF) / 255f;
            gCol = (tint >> 8 & 0xFF) / 255f;
            bCol = (tint & 0xFF) / 255f;
        }
        this.size = size;
        setSize(size, size);
        this.xd = velocity.x;
        this.yd = velocity.y;
        this.zd = velocity.z;
        this.gravity = 1f;
        this.lifetime = 40 + random.nextInt(30);
        this.spinAxis = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize();
        this.spinSpeed = 0.25f + random.nextFloat() * 0.35f;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        oldAngle = angle;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        yd -= 0.04 * gravity;
        Vec3 wanted = new Vec3(xd, yd, zd);
        Vec3 moved = Entity.collideBoundingBox(null, wanted, getBoundingBox(), level, List.of());
        setBoundingBox(getBoundingBox().move(moved));
        setLocationFromBoundingbox();
        onGround = false;
        if (wanted.y != moved.y) {
            if (wanted.y < 0) {
                onGround = true;
                // Bounce if it came down hard; otherwise settle.
                yd = wanted.y < -0.1 ? -wanted.y * 0.35 : 0;
                xd *= 0.6;
                zd *= 0.6;
                spinSpeed *= 0.6f;
            } else {
                yd = 0;
            }
        }
        if (wanted.x != moved.x) {
            xd = -xd * 0.3;
        }
        if (wanted.z != moved.z) {
            zd = -zd * 0.3;
        }
        xd *= 0.98;
        yd *= 0.98;
        zd *= 0.98;
        angle += spinSpeed;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        float life = (age + partialTicks) / lifetime;
        float half = size / 2f * (life < 0.75f ? 1f : Math.max(0f, 1f - (life - 0.75f) / 0.25f));
        if (half <= 0f) {
            return;
        }
        Quaternionf rotation = new Quaternionf().rotateAxis(Mth.lerp(partialTicks, oldAngle, angle), spinAxis);
        Vec3 cameraPos = camera.getPosition();
        float cx = (float) (Mth.lerp(partialTicks, xo, x) - cameraPos.x);
        // Its position is the bottom of its box; draw it around the middle.
        float cy = (float) (Mth.lerp(partialTicks, yo, y) - cameraPos.y) + size / 2f;
        float cz = (float) (Mth.lerp(partialTicks, zo, z) - cameraPos.z);
        int light = getLightColor(partialTicks);
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = rotation.transform(new Vector3f(CORNERS[i][0] * half, CORNERS[i][1] * half, CORNERS[i][2] * half));
        }
        float[][] uv = {{u0, v1}, {u0, v0}, {u1, v0}, {u1, v1}};
        for (int f = 0; f < 6; f++) {
            Vector3f normal = rotation.transform(new Vector3f(NORMALS[f][0], NORMALS[f][1], NORMALS[f][2]));
            // Block-style shading: tops brightest, bottoms darkest.
            float shade = 0.75f + 0.25f * normal.y();
            for (int k = 0; k < 4; k++) {
                Vector3f corner = corners[FACES[f][k]];
                buffer.addVertex(cx + corner.x(), cy + corner.y(), cz + corner.z())
                        .setUv(uv[k][0], uv[k][1])
                        .setColor(rCol * shade, gCol * shade, bCol * shade, 1f)
                        .setLight(light);
            }
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }
}
