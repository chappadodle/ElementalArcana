package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.content.world.ShrineKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A sanctum's Seal: a rune stone of its Sovereign's element on top of the dais. Its block entity
 * wakes the Sovereign; when the Sovereign falls it is restored. It can't be broken.
 */
public class SanctumSealBlock extends BaseEntityBlock {
    public static final MapCodec<SanctumSealBlock> CODEC = simpleCodec(SanctumSealBlock::new);
    public static final EnumProperty<ShrineKind> KIND = EnumProperty.create("kind", ShrineKind.class,
            ShrineKind.FIRE, ShrineKind.WATER, ShrineKind.WIND, ShrineKind.EARTH);
    public static final EnumProperty<SealState> STATE = EnumProperty.create("state", SealState.class);

    public SanctumSealBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, ShrineKind.FIRE).setValue(STATE, SealState.SEALED));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, STATE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SanctumSealBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModSanctums.SANCTUM_SEAL_ENTITY.get(), SanctumSealBlockEntity::serverTick);
    }

    /**
     * Sealed, a few motes of its element drift off it; awake, its element streams up out of it;
     * restored, a steady column of light rises from it.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        int color = state.getValue(KIND).element().color();
        DustParticleOptions dust = new DustParticleOptions(new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f), 1.4f);
        switch (state.getValue(STATE)) {
            case SEALED -> {
                if (random.nextInt(3) == 0) {
                    level.addParticle(dust, x + (random.nextDouble() - 0.5), y + random.nextDouble() * 0.5, z + (random.nextDouble() - 0.5), 0, 0.02, 0);
                }
            }
            case AWAKE -> {
                for (int i = 0; i < 3; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    level.addParticle(dust, x + Math.cos(angle) * 0.6, y + random.nextDouble() * 0.3, z + Math.sin(angle) * 0.6, 0, 0.15, 0);
                }
            }
            case RESTORED -> {
                level.addParticle(ParticleTypes.END_ROD, x + (random.nextDouble() - 0.5) * 0.3, y + 0.1, z + (random.nextDouble() - 0.5) * 0.3,
                        0, 0.12 + random.nextDouble() * 0.05, 0);
                if (random.nextInt(2) == 0) {
                    level.addParticle(dust, x + (random.nextDouble() - 0.5) * 1.2, y + random.nextDouble() * 2, z + (random.nextDouble() - 0.5) * 1.2,
                            0, 0.05, 0);
                }
            }
        }
    }
}
