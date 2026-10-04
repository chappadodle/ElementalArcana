package com.chappadodle.elementalarcana.content.world;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * The heart of an elemental shrine: a carved pedestal with a floating crystal of its element. It
 * can't be broken. Nearby players stand in a place of power (faster mana), and using it either
 * stirs a sleeping player's magic toward its element or blesses an awakened one (see Shrines).
 */
public class ShrineCoreBlock extends BaseEntityBlock {
    public static final MapCodec<ShrineCoreBlock> CODEC = simpleCodec(ShrineCoreBlock::new);
    public static final EnumProperty<ShrineKind> KIND = EnumProperty.create("kind", ShrineKind.class);
    private static final VoxelShape SHAPE = Shapes.or(box(1, 0, 1, 15, 6, 15), box(5, 7, 5, 11, 15, 11));

    public ShrineCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, ShrineKind.FIRE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND);
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
        return new ShrineCoreBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModWorld.SHRINE_CORE_ENTITY.get(), ShrineCoreBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof ShrineCoreBlockEntity shrine) {
            // Every shrine touched joins the mage's ley lines; sneaking, they open the ley menu instead.
            LeyLines.remember(serverPlayer, pos, state.getValue(KIND));
            if (serverPlayer.isShiftKeyDown()) {
                LeyLines.showMenu(serverPlayer, pos);
            } else {
                Shrines.use(serverPlayer, shrine, state.getValue(KIND));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Motes of the element drifting up around the crystal, and now and then a glint. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int color = state.getValue(KIND).element().color();
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.0f);
        for (int i = 0; i < 2; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double radius = 0.6 + random.nextDouble() * 1.4;
            level.addParticle(dust, pos.getX() + 0.5 + Math.cos(angle) * radius, pos.getY() + random.nextDouble() * 1.2,
                    pos.getZ() + 0.5 + Math.sin(angle) * radius, 0, 0.02, 0);
        }
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.7 + random.nextDouble() * 0.4, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.02, 0.02, (random.nextDouble() - 0.5) * 0.02);
        }
    }
}
