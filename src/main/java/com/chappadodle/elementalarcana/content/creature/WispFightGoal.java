package com.chappadodle.elementalarcana.content.creature;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A wisp in a fight: it keeps 5 to 8 blocks from its target, circling it a little above its head
 * (now and then turning the other way), and leaves the casting to CastMobSpellGoal.
 */
final class WispFightGoal extends Goal {
    private final WispEntity wisp;
    private double angle;
    private int direction = 1;
    private int repath;

    WispFightGoal(WispEntity wisp) {
        this.wisp = wisp;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = wisp.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        LivingEntity target = wisp.getTarget();
        if (target != null) {
            angle = Math.atan2(wisp.getZ() - target.getZ(), wisp.getX() - target.getX());
        }
        repath = 0;
    }

    @Override
    public void tick() {
        LivingEntity target = wisp.getTarget();
        if (target == null) {
            return;
        }
        wisp.getLookControl().setLookAt(target, 30f, 30f);
        if (--repath > 0 && wisp.getNavigation().isInProgress()) {
            return;
        }
        RandomSource random = wisp.getRandom();
        repath = 20 + random.nextInt(20);
        if (random.nextInt(4) == 0) {
            direction = -direction;
        }
        for (int tries = 0; tries < 6; tries++) {
            angle += direction * (0.4 + random.nextDouble() * 0.5);
            double distance = 5 + random.nextDouble() * 3;
            Vec3 point = new Vec3(target.getX() + Math.cos(angle) * distance,
                    target.getY() + target.getBbHeight() + 0.5 + random.nextDouble() * 1.5,
                    target.getZ() + Math.sin(angle) * distance);
            if (wisp.level().noCollision(wisp, wisp.getBoundingBox().move(point.subtract(wisp.position())))) {
                wisp.getNavigation().moveTo(point.x, point.y, point.z, 1.0);
                return;
            }
        }
        // Boxed in: make for the open air over the target.
        wisp.getNavigation().moveTo(target.getX(), target.getEyeY() + 1.5, target.getZ(), 1.0);
    }

    @Override
    public void stop() {
        wisp.getNavigation().stop();
    }
}
