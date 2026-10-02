package com.chappadodle.elementalarcana.content.creature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A wisp with nothing to fight drifts about, 1.5 to 3 blocks above the ground (or water): near its
 * shrine if it guards one (straight back if it strayed), otherwise near where it is.
 */
final class WispWanderGoal extends Goal {
    private static final int RANGE = 8;
    private static final int MAX_DROP = 12;

    private final WispEntity wisp;

    WispWanderGoal(WispEntity wisp) {
        this.wisp = wisp;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (wisp.getTarget() != null || wisp.getNavigation().isInProgress()) {
            return false;
        }
        boolean strayed = wisp.home() != null && !wisp.isWithinRestriction();
        return strayed || wisp.getRandom().nextInt(40) == 0;
    }

    @Override
    public boolean canContinueToUse() {
        return wisp.getTarget() == null && wisp.getNavigation().isInProgress();
    }

    @Override
    public void start() {
        Vec3 point = pick();
        if (point != null) {
            wisp.getNavigation().moveTo(point.x, point.y, point.z, 0.6);
        }
    }

    @Nullable
    private Vec3 pick() {
        BlockPos home = wisp.home();
        BlockPos center = home != null ? home : wisp.blockPosition();
        int range = home != null ? WispEntity.GUARD_RADIUS - 2 : RANGE;
        RandomSource random = wisp.getRandom();
        Level level = wisp.level();
        for (int tries = 0; tries < 10; tries++) {
            BlockPos pos = new BlockPos(center.getX() + random.nextInt(range * 2 + 1) - range,
                    center.getY() + random.nextInt(5) - 2,
                    center.getZ() + random.nextInt(range * 2 + 1) - range);
            if (!level.isEmptyBlock(pos)) {
                continue;
            }
            int drop = 0;
            while (drop < MAX_DROP && level.isEmptyBlock(pos.below(drop + 1))) {
                drop++;
            }
            // pos.below(drop) is the lowest air: settle 1.5 to 3 blocks above the ground under it.
            double y = pos.getY() - drop + 1.5 + random.nextDouble() * 1.5;
            BlockPos at = BlockPos.containing(pos.getX() + 0.5, y, pos.getZ() + 0.5);
            if (level.isEmptyBlock(at)) {
                return new Vec3(pos.getX() + 0.5, y, pos.getZ() + 0.5);
            }
        }
        return null;
    }
}
