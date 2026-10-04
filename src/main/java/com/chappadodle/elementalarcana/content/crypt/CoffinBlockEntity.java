package com.chappadodle.elementalarcana.content.crypt;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Watches for someone coming near a closed coffin (see CoffinBlock): within COFFIN_REACH blocks
 * across, and 3 up or down. Holds nothing of its own.
 */
public class CoffinBlockEntity extends BlockEntity {

    public CoffinBlockEntity(BlockPos pos, BlockState state) {
        super(ModCrypts.COFFIN_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CoffinBlockEntity coffin) {
        if (Math.floorMod(level.getGameTime() + pos.hashCode(), 10) != 0 || !(level instanceof ServerLevel server)
                || server.getDifficulty() == Difficulty.PEACEFUL || state.getValue(CoffinBlock.OPEN) || state.getValue(CoffinBlock.SEALED)) {
            return;
        }
        double reach = CoffinBlock.reach();
        for (ServerPlayer player : server.players()) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dz = player.getZ() - (pos.getZ() + 0.5);
            if (!player.isSpectator() && dx * dx + dz * dz <= reach * reach && Math.abs(player.getY() - pos.getY()) < 3) {
                CoffinBlock.open(server, pos, player);
                return;
            }
        }
    }
}
