package com.chappadodle.elementalarcana.content.flora;

import com.chappadodle.elementalarcana.api.Herb;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * A herb whose flower opens and shuts with the sun (the Sunpetal by day, the Moonlily by night; see
 * Herb.Bloom), and glows only open. It turns when a random tick reaches it, so a meadow at dusk
 * closes over a minute or two rather than all at once.
 */
public class BloomingHerbBlock extends HerbBlock {
    public static final MapCodec<BloomingHerbBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            herbCodec(), propertiesCodec()).apply(instance, BloomingHerbBlock::new));

    public BloomingHerbBlock(Herb herb, Properties properties) {
        super(herb, properties);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.OPEN, herb.openAt(true)));
    }

    @Override
    protected MapCodec<? extends BloomingHerbBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.OPEN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(BlockStateProperties.OPEN, herb().openAt(context.getLevel().isDay()));
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean open = herb().openAt(level.isDay());
        if (state.getValue(BlockStateProperties.OPEN) != open) {
            level.setBlock(pos, state.setValue(BlockStateProperties.OPEN, open), Block.UPDATE_ALL);
        }
    }
}
