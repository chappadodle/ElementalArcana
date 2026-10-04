package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.Element;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A drake's egg (see the Drake Riding spec), one block for each drake's element: kept warm in its
 * element's way it stirs, cracks (CRACKS) and hatches into a hatchling (DrakeEggBlockEntity). It's
 * picked up whole when broken.
 */
public class DrakeEggBlock extends BaseEntityBlock {
    public static final IntegerProperty CRACKS = IntegerProperty.create("cracks", 0, 2);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

    private final Element element;

    public DrakeEggBlock(Element element, Properties properties) {
        super(properties);
        this.element = element;
        registerDefaultState(stateDefinition.any().setValue(CRACKS, 0));
    }

    public Element element() {
        return element;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(properties -> new DrakeEggBlock(element, properties));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRACKS);
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
        return new DrakeEggBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModDrakes.EGG_ENTITY.get(), DrakeEggBlockEntity::serverTick);
    }

    /** A cracked egg trembles now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CRACKS) > 0 && random.nextInt(8 - state.getValue(CRACKS) * 3) == 0) {
            level.addParticle(ParticleTypes.POOF, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.9,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
    }
}
