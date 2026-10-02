package com.chappadodle.elementalarcana.content.tower;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A caster's footwork: backs off when its target is closer than {@code min}, closes in when it's
 * farther than {@code max} or out of sight, and otherwise sidesteps around it. The casting itself
 * is CastMobSpellGoal's.
 */
final class KeepDistanceGoal extends Goal {
    private final PathfinderMob mob;
    private final double min;
    private final double max;
    private int repath;
    private int direction = 1;

    KeepDistanceGoal(PathfinderMob mob, double min, double max) {
        this.mob = mob;
        this.min = min;
        this.max = max;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        repath = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (--repath > 0) {
            return;
        }
        repath = 10 + mob.getRandom().nextInt(10);
        double distance = mob.distanceTo(target);
        if (distance < min) {
            Vec3 away = DefaultRandomPos.getPosAway(mob, 8, 3, target.position());
            if (away != null) {
                mob.getNavigation().moveTo(away.x, away.y, away.z, 1.15);
            }
        } else if (distance > max || !mob.getSensing().hasLineOfSight(target)) {
            mob.getNavigation().moveTo(target, 1.0);
        } else {
            if (mob.getRandom().nextInt(3) == 0) {
                direction = -direction;
            }
            Vec3 offset = mob.position().subtract(target.position());
            double angle = Math.atan2(offset.z, offset.x) + direction * 0.7;
            mob.getNavigation().moveTo(target.getX() + Math.cos(angle) * distance, mob.getY(),
                    target.getZ() + Math.sin(angle) * distance, 0.8);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
