package com.chappadodle.elementalarcana.content.crypt;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A rune gate's seal: a shimmering wall of runes that nothing breaks. It dissolves when its gate's
 * runes are all lit (Runes#open).
 */
public class RunicSealBlock extends Block {
    public static final MapCodec<RunicSealBlock> CODEC = simpleCodec(RunicSealBlock::new);

    public RunicSealBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, net.minecraft.core.Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.ENCHANT, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), (random.nextDouble() - 0.5) * 0.4, 0.1, (random.nextDouble() - 0.5) * 0.4);
        }
    }
}
