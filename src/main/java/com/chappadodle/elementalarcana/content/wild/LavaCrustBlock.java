package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.WildTrophyRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Cooling Crust (see the Trophies of the Wild spec): lava cooled under a Salamander Charm's bearer,
 * the way Frost Walker freezes water. It cracks as it ages, the cracks glowing brighter, and melts
 * back to lava (vanilla frosted ice's timing, without the need for light); a crust with few crust
 * neighbours goes faster. It's hot underfoot, like magma. Broken, it's lava again.
 */
public class LavaCrustBlock extends Block {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    public LavaCrustBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, Mth.nextInt(level.getRandom(), WildTrophyRules.CRUST_FIRST_MIN_TICKS, WildTrophyRules.CRUST_FIRST_MAX_TICKS));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if ((random.nextInt(3) == 0 || fewerNeighboursThan(level, pos, 4)) && age(state, level, pos)) {
            // Melted: its neighbours weaken too.
            BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
            for (Direction direction : Direction.values()) {
                next.setWithOffset(pos, direction);
                BlockState neighbour = level.getBlockState(next);
                if (neighbour.is(this) && !age(neighbour, level, next)) {
                    level.scheduleTick(next, this, ageTicks(random));
                }
            }
        } else {
            level.scheduleTick(pos, this, ageTicks(random));
        }
    }

    private static int ageTicks(RandomSource random) {
        return Mth.nextInt(random, WildTrophyRules.CRUST_AGE_MIN_TICKS, WildTrophyRules.CRUST_AGE_MAX_TICKS);
    }

    /** One stage older; returns true if that melted it. */
    private boolean age(BlockState state, Level level, BlockPos pos) {
        int next = WildTrophyRules.nextCrustAge(state.getValue(AGE));
        if (next >= 0) {
            level.setBlock(pos, state.setValue(AGE, next), Block.UPDATE_CLIENTS);
            return false;
        }
        melt(level, pos);
        return true;
    }

    /** Lava again, with a hiss. */
    public static void melt(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
        level.neighborChanged(pos, Blocks.LAVA, pos);
        level.playSound(null, pos, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.6f, 0.8f + level.getRandom().nextFloat() * 0.4f);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (block.defaultBlockState().is(this) && fewerNeighboursThan(level, pos, 2)) {
            melt(level, pos);
        }
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
    }

    private boolean fewerNeighboursThan(BlockGetter level, BlockPos pos, int needed) {
        int count = 0;
        BlockPos.MutableBlockPos next = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            next.setWithOffset(pos, direction);
            if (level.getBlockState(next).is(this) && ++count >= needed) {
                return false;
            }
        }
        return true;
    }

    /** Hot underfoot, like magma, unless you step carefully (the charm's bearer doesn't feel it: WildCharms). */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully() && entity instanceof LivingEntity) {
            entity.hurt(level.damageSources().hotFloor(), 1f);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= 2 && random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0.02, 0);
        }
        if (age == WildTrophyRules.CRUST_MAX_AGE && random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + random.nextDouble(), pos.getY() + 1.02, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
