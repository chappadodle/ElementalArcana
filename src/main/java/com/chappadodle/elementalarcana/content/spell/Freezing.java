package com.chappadodle.elementalarcana.content.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;

final class Freezing {

    private Freezing() {
    }

    /** Turns still water near {@code center} into Frost Walker-style frosted ice that melts back on its own. */
    static void freezeWater(ServerLevel level, BlockPos center, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -1, -radius), center.offset(radius, 1, radius))) {
            if (pos.distSqr(center) > radius * radius + 1) {
                continue;
            }
            if (level.getBlockState(pos).is(Blocks.WATER) && level.getFluidState(pos).isSource() && level.isEmptyBlock(pos.above())) {
                level.setBlockAndUpdate(pos, Blocks.FROSTED_ICE.defaultBlockState());
                level.scheduleTick(pos.immutable(), Blocks.FROSTED_ICE, Mth.nextInt(level.getRandom(), 60, 120));
            }
        }
    }
}
