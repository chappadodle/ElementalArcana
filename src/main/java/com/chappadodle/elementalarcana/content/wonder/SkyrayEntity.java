package com.chappadodle.elementalarcana.content.wonder;

import com.chappadodle.elementalarcana.api.WonderRules;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A skyray (see the Wonders of the Wild spec): a great gentle ray gliding in slow wide circles high
 * over the sea. It circles an anchor (where it came, drifting off across the sea every few
 * minutes), rising and falling a little as it goes; hurt, it climbs away fast for a while. Wind
 * streams off its wingtips; gliders near it are borne up on its wake (Wonders, SkyrayWake). It
 * fights nothing and drops nothing.
 */
public class SkyrayEntity extends FlyingMob {
    private static final double SPEED = 0.3;
    private static final int CLIMB_TICKS = 200;

    @Nullable
    private Vec3 anchor;
    private double radius;
    private boolean clockwise;
    private double angle;
    private long nextDrift;
    private int climbing;

    public SkyrayEntity(EntityType<? extends SkyrayEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        long time = level().getGameTime();
        if (anchor == null) {
            anchor = position();
            radius = 18 + random.nextDouble() * 20;
            clockwise = random.nextBoolean();
            angle = random.nextDouble() * Math.PI * 2;
            nextDrift = time + 2400 + random.nextInt(2400);
        }
        if (time >= nextDrift) {
            // Off across the sea.
            nextDrift = time + 2400 + random.nextInt(2400);
            anchor = anchor.add((random.nextDouble() - 0.5) * 96, 0, (random.nextDouble() - 0.5) * 96);
        }
        angle += (clockwise ? -1 : 1) * SPEED / radius;
        double height = anchor.y + Math.sin(time * 0.01 + getId()) * 6 + (climbing > 0 ? 24 : 0);
        height = Mth.clamp(height, WonderRules.SKYRAY_MIN_Y, WonderRules.SKYRAY_MAX_Y + 24);
        Vec3 target = new Vec3(anchor.x + Math.cos(angle) * radius, height, anchor.z + Math.sin(angle) * radius);
        Vec3 want = target.subtract(position()).normalize().scale(climbing > 0 ? SPEED * 1.8 : SPEED);
        Vec3 motion = getDeltaMovement().lerp(want, 0.08);
        setDeltaMovement(motion);
        float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90f;
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
        setXRot((float) (-Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
        if (climbing > 0) {
            climbing--;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && tickCount % 2 == 0) {
            // Wind streaming off its wingtips.
            float yaw = yBodyRot * Mth.DEG_TO_RAD;
            Vec3 side = new Vec3(Mth.cos(yaw), 0, Mth.sin(yaw));
            Vec3 trail = getDeltaMovement().scale(-0.4);
            for (int tip = -1; tip <= 1; tip += 2) {
                Vec3 at = position().add(0, 0.3, 0).add(side.scale(2.4 * tip));
                level().addParticle(ModContent.WIND_STREAK.get(), at.x, at.y, at.z, trail.x, trail.y, trail.z);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide()) {
            climbing = CLIMB_TICKS;
        }
        return hurt;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2, 0.6, 2);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.CONDUIT_AMBIENT_SHORT;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 600;
    }

    @Override
    protected float getSoundVolume() {
        return 3f;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }
}
