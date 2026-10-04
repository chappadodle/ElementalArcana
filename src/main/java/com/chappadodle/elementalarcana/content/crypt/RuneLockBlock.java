package com.chappadodle.elementalarcana.content.crypt;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * A rune gate's keystone (see the Arcane Crypts spec): set in the middle of the gate's wall over the
 * runic seal, facing into the gate's room. Its notches light as the room's runes do; when all are
 * lit it opens (Runes#open). Everything it guards follows from where it is: the room in front of
 * it, the seal under it, the coffins three blocks to either side.
 */
public class RuneLockBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RuneLockBlock> CODEC = simpleCodec(RuneLockBlock::new);
    public static final IntegerProperty PROGRESS = IntegerProperty.create("progress", 0, 3);
    public static final BooleanProperty OPEN = BooleanProperty.create("open");

    public RuneLockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PROGRESS, 0).setValue(OPEN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PROGRESS, OPEN);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
