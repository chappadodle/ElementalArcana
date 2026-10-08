package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.FarIslesRules;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * An Astral Orrery's memory (see the Far Isles spec): its observatory's chart (one constellation for
 * each side, north, east, south and west: FarIslesRules#chart of its place, so the observatory's
 * floor can show it as it's built) and whether its vault has opened. When the four Star Lenses round it show the chart, it wakes, the
 * vault (a chest of the observatory's treasure) rises beside it on the dais, and everyone near is
 * told: the Far Isles advancement.
 */
public class AstralOrreryBlockEntity extends BlockEntity {
    private static final ResourceKey<LootTable> VAULT = ResourceKey.create(Registries.LOOT_TABLE, ElementalArcana.id("chests/observatory"));
    /** The four sides in the chart's order. */
    public static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final double WITNESS_RADIUS = 24;

    private boolean opened;

    public AstralOrreryBlockEntity(BlockPos pos, BlockState state) {
        super(ModEnd.ASTRAL_ORRERY_ENTITY.get(), pos, state);
    }

    /** The chart of an orrery standing at {@code pos} (ObservatoryPiece lays it in the floor). */
    public static int[] chartAt(BlockPos pos) {
        return FarIslesRules.chart(Mth.getSeed(pos));
    }

    public int[] chart() {
        return chartAt(worldPosition);
    }

    public boolean opened() {
        return opened;
    }

    /** What the lenses round it show (north, east, south, west; -1 where none stands). */
    public int[] lenses() {
        int[] shown = new int[SIDES.size()];
        for (int i = 0; i < SIDES.size(); i++) {
            shown[i] = -1;
            BlockPos ring = worldPosition.relative(SIDES.get(i), FarIslesRules.LENS_DISTANCE);
            for (int dy = -2; dy <= 1; dy++) {
                BlockState state = level.getBlockState(ring.above(dy));
                if (state.getBlock() instanceof StarLensBlock) {
                    shown[i] = state.getValue(StarLensBlock.CONSTELLATION);
                    break;
                }
            }
        }
        return shown;
    }

    /** Reads its chart out to {@code player}, and whether the vault is open. */
    public void describe(Player player) {
        int[] chart = chart();
        MutableComponent order = Component.empty();
        for (int i = 0; i < chart.length; i++) {
            if (i > 0) {
                order.append(", ");
            }
            order.append(Component.translatable("message.elementalarcana.orrery.side." + SIDES.get(i).getName(),
                    Component.translatable("constellation.elementalarcana." + FarIslesRules.CONSTELLATIONS.get(chart[i]))));
        }
        player.sendSystemMessage(Component.translatable(opened ? "message.elementalarcana.orrery.open" : "message.elementalarcana.orrery.chart", order)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Looks whether its lenses show the chart; if they do (and it's still sealed), opens the vault. */
    public void check(Player by) {
        if (opened || !(level instanceof ServerLevel server) || !FarIslesRules.matches(lenses(), chart())) {
            return;
        }
        opened = true;
        setChanged();
        server.setBlock(worldPosition, getBlockState().setValue(AstralOrreryBlock.AWAKE, true), Block.UPDATE_ALL);
        BlockPos vault = worldPosition.relative(Direction.SOUTH);
        server.setBlock(vault, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), Block.UPDATE_ALL);
        if (server.getBlockEntity(vault) instanceof ChestBlockEntity chest) {
            chest.setLootTable(VAULT, server.getRandom().nextLong());
        }
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.8;
        double z = worldPosition.getZ() + 0.5;
        server.sendParticles(ParticleTypes.END_ROD, x, y, z, 60, 1.2, 1.0, 1.2, 0.08);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, vault.getX() + 0.5, vault.getY() + 0.5, vault.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.05);
        server.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 1.4f);
        server.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 2f, 0.6f);
        for (ServerPlayer player : server.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(WITNESS_RADIUS))) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.orrery.wakes").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            MagicTriggers.fire(player, "far_isles", null, 1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("opened", opened);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        opened = tag.getBoolean("opened");
    }
}
