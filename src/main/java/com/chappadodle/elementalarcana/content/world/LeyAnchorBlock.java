package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
 * A Ley Anchor (see the Ley Anchors spec): a standing stone with an amethyst on top that a mage makes
 * and sets down, a place of their own on the ley lines. Touched (used), it's remembered, by its name
 * (LeyAnchorBlockEntity); sneak-used, it opens the ley menu, as a Shrine Core does. Overworld only.
 */
public class LeyAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<LeyAnchorBlock> CODEC = simpleCodec(LeyAnchorBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(3, 0, 3, 13, 13, 13), Block.box(5, 13, 5, 11, 16, 11));

    public LeyAnchorBlock(Properties properties) {
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
        return new LeyAnchorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (level.dimension() != Level.OVERWORLD) {
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.ley.not_here"), true);
            return InteractionResult.SUCCESS;
        }
        String name = level.getBlockEntity(pos) instanceof LeyAnchorBlockEntity anchor ? anchor.name() : "";
        boolean known = LeyLines.rememberAnchor(serverPlayer, pos, name);
        MagicTriggers.fire(serverPlayer, "ley_anchor", null, 1);
        if (player.isShiftKeyDown()) {
            LeyLines.showMenu(serverPlayer, pos);
        } else {
            serverPlayer.displayClientMessage(Component.translatable(known ? "message.elementalarcana.ley_anchor.known"
                    : "message.elementalarcana.ley_anchor.touched", LeyLines.anchorTitle(name)), true);
            if (!known) {
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 1f);
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Violet sparks drifting up off the amethyst. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(GlowParticleOptions.of(ModContent.FLARE.get(), LeyLines.ANCHOR_COLOR, 0xFFFFFF, 0.06f, 24),
                    pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 1.0, pos.getZ() + 0.35 + random.nextDouble() * 0.3,
                    0, 0.02, 0);
        }
    }
}
