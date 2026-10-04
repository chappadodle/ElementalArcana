package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.event.SpellCastEvent;
import com.chappadodle.elementalarcana.content.EssenceService;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The rune gates (see the Arcane Crypts spec): a spell cast at a runestone of its element's family
 * lights it, and so does its family's Essence (RunestoneBlock). Each lit rune tells the keystone
 * whose room holds it; when every rune in the room is lit, the keystone opens: the seal under it
 * dissolves and the coffins beside it burst open.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Runes {
    private static final double TELL_RADIUS = 24;

    private Runes() {
    }

    @SubscribeEvent
    public static void onCast(SpellCastEvent.Post event) {
        ServerPlayer caster = event.caster();
        Element element = EssenceService.elementOf(event.spell());
        if (element == null) {
            return;
        }
        HitResult hit = caster.pick(CryptRules.RUNE_REACH, 1f, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        BlockState state = caster.serverLevel().getBlockState(pos);
        if (state.getBlock() instanceof RunestoneBlock && !state.getValue(RunestoneBlock.LIT) && RunestoneBlock.answers(state, element)) {
            light(caster.serverLevel(), pos, state, caster);
        }
    }

    /** Lights a runestone and tells its keystone. */
    public static void light(ServerLevel level, BlockPos pos, BlockState state, @Nullable Player by) {
        level.setBlock(pos, state.setValue(RunestoneBlock.LIT, true), Block.UPDATE_ALL);
        int color = RunestoneBlock.element(state).color();
        level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), color, color, 0.5f, 10),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.45, 0.45, 0.45, 0.05);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 0.8f);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1f, 1.4f);
        BlockPos lock = findLock(level, pos);
        if (lock != null) {
            update(level, lock);
        }
    }

    /** What an unlit rune answers to, or that a lit one burns, on the action bar. */
    public static void explain(Player player, BlockState state) {
        Element element = RunestoneBlock.element(state);
        Component name = Component.translatable("school.elementalarcana." + element.family().name().toLowerCase(Locale.ROOT))
                .withColor(element.family().color());
        player.displayClientMessage(state.getValue(RunestoneBlock.LIT)
                ? Component.translatable("message.elementalarcana.rune.lit").withStyle(ChatFormatting.GRAY)
                : Component.translatable("message.elementalarcana.rune.answers", name).withStyle(ChatFormatting.GRAY), true);
    }

    /** The keystone whose room holds the rune at {@code rune}. */
    @Nullable
    public static BlockPos findLock(ServerLevel level, BlockPos rune) {
        for (BlockPos pos : BlockPos.betweenClosed(rune.offset(-12, -4, -12), rune.offset(12, 4, 12))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof RuneLockBlock && roomOf(pos, state).isInside(rune)) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** The room a keystone faces: 11 by 11 in front of it, from its floor (four blocks under the keystone) to its ceiling. */
    public static BoundingBox roomOf(BlockPos lock, BlockState state) {
        BlockPos middle = lock.relative(state.getValue(RuneLockBlock.FACING), 6);
        return new BoundingBox(middle.getX() - 5, lock.getY() - 3, middle.getZ() - 5, middle.getX() + 5, lock.getY() + 1, middle.getZ() + 5);
    }

    /** Counts the room's lit runes onto the keystone, and opens it once they're all lit. */
    public static void update(ServerLevel level, BlockPos lock) {
        BlockState state = level.getBlockState(lock);
        if (!(state.getBlock() instanceof RuneLockBlock) || state.getValue(RuneLockBlock.OPEN)) {
            return;
        }
        BoundingBox room = roomOf(lock, state);
        int total = 0;
        int lit = 0;
        for (BlockPos pos : BlockPos.betweenClosed(room.minX(), room.minY(), room.minZ(), room.maxX(), room.maxY(), room.maxZ())) {
            BlockState rune = level.getBlockState(pos);
            if (rune.getBlock() instanceof RunestoneBlock) {
                total++;
                if (rune.getValue(RunestoneBlock.LIT)) {
                    lit++;
                }
            }
        }
        level.setBlock(lock, state.setValue(RuneLockBlock.PROGRESS, Math.min(3, lit)), Block.UPDATE_ALL);
        if (total > 0 && lit >= total) {
            open(level, lock);
        }
    }

    /** The gate opens: the seal under the keystone dissolves and the coffins beside it burst. */
    public static void open(ServerLevel level, BlockPos lock) {
        BlockState state = level.getBlockState(lock);
        level.setBlock(lock, state.setValue(RuneLockBlock.OPEN, true).setValue(RuneLockBlock.PROGRESS, 3), Block.UPDATE_ALL);
        Direction facing = state.getValue(RuneLockBlock.FACING);
        Direction side = facing.getClockWise();
        for (int dy = 1; dy <= 3; dy++) {
            for (int ds = -1; ds <= 1; ds++) {
                BlockPos seal = lock.below(dy).relative(side, ds);
                if (level.getBlockState(seal).getBlock() instanceof RunicSealBlock) {
                    level.setBlock(seal, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    level.sendParticles(ParticleTypes.ENCHANT, seal.getX() + 0.5, seal.getY() + 0.5, seal.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.6);
                    level.sendParticles(ParticleTypes.END_ROD, seal.getX() + 0.5, seal.getY() + 0.5, seal.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
        level.playSound(null, lock, SoundEvents.VAULT_OPEN_SHUTTER, SoundSource.BLOCKS, 2f, 0.7f);
        level.playSound(null, lock, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.8f);
        Player near = level.getNearestPlayer(lock.getX() + 0.5, lock.getY(), lock.getZ() + 0.5, TELL_RADIUS, false);
        for (int ds = -3; ds <= 3; ds += 6) {
            BlockPos coffin = lock.below(3).relative(side, ds);
            if (CoffinBlock.waitsSealed(level.getBlockState(coffin))) {
                CoffinBlock.open(level, coffin, near);
            }
        }
        Component message = Component.translatable("message.elementalarcana.rune.gate_open").withStyle(ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(lock).inflate(TELL_RADIUS))) {
            player.sendSystemMessage(message);
            MagicTriggers.fire(player, "rune_gate", null, 1);
        }
    }
}
