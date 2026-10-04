package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.content.world.ShrineKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * An upright coffin, two blocks tall, set in a crypt's wall with its lid to the room (FACING). A
 * closed one opens when someone comes within 4 blocks, unless it's SEALED: those wait for their gate
 * or their Revenant (CoffinBlock#open). Opening, the lid bursts and one of the dead steps out of the
 * hollow behind it (CryptDead). Breaking a closed coffin lets its dead out too.
 */
public class CoffinBlock extends BaseEntityBlock {
    public static final MapCodec<CoffinBlock> CODEC = simpleCodec(CoffinBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");
    public static final EnumProperty<ShrineKind> ELEMENT = EnumProperty.create("element", ShrineKind.class);
    private static final Map<Direction, VoxelShape> OPEN_LOWER = new EnumMap<>(Direction.class);
    private static final Map<Direction, VoxelShape> OPEN_UPPER = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            // The back and sides of the hollow (and its roof up top); the lid's side is open.
            VoxelShape back = rotated(facing, 0, 0, 14, 16, 16, 16);
            VoxelShape left = rotated(facing, 0, 0, 0, 2, 16, 16);
            VoxelShape right = rotated(facing, 14, 0, 0, 16, 16, 16);
            OPEN_LOWER.put(facing, Shapes.or(back, left, right));
            OPEN_UPPER.put(facing, Shapes.or(back, left, right, rotated(facing, 0, 13, 0, 16, 16, 16)));
        }
    }

    public CoffinBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(OPEN, false).setValue(SEALED, false).setValue(ELEMENT, ShrineKind.FIRE));
    }

    /** A box given for a coffin facing north (lid at z 0), turned to {@code facing}. */
    private static VoxelShape rotated(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN, SEALED, ELEMENT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return Shapes.block();
        }
        return (state.getValue(HALF) == DoubleBlockHalf.LOWER ? OPEN_LOWER : OPEN_UPPER).get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            // The other half: gone or opened with it.
            if (!neighbor.is(this) || neighbor.getValue(HALF) == half) {
                return Blocks.AIR.defaultBlockState();
            }
            return state.setValue(OPEN, neighbor.getValue(OPEN));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !state.getValue(OPEN)) {
            BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            CryptDead.raise(server, lower, state.getValue(ELEMENT).element(), state.getValue(FACING), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Both halves have one (the world expects it of every state of a block entity block); only the lower one watches. */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CoffinBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() || state.getValue(OPEN) || state.getValue(SEALED) || state.getValue(HALF) != DoubleBlockHalf.LOWER ? null
                : createTickerHelper(type, ModCrypts.COFFIN_ENTITY.get(), CoffinBlockEntity::serverTick);
    }

    /**
     * Opens the coffin whose lower half is at {@code lower}: the lid bursts and its dead step out,
     * after {@code target} if there is one. Returns false if it was already open.
     */
    public static boolean open(ServerLevel level, BlockPos lower, @Nullable LivingEntity target) {
        BlockState state = level.getBlockState(lower);
        if (!(state.getBlock() instanceof CoffinBlock) || state.getValue(OPEN) || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return false;
        }
        level.setBlock(lower, state.setValue(OPEN, true), Block.UPDATE_CLIENTS);
        BlockState upper = level.getBlockState(lower.above());
        if (upper.getBlock() instanceof CoffinBlock) {
            level.setBlock(lower.above(), upper.setValue(OPEN, true), Block.UPDATE_CLIENTS);
        }
        Direction facing = state.getValue(FACING);
        double x = lower.getX() + 0.5 + facing.getStepX() * 0.5;
        double z = lower.getZ() + 0.5 + facing.getStepZ() * 0.5;
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.POLISHED_DEEPSLATE.defaultBlockState()),
                x, lower.getY() + 1, z, 40, 0.3, 0.8, 0.3, 0.15);
        level.sendParticles(ParticleTypes.SOUL, x, lower.getY() + 1, z, 6, 0.3, 0.6, 0.3, 0.03);
        level.playSound(null, lower, SoundEvents.DEEPSLATE_BRICKS_BREAK, SoundSource.BLOCKS, 1.4f, 0.6f);
        level.playSound(null, lower, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 0.8f, 0.6f);
        CryptDead.raise(level, lower, state.getValue(ELEMENT).element(), facing, target);
        return true;
    }

    /** Whether a coffin still waits for its gate or its Revenant. */
    public static boolean waitsSealed(BlockState state) {
        return state.getBlock() instanceof CoffinBlock && state.getValue(HALF) == DoubleBlockHalf.LOWER
                && state.getValue(SEALED) && !state.getValue(OPEN);
    }

    static double reach() {
        return CryptRules.COFFIN_REACH;
    }
}
