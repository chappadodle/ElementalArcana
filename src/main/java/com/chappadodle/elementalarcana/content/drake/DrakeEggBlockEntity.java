package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.DrakeRidingRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;

/**
 * A drake egg's warmth (see the Drake Riding spec), judged every second: fire near it (lava, fire,
 * magma under it), cold for a frost egg (snow or ice under or beside it), copper or a lightning rod
 * near a storm egg (twice as fast in a thunderstorm), water round a tide egg, the open sky high up
 * (120 and over) for a gale egg. Warmth adds up and never drains; once there's enough it hatches,
 * for the nearest player within 16 blocks.
 */
public class DrakeEggBlockEntity extends BlockEntity {
    private static final double OWNER_RADIUS = 16;
    private static final int GALE_HEIGHT = 120;

    private int warmth;

    public DrakeEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModDrakes.EGG_ENTITY.get(), pos, state);
    }

    public int warmth() {
        return warmth;
    }

    public void setWarmth(int warmth) {
        this.warmth = Math.max(0, warmth);
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DrakeEggBlockEntity egg) {
        if (level.getGameTime() % 20 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        if (!(state.getBlock() instanceof DrakeEggBlock block)) {
            return;
        }
        Element element = block.element();
        int gain = DrakeRidingRules.warmth(isWarm(server, pos, element), element == Element.LIGHTNING && server.isThundering()) * 20;
        if (gain == 0) {
            return;
        }
        egg.setWarmth(egg.warmth + gain);
        int cracks = DrakeRidingRules.cracks(egg.warmth);
        if (cracks != state.getValue(DrakeEggBlock.CRACKS)) {
            server.setBlock(pos, state.setValue(DrakeEggBlock.CRACKS, cracks), Block.UPDATE_ALL);
            server.playSound(null, pos, SoundEvents.SNIFFER_EGG_CRACK, SoundSource.BLOCKS, 1f, 0.8f);
        } else if (server.getRandom().nextInt(10) == 0) {
            server.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.4f, 0.6f);
        }
        if (egg.warmth >= DrakeRidingRules.HATCH_TICKS) {
            hatch(server, pos, element);
        }
    }

    /** Whether the egg at {@code pos} is kept warm in its element's way. */
    public static boolean isWarm(ServerLevel level, BlockPos pos, Element element) {
        return switch (element) {
            case FIRE -> level.getBlockState(pos.below()).is(Blocks.MAGMA_BLOCK) || near(level, pos, 2, state ->
                    state.is(Blocks.LAVA) || state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK));
            case ICE -> near(level, pos, 1, state -> state.is(BlockTags.ICE) || state.is(BlockTags.SNOW) || state.is(Blocks.POWDER_SNOW));
            case LIGHTNING -> near(level, pos, 2, state -> state.is(Blocks.LIGHTNING_ROD) || state.is(Tags.Blocks.STORAGE_BLOCKS_COPPER));
            case WATER -> level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                    || near(level, pos, 1, state -> state.getFluidState().is(net.minecraft.tags.FluidTags.WATER));
            default -> pos.getY() >= GALE_HEIGHT && level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
        };
    }

    private static boolean near(ServerLevel level, BlockPos pos, int radius, java.util.function.Predicate<BlockState> warm) {
        for (BlockPos at : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            if (!at.equals(pos) && warm.test(level.getBlockState(at))) {
                return true;
            }
        }
        return false;
    }

    private static void hatch(ServerLevel level, BlockPos pos, Element element) {
        level.removeBlock(pos, false);
        TamedDrakeEntity hatchling = ModDrakes.tamedDrake(element).create(level);
        if (hatchling == null) {
            return;
        }
        hatchling.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        Player near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, OWNER_RADIUS, EntitySelector.NO_SPECTATORS);
        if (near != null) {
            hatchling.tame(near);
            if (near instanceof net.minecraft.server.level.ServerPlayer owner) {
                MagicTriggers.fire(owner, "drake_hatched", element.name().toLowerCase(java.util.Locale.ROOT), 1);
            }
        }
        hatchling.setGrowth(0);
        hatchling.setPersistenceRequired();
        level.addFreshEntity(hatchling);
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.SNIFFER_EGG_HATCH, SoundSource.BLOCKS, 1.2f, 0.9f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("warmth", warmth);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        warmth = tag.getInt("warmth");
    }
}
