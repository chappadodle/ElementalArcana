package com.chappadodle.elementalarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * A solid 3D piece thrown by an impact (debris, ice shards): it tumbles around a random axis
 * through the air and bounces off what it hits (vanilla particles just stop), rolling to a halt.
 * Subclasses draw its shape; {@link #rotation} and {@link #fade} help them.
 */
public abstract class TumblingParticle extends Particle {
    protected final float size;
    private final Vector3f spinAxis;
    private float spinSpeed;
    private float angle;
    private float oldAngle;

    protected TumblingParticle(ClientLevel level, Vec3 at, Vec3 velocity, float size, int lifetime) {
        super(level, at.x, at.y, at.z);
        this.size = size;
        setSize(size, size);
        this.xd = velocity.x;
        this.yd = velocity.y;
        this.zd = velocity.z;
        this.gravity = 1f;
        this.lifetime = lifetime;
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

    /** Its orientation this frame. */
    protected Quaternionf rotation(float partialTicks) {
        return new Quaternionf().rotateAxis(Mth.lerp(partialTicks, oldAngle, angle), spinAxis);
    }

    /** 1 for most of its life, shrinking to 0 over the last quarter. */
    protected float fade(float partialTicks) {
        float life = (age + partialTicks) / lifetime;
        return life < 0.75f ? 1f : Math.max(0f, 1f - (life - 0.75f) / 0.25f);
    }

    /** Its centre this frame, relative to the camera (its position is the bottom of its box). */
    protected Vector3f center(Vec3 cameraPos, float partialTicks) {
        return new Vector3f(
                (float) (Mth.lerp(partialTicks, xo, x) - cameraPos.x),
                (float) (Mth.lerp(partialTicks, yo, y) - cameraPos.y) + size / 2f,
                (float) (Mth.lerp(partialTicks, zo, z) - cameraPos.z));
    }
}
