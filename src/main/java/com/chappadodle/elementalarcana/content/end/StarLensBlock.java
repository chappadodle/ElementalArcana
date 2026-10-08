package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.api.FarIslesRules;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Star Lens (see the Far Isles spec): a lens at a pillar's foot in an observatory, facing its
 * orrery, showing one of four constellations (FarIslesRules#CONSTELLATIONS). Use it to turn it to
 * the next; its orrery then looks whether the four lenses show its chart.
 */
public class StarLensBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<StarLensBlock> CODEC = simpleCodec(StarLensBlock::new);
    public static final IntegerProperty CONSTELLATION = IntegerProperty.create("constellation", 0, FarIslesRules.CONSTELLATIONS.size() - 1);
    /** How far round a lens its orrery is looked for. */
    private static final int ORRERY_SEARCH = FarIslesRules.LENS_DISTANCE + 2;

    public StarLensBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONSTELLATION, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONSTELLATION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        int next = FarIslesRules.next(state.getValue(CONSTELLATION));
        server.setBlock(pos, state.setValue(CONSTELLATION, next), Block.UPDATE_ALL);
        server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 0.8f + 0.15f * next);
        server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        player.displayClientMessage(Component.translatable("message.elementalarcana.star_lens.turned",
                Component.translatable("constellation.elementalarcana." + FarIslesRules.CONSTELLATIONS.get(next))), true);
        AstralOrreryBlockEntity orrery = nearestOrrery(server, pos);
        if (orrery != null) {
            orrery.check(player);
        }
        return InteractionResult.CONSUME;
    }

    private static AstralOrreryBlockEntity nearestOrrery(ServerLevel level, BlockPos pos) {
        for (BlockPos at : BlockPos.withinManhattan(pos, ORRERY_SEARCH, 3, ORRERY_SEARCH)) {
            if (level.getBlockEntity(at) instanceof AstralOrreryBlockEntity orrery) {
                return orrery;
            }
        }
        return null;
    }
}
