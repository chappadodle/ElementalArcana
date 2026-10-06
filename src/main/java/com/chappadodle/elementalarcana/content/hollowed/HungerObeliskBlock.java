package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.content.circle.Commissions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Hunger Obelisk (see the Hollowed spec): the stone at a Hollowed camp's middle that drinks at
 * the cracks in the seals. Its block entity calls the camp's people and keeps them strong; broken,
 * it shatters ({@link HollowedCamps#shatter}).
 */
public class HungerObeliskBlock extends BaseEntityBlock {
    public static final MapCodec<HungerObeliskBlock> CODEC = simpleCodec(HungerObeliskBlock::new);

    public HungerObeliskBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HungerObeliskBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModHollowed.OBELISK_ENTITY.get(), HungerObeliskBlockEntity::serverTick);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            MagicTriggers.fire(serverPlayer, "obelisk", null, 1);
            Commissions.obeliskBroken(serverPlayer);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            HollowedCamps.shatter(server, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The dark it drinks: drawn up out of the ground and into it. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 1.2,
                    pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.03, 0);
        }
    }
}
