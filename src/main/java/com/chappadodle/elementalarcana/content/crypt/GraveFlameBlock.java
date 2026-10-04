package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.content.world.ShrineKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A crypt's grave flame: a stone brazier at the head of the Revenant's tomb, burning in the crypt's
 * colour (tinted by ArcanaClient). FACING looks down the tomb toward the chamber's door. Its block
 * entity raises the Revenant and its dead; when the Revenant falls it goes out (LIT false). It
 * can't be broken.
 */
public class GraveFlameBlock extends BaseEntityBlock {
    public static final MapCodec<GraveFlameBlock> CODEC = simpleCodec(GraveFlameBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ShrineKind> ELEMENT = EnumProperty.create("element", ShrineKind.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 10, 14);

    public GraveFlameBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ELEMENT, ShrineKind.FIRE).setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ELEMENT, LIT);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GraveFlameBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || !state.getValue(LIT) ? null
                : createTickerHelper(type, ModCrypts.GRAVE_FLAME_ENTITY.get(), GraveFlameBlockEntity::serverTick);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 0, 0.02, 0);
            }
            return;
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SOUL, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.9,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.03, 0);
        }
    }
}
