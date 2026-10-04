package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.ElementalEssenceItem;
import com.chappadodle.elementalarcana.content.world.ShrineKind;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Vector3f;

/**
 * A crypt's runestone (see the Arcane Crypts spec): dark stone carved with its element's sigil. It
 * lights when a spell of its element family is cast at it (Runes) or when it's given that family's
 * Essence, then glows and tells its gate's keystone. Anything else gets a hint of what it answers to.
 * It can't be broken.
 */
public class RunestoneBlock extends Block {
    public static final MapCodec<RunestoneBlock> CODEC = simpleCodec(RunestoneBlock::new);
    public static final EnumProperty<ShrineKind> ELEMENT = EnumProperty.create("element", ShrineKind.class);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public RunestoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ELEMENT, ShrineKind.FIRE).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ELEMENT, LIT);
    }

    public static Element element(BlockState state) {
        return state.getValue(ELEMENT).element();
    }

    /** Whether an element lights this rune: any of its element's family. */
    public static boolean answers(BlockState state, Element element) {
        return element != null && element.family() == element(state).family();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!state.getValue(LIT) && stack.getItem() instanceof ElementalEssenceItem essence && answers(state, essence.element())) {
            if (level instanceof ServerLevel server) {
                stack.consume(1, player);
                Runes.light(server, pos, state, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            Runes.explain(player, state);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** A lit rune sheds motes of its element. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || random.nextInt(2) != 0) {
            return;
        }
        int color = element(state).color();
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 0.8f);
        level.addParticle(dust, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 1.1, pos.getY() + random.nextDouble(),
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 1.1, 0, 0.03, 0);
    }
}
