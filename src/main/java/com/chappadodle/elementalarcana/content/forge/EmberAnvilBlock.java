package com.chappadodle.elementalarcana.content.forge;

import com.chappadodle.elementalarcana.api.StatGear;
import com.chappadodle.elementalarcana.api.TemperRules;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Ember Anvil (see the Ember Anvil spec): an anvil of blackstone and dark iron with an ember
 * inlay, on every Cinder Forge's dais (and made with an Ember Core). Used holding a focus or a piece
 * of a mage's robe, with an Ember Core and five levels of experience to spend, it tempers the piece
 * once more (three tempers at most: TemperRules); short of anything, it says what. It has an anvil's
 * shape but doesn't fall, and does nothing else.
 */
public class EmberAnvilBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<EmberAnvilBlock> CODEC = simpleCodec(EmberAnvilBlock::new);
    // An anvil's shape: a base, a narrow waist, and the face on top across the way it faces.
    private static final VoxelShape BASE = Block.box(2, 0, 2, 14, 4, 14);
    private static final VoxelShape X_AXIS = Shapes.or(BASE, Block.box(3, 4, 4, 13, 5, 12), Block.box(4, 5, 6, 12, 10, 10),
            Block.box(0, 10, 3, 16, 16, 13));
    private static final VoxelShape Z_AXIS = Shapes.or(BASE, Block.box(4, 4, 3, 12, 5, 13), Block.box(6, 5, 4, 10, 10, 12),
            Block.box(3, 10, 0, 13, 16, 16));

    public EmberAnvilBlock(Properties properties) {
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
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? X_AXIS : Z_AXIS;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof StatGear)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level instanceof ServerLevel server)) {
            return ItemInteractionResult.SUCCESS;
        }
        int times = stack.getOrDefault(ModForge.TEMPERED.get(), 0);
        boolean free = player.hasInfiniteMaterials();
        String refusal = !TemperRules.canTemper(times) ? "full"
                : !free && cores(player.getInventory()) < TemperRules.CORES ? "no_core"
                : !free && player.experienceLevel < TemperRules.LEVELS ? "no_levels"
                : null;
        if (refusal != null) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.temper." + refusal, TemperRules.LEVELS)
                    .withStyle(ChatFormatting.RED), true);
            server.playSound(null, pos, SoundEvents.CHAIN_HIT, SoundSource.BLOCKS, 0.8f, 0.6f);
            return ItemInteractionResult.CONSUME;
        }
        if (!free) {
            takeCores(player.getInventory(), TemperRules.CORES);
            player.giveExperienceLevels(-TemperRules.LEVELS);
        }
        stack.set(ModForge.TEMPERED.get(), times + 1);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.05;
        double z = pos.getZ() + 0.5;
        server.sendParticles(ParticleTypes.FLAME, x, y, z, 16, 0.3, 0.05, 0.3, 0.06);
        server.sendParticles(ParticleTypes.LAVA, x, y, z, 6, 0.2, 0.05, 0.2, 0);
        server.sendParticles(ParticleTypes.SMOKE, x, y + 0.2, z, 10, 0.2, 0.1, 0.2, 0.02);
        server.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1f, 0.8f);
        server.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.8f, 1.2f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.temper.done", stack.getHoverName(), times + 1, TemperRules.MAX)
                .withStyle(ChatFormatting.GOLD), true);
        if (player instanceof ServerPlayer serverPlayer) {
            MagicTriggers.fire(serverPlayer, "tempered", null, times + 1);
        }
        return ItemInteractionResult.CONSUME;
    }

    private static int cores(Inventory inventory) {
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModForge.EMBER_CORE.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void takeCores(Inventory inventory, int count) {
        for (int slot = 0; slot < inventory.getContainerSize() && count > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModForge.EMBER_CORE.get())) {
                int taken = Math.min(count, stack.getCount());
                stack.shrink(taken);
                count -= taken;
            }
        }
    }
}
