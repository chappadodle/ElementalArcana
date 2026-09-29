package com.chappadodle.elementalarcana.client.particle;

import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.client.Bloom;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The glowing particles (see GlowParticleOptions), blended additively. Each shifts from its colour
 * to its fade colour over its life; how it moves depends on its kind:
 * <ul>
 *   <li>Flare: drifts up a little, shrinks and dissolves.</li>
 *   <li>Spark: a streak turned to point along its motion; it slows down and sags.</li>
 *   <li>Feather: flutters down, swaying and rocking.</li>
 *   <li>Ring: lies flat and expands to its size, fading (a shockwave).</li>
 *   <li>Corona: a round glow that swells and fades.</li>
 * </ul>
 * An anchored particle keeps its position and motion relative to its anchor entity (for a held
 * projectile, its place in the caster's hand), and disappears with it.
 */
public class GlowParticle extends TextureSheetParticle {
    public enum Kind {
        FLARE, SPARK, FEATHER, RING, CORONA;

        /** Lies flat on the ground instead of facing the camera. */
        boolean flat() {
            return this == RING;
        }
    }

    private final Kind kind;
    private final SpriteSet sprites;
    private final float startRed, startGreen, startBlue;
    private final float endRed, endGreen, endBlue;
    private final float baseSize;
    @Nullable
    private final Entity anchor;
    private final float swayPhase;
    private float streakAngle;

    protected GlowParticle(Kind kind, ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                           GlowParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z);
        this.kind = kind;
        this.sprites = sprites;
        this.anchor = options.anchor() == GlowParticleOptions.NO_ANCHOR ? null : level.getEntity(options.anchor());
        if (anchor != null) {
            // Keep the position relative to the anchor from here on.
            setPos(x - anchor.getX(), y - anchor.getY(), z - anchor.getZ());
            xo = this.x;
            yo = this.y;
            zo = this.z;
        }
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.startRed = (options.color() >> 16 & 0xFF) / 255f;
        this.startGreen = (options.color() >> 8 & 0xFF) / 255f;
        this.startBlue = (options.color() & 0xFF) / 255f;
        this.endRed = (options.fadeColor() >> 16 & 0xFF) / 255f;
        this.endGreen = (options.fadeColor() >> 8 & 0xFF) / 255f;
        this.endBlue = (options.fadeColor() & 0xFF) / 255f;
        this.baseSize = options.size() * (0.85f + random.nextFloat() * 0.3f);
        this.lifetime = Math.max(2, Math.round(options.lifetime() * (0.8f + random.nextFloat() * 0.4f)));
        this.hasPhysics = false;
        this.swayPhase = random.nextFloat() * Mth.TWO_PI;
        switch (kind) {
            case FLARE -> {
                friction = 0.86f;
                gravity = -0.25f;
                setSpriteFromAge(sprites);
            }
            case SPARK -> {
                friction = 0.82f;
                gravity = 0.35f;
                pickSprite(sprites);
            }
            case FEATHER -> {
                friction = 0.96f;
                gravity = 0.05f;
                pickSprite(sprites);
                roll = oRoll = (random.nextFloat() - 0.5f) * 1.2f;
            }
            case RING, CORONA -> {
                friction = 0.9f;
                gravity = 0f;
                pickSprite(sprites);
                roll = oRoll = random.nextFloat() * Mth.TWO_PI;
            }
        }
        this.quadSize = baseSize;
        updateLook();
        Bloom.track(this, level, lifetime);
    }

    @Override
    public void tick() {
        if (anchor != null && anchor.isRemoved()) {
            remove();
            return;
        }
        super.tick();
        if (kind == Kind.FLARE) {
            setSpriteFromAge(sprites);
        } else if (kind == Kind.FEATHER) {
            // Rock side to side as it drifts down, never falling fast.
            oRoll = roll;
            roll = Mth.sin(age * 0.22f + swayPhase) * 0.7f;
            xd += Mth.cos(age * 0.22f + swayPhase) * 0.004;
            yd = Math.max(yd, -0.045);
        }
        updateLook();
    }

    /** Colour, size and fade for the particle's age. */
    private void updateLook() {
        float life = Mth.clamp(age / (float) lifetime, 0f, 1f);
        float shift = Mth.sqrt(life);
        setColor(Mth.lerp(shift, startRed, endRed), Mth.lerp(shift, startGreen, endGreen), Mth.lerp(shift, startBlue, endBlue));
        switch (kind) {
            case FLARE -> {
                quadSize = baseSize * (1f - 0.65f * life);
                alpha = 0.85f * (1f - life * life);
            }
            case SPARK -> {
                quadSize = baseSize * (1f - 0.4f * life);
                alpha = life < 0.6f ? 1f : 1f - (life - 0.6f) / 0.4f;
            }
            case FEATHER -> {
                quadSize = baseSize;
                alpha = life < 0.7f ? 0.9f : 0.9f * (1f - (life - 0.7f) / 0.3f);
            }
            case RING -> {
                float grown = 1f - (1f - life) * (1f - life) * (1f - life);
                quadSize = baseSize * Math.max(0.05f, grown);
                alpha = 1f - life;
            }
            case CORONA -> {
                float grown = 1f - (1f - life) * (1f - life);
                quadSize = baseSize * (0.4f + 0.6f * grown);
                alpha = 0.9f * (1f - life) * (1f - life);
            }
        }
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 base = anchorPosition(partialTicks);
        if (kind.flat()) {
            // Lying on the ground: drawn facing up and facing down, so it shows from either side.
            Vec3 cameraPos = camera.getPosition();
            float x = (float) (base.x + Mth.lerp(partialTicks, xo, this.x) - cameraPos.x);
            float y = (float) (base.y + Mth.lerp(partialTicks, yo, this.y) - cameraPos.y);
            float z = (float) (base.z + Mth.lerp(partialTicks, zo, this.z) - cameraPos.z);
            renderRotatedQuad(buffer, new Quaternionf().rotateX(-Mth.HALF_PI).rotateZ(roll), x, y, z, partialTicks);
            renderRotatedQuad(buffer, new Quaternionf().rotateX(Mth.HALF_PI).rotateZ(-roll), x, y, z, partialTicks);
            return;
        }
        Quaternionf rotation = new Quaternionf();
        getFacingCameraMode().setRotation(rotation, camera, partialTicks);
        float angle = kind == Kind.SPARK ? streakAngle(camera) : Mth.lerp(partialTicks, oRoll, roll);
        if (angle != 0f) {
            rotation.rotateZ(angle);
        }
        Vec3 cameraPos = camera.getPosition();
        renderRotatedQuad(buffer, rotation,
                (float) (base.x + Mth.lerp(partialTicks, xo, x) - cameraPos.x),
                (float) (base.y + Mth.lerp(partialTicks, yo, y) - cameraPos.y),
                (float) (base.z + Mth.lerp(partialTicks, zo, z) - cameraPos.z),
                partialTicks);
    }

    /** Where the anchor is drawn this frame (the origin for unanchored particles). */
    private Vec3 anchorPosition(float partialTicks) {
        if (anchor == null) {
            return Vec3.ZERO;
        }
        if (anchor instanceof SpellProjectile projectile && projectile.isHeld()) {
            Entity owner = projectile.getOwner();
            ProjectileSpell spell = projectile.spell();
            if (owner != null && spell != null) {
                return projectile.holdPosition(owner, spell, partialTicks);
            }
        }
        return anchor.getPosition(partialTicks);
    }

    /** The screen angle of the spark's motion, so its streak points the way it's flying. */
    private float streakAngle(Camera camera) {
        Vector3f up = camera.getUpVector();
        Vector3f right = new Vector3f(camera.getLookVector()).cross(up);
        double across = xd * right.x() + yd * right.y() + zd * right.z();
        double upward = xd * up.x() + yd * up.y() + zd * up.z();
        if (across * across + upward * upward > 1.0e-6) {
            streakAngle = (float) Math.atan2(upward, across);
        }
        return streakAngle;
    }

    @Override
    public AABB getRenderBoundingBox(float partialTicks) {
        return anchor == null ? super.getRenderBoundingBox(partialTicks) : anchor.getBoundingBox().inflate(3);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return AdditiveParticles.RENDER_TYPE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    public record Provider(Kind kind, SpriteSet sprites) implements ParticleProvider<GlowParticleOptions> {
        @Override
        public Particle createParticle(GlowParticleOptions options, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new GlowParticle(kind, level, x, y, z, xd, yd, zd, options, sprites);
        }
    }
}
