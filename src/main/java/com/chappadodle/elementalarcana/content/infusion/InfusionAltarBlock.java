package com.chappadodle.elementalarcana.content.infusion;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.ElementalEssenceItem;
import com.chappadodle.elementalarcana.content.mob.MobCasting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Infusion Altar (see the Arcane Infusion spec): set a weapon or armour piece on it, pour in
 * eight Essence of one element, and it's infused. Its block entity keeps the item and the basin.
 */
public class InfusionAltarBlock extends BaseEntityBlock {
    public static final MapCodec<InfusionAltarBlock> CODEC = simpleCodec(InfusionAltarBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(3, 3, 3, 13, 10, 13),
            Block.box(1, 10, 1, 15, 13, 15));

    public InfusionAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
        return new InfusionAltarBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof InfusionAltarBlockEntity altar)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.getItem() instanceof ElementalEssenceItem essence) {
            if (altar.item().isEmpty()) {
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.translatable("message.elementalarcana.altar.empty"), true);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            if (level instanceof ServerLevel server) {
                if (altar.pour(server, player, essence.element())) {
                    stack.consume(1, player);
                } else {
                    Element held = altar.element();
                    player.displayClientMessage(held == null ? Component.translatable("message.elementalarcana.altar.full")
                            : Component.translatable("message.elementalarcana.altar.other",
                            Component.translatable("school.elementalarcana." + held.name().toLowerCase(java.util.Locale.ROOT))), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(ModInfusion.INFUSABLE) && altar.item().isEmpty()) {
            if (level instanceof ServerLevel server) {
                altar.set(server, stack.consumeAndReturn(1, player));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /** An empty hand takes the item back. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof InfusionAltarBlockEntity altar) || altar.item().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            ItemStack item = altar.take(server);
            if (!player.addItem(item)) {
                player.drop(item, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof InfusionAltarBlockEntity altar && !altar.item().isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, altar.item());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The basin glows with its Essence: motes of the element rise from it. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof InfusionAltarBlockEntity altar && altar.element() != null && altar.charge() > 0
                && random.nextInt(8) < 2 + altar.charge()) {
            level.addParticle(MobCasting.handsParticle(altar.element()), pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.85,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0, 0.02 + random.nextDouble() * 0.02, 0);
        }
    }
}
