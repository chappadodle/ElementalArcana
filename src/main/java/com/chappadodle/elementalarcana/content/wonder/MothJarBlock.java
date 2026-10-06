package com.chappadodle.elementalarcana.content.wonder;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * A Moth Jar (see the Wonders of the Wild spec): a bottled glowmoth set down, glowing in the jar in
 * its element's colour (the model's moth is tinted by ArcanaClient), a lantern that never goes out.
 * Broken, it gives the bottled glowmoth back.
 */
public class MothJarBlock extends Block {
    /** The moth's element, by its place in Element.values(). */
    public static final IntegerProperty ELEMENT = IntegerProperty.create("element", 0, Element.values().length - 1);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(4, 0, 4, 12, 9, 12), Block.box(5, 9, 5, 11, 11, 11));

    public MothJarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ELEMENT, Element.RADIANCE.ordinal()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ELEMENT);
    }

    public static Element element(BlockState state) {
        return Element.values()[state.getValue(ELEMENT)];
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(BottledGlowmothItem.of(element(state)));
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return BottledGlowmothItem.of(element(state));
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(GlowParticleOptions.of(ModContent.FLARE.get(), element(state).color(), 0xFFFFFF, 0.04f, 12),
                    pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.2 + random.nextDouble() * 0.35,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0, 0.005, 0);
        }
    }
}
