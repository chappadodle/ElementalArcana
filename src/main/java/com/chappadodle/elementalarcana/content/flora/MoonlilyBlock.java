package com.chappadodle.elementalarcana.content.flora;

import com.chappadodle.elementalarcana.api.Herb;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Moonlily: a lily pad on still water whose pale flower opens at night and glows (see
 * BloomingHerbBlock). Like a lily pad it lies on water or ice with air over it, can be stood on,
 * and a boat runs it down.
 */
public class MoonlilyBlock extends BloomingHerbBlock {
    public static final MapCodec<MoonlilyBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            herbCodec(), propertiesCodec()).apply(instance, MoonlilyBlock::new));
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 1.5, 15);

    public MoonlilyBlock(Herb herb, Properties properties) {
        super(herb, properties);
    }

    @Override
    protected MapCodec<? extends MoonlilyBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape shape() {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return (level.getFluidState(pos).getType() == Fluids.WATER || state.getBlock() instanceof IceBlock)
                && level.getFluidState(pos.above()).getType() == Fluids.EMPTY;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level instanceof ServerLevel && entity instanceof Boat) {
            level.destroyBlock(new BlockPos(pos), true, entity);
        }
    }
}
