package com.chappadodle.elementalarcana.content.end;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Astral Orrery (see the Far Isles spec): the instrument on an observatory's dais, a copper
 * stand under an amethyst sphere. It holds the observatory's star chart (AstralOrreryBlockEntity);
 * used, it reads the chart out; once its lenses show the chart it wakes and its vault opens.
 */
public class AstralOrreryBlock extends BaseEntityBlock {
    public static final MapCodec<AstralOrreryBlock> CODEC = simpleCodec(AstralOrreryBlock::new);
    public static final BooleanProperty AWAKE = BooleanProperty.create("awake");
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(6, 3, 6, 10, 8, 10),
            Block.box(3, 8, 3, 13, 16, 13));

    public AstralOrreryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AWAKE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AWAKE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AstralOrreryBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof AstralOrreryBlockEntity orrery) {
            orrery.describe(player);
            orrery.check(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
