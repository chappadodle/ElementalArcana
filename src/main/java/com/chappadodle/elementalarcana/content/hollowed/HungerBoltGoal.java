package com.chappadodle.elementalarcana.content.hollowed;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Throwing Hunger Bolts (see the Hollowed spec): with its target in sight and within 20 blocks, it
 * stops, raises its arms for a moment (violet drawn into its hands) and throws {@code bolts} of them
 * (one straight at the target, the rest fanned either side), then waits {@code cooldown} ticks or a
 * little more.
 */
public class HungerBoltGoal extends Goal {
    private static final int WINDUP_TICKS = 12;
    private static final double RANGE = 20;
    private static final double FAN_DEGREES = 14;
    private static final float SPEED = 0.8f;

    private final HollowedEntity mob;
    private final int cooldown;
    private final int bolts;
    private long readyAt;
    private int windup;
    @Nullable
    private LivingEntity target;

    public HungerBoltGoal(HollowedEntity mob, int cooldown, int bolts) {
        this.mob = mob;
        this.cooldown = cooldown;
        this.bolts = bolts;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || mob.level().getGameTime() < readyAt || mob.distanceToSqr(target) > RANGE * RANGE
                || !mob.getSensing().hasLineOfSight(target)) {
            return false;
        }
        this.target = target;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return windup > 0 && target != null && target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        windup = WINDUP_TICKS;
        mob.getNavigation().stop();
        mob.setCasting(true);
        mob.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1f, 0.7f);
    }

    @Override
    public void tick() {
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (mob.level() instanceof ServerLevel level && windup % 3 == 0) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY(0.6), mob.getZ(), 4, 0.4, 0.2, 0.4, 0.02);
        }
        if (--windup == 0) {
            throwBolts();
        }
    }

    @Override
    public void stop() {
        mob.setCasting(false);
        windup = 0;
        readyAt = mob.level().getGameTime() + cooldown + mob.getRandom().nextInt(20);
    }

    private void throwBolts() {
        if (target == null || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 from = mob.getEyePosition().subtract(0, 0.3, 0);
        Vec3 aim = target.getEyePosition().subtract(0, 0.3, 0).subtract(from);
        for (int i = 0; i < bolts; i++) {
            double degrees = (i - (bolts - 1) / 2.0) * FAN_DEGREES;
            Vec3 direction = aim.yRot((float) Math.toRadians(degrees));
            HungerBolt bolt = new HungerBolt(level, mob);
            bolt.setPos(from.x, from.y, from.z);
            bolt.shoot(direction.x, direction.y, direction.z, SPEED, 1.5f);
            level.addFreshEntity(bolt);
        }
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1f, 0.6f);
    }
}
