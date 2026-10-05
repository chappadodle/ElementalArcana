package com.chappadodle.elementalarcana.content.star;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps its Starlit Lantern on Starfalls' list of lanterns while it's loaded. */
public class StarlitLanternBlockEntity extends BlockEntity {
    public StarlitLanternBlockEntity(BlockPos pos, BlockState state) {
        super(ModStars.LANTERN_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            Starfalls.addLantern(server, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel server) {
            Starfalls.removeLantern(server, worldPosition);
        }
    }
}
