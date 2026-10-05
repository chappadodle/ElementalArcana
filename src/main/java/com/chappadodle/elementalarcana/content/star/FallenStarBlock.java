package com.chappadodle.elementalarcana.content.star;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A Fallen Star (see the Starfall spec): glowing in its crater, sparks drifting up from it. Mined,
 * it gives Star Fragments and the pillar of light over it goes out (Starfalls).
 */
public class FallenStarBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 11, 13);

    public FallenStarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server) {
            Starfalls.extinguish(server, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.7,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, (random.nextDouble() - 0.5) * 0.02, 0.03 + random.nextDouble() * 0.03,
                    (random.nextDouble() - 0.5) * 0.02);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.FIREWORK, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                    (random.nextDouble() - 0.5) * 0.1, 0.05, (random.nextDouble() - 0.5) * 0.1);
        }
    }
}
