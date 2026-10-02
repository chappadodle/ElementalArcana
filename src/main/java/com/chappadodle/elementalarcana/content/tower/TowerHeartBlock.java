package com.chappadodle.elementalarcana.content.tower;

import com.chappadodle.elementalarcana.content.world.ShrineKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A mage tower's heart: a great crystal of the tower's element over a pedestal, in its sanctum. Its
 * block entity wakes the tower and calls the Magister; when the Magister falls it goes dark (LIT
 * false). It can't be broken.
 */
public class TowerHeartBlock extends BaseEntityBlock {
    public static final MapCodec<TowerHeartBlock> CODEC = simpleCodec(TowerHeartBlock::new);
    public static final EnumProperty<ShrineKind> KIND = EnumProperty.create("kind", ShrineKind.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    public TowerHeartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, ShrineKind.FIRE).setValue(LIT, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, LIT);
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
        return new TowerHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModTowers.TOWER_HEART_ENTITY.get(), TowerHeartBlockEntity::serverTick);
    }

    /** A lit heart sheds motes of its element's colour; a dark one now and then a wisp of smoke. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.6;
        double z = pos.getZ() + 0.5;
        if (!state.getValue(LIT)) {
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y + 0.4, z, 0, 0.02, 0);
            }
            return;
        }
        int color = state.getValue(KIND).element().color();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f), 1.2f);
        for (int i = 0; i < 2; i++) {
            level.addParticle(dust, x + (random.nextDouble() - 0.5) * 1.6, y + random.nextDouble() * 1.2,
                    z + (random.nextDouble() - 0.5) * 1.6, 0, 0.03, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, x, y + 0.6, z, (random.nextDouble() - 0.5) * 0.05, 0.05,
                    (random.nextDouble() - 0.5) * 0.05);
        }
    }
}
