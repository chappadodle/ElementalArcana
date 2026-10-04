package com.chappadodle.elementalarcana.content.people;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Arcanist's workstation: a lectern with an open book of glowing runes and a small crystal
 * floating over it. It faces whoever placed it, and enchanting glyphs drift in to its book. Bounty
 * Contracts are handed in here.
 */
public class ArcaneLecternBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ArcaneLecternBlock> CODEC = simpleCodec(ArcaneLecternBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(4, 2, 4, 12, 13, 12),
            Block.box(0, 12, 2, 16, 15, 14));

    public ArcaneLecternBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** A Bounty Contract handed in here pays out (see Bounties); an unfinished one is turned away. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        Bounty bounty = stack.get(ModPeople.BOUNTY.get());
        if (bounty == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && !Bounties.handIn(serverLevel, pos, serverPlayer, stack)) {
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.bounty.unfinished"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double bookX = pos.getX() + 0.5;
        double bookY = pos.getY() + 1.1;
        double bookZ = pos.getZ() + 0.5;
        if (random.nextInt(2) == 0) {
            // An enchanting glyph: it starts at the offset and flies in to the book.
            level.addParticle(ParticleTypes.ENCHANT, bookX, bookY, bookZ,
                    (random.nextDouble() - 0.5) * 3, random.nextDouble() * 1.2, (random.nextDouble() - 0.5) * 3);
        }
        if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.END_ROD, bookX + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 1.25,
                    bookZ + (random.nextDouble() - 0.5) * 0.2, 0, 0.01, 0);
        }
    }
}
