package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/** Calls a nest's drake, circling high over it, when someone first comes within 64 blocks; then the heart is gone. */
public class NestHeartBlockEntity extends BlockEntity {
    private static final double CALL_RADIUS = 64;
    private static final double CIRCLE_HEIGHT = 24;

    public NestHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModDrakes.NEST_HEART_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, NestHeartBlockEntity heart) {
        if (level.getGameTime() % 20 != 0 || !(level instanceof ServerLevel server) || !WispSpawner.canSpawn(server)
                || server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, CALL_RADIUS, EntitySelector.NO_SPECTATORS) == null) {
            return;
        }
        Element element = state.getValue(NestHeartBlock.ELEMENT).element();
        DrakeEntity drake = ModDrakes.drake(element).create(server);
        if (drake != null) {
            drake.setHome(pos);
            drake.moveTo(pos.getX() + 0.5, pos.getY() + CIRCLE_HEIGHT, pos.getZ() + 0.5, server.getRandom().nextFloat() * 360f, 0f);
            EventHooks.finalizeMobSpawn(drake, server, server.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
            server.addFreshEntity(drake);
        }
        server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
