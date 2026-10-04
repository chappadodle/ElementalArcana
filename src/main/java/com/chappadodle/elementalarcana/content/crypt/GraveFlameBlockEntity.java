package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Runs a crypt's burial chamber (see the Arcane Crypts spec). When someone (not a spectator) comes
 * within 10 blocks, the Revenant rises from its tomb, three blocks down the tomb from the flame.
 * At its half health it raises the dead (the chamber's sealed coffins); when it falls the flame goes
 * out. A Revenant that went missing without dying (a world turned peaceful) rises again once
 * someone has been near for a while.
 */
public class GraveFlameBlockEntity extends BlockEntity {
    private static final int MISSING_CHECKS = 3;

    private boolean called;
    @Nullable
    private UUID revenant;
    private int missing;

    public GraveFlameBlockEntity(BlockPos pos, BlockState state) {
        super(ModCrypts.GRAVE_FLAME_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GraveFlameBlockEntity flame) {
        if (level.getGameTime() % 10 != 0 || !(level instanceof ServerLevel server) || server.getDifficulty() == Difficulty.PEACEFUL
                || !state.getValue(GraveFlameBlock.LIT)) {
            return;
        }
        boolean someoneNear = server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, CryptRules.RISE_RADIUS,
                EntitySelector.NO_SPECTATORS) != null;
        if (!flame.called) {
            if (someoneNear) {
                flame.call(server, state);
            }
            return;
        }
        if (someoneNear && level.getGameTime() % 100 == 0) {
            Entity found = flame.revenant == null ? null : server.getEntity(flame.revenant);
            flame.missing = found != null && found.isAlive() ? 0 : flame.missing + 1;
            if (flame.missing >= MISSING_CHECKS) {
                flame.called = false;
                flame.missing = 0;
            }
            flame.setChanged();
        }
    }

    /** Where the Revenant rises: on its tomb, three blocks from the flame toward the door. */
    public Vec3 tomb() {
        Direction facing = getBlockState().getValue(GraveFlameBlock.FACING);
        BlockPos top = worldPosition.relative(facing, 3);
        return new Vec3(top.getX() + 0.5, top.getY() + 1.5, top.getZ() + 0.5);
    }

    /** The chamber's middle, nine blocks from the flame toward the door. */
    public Vec3 chamberMiddle() {
        Direction facing = getBlockState().getValue(GraveFlameBlock.FACING);
        BlockPos middle = worldPosition.relative(facing, 9);
        return new Vec3(middle.getX() + 0.5, middle.getY() - 1, middle.getZ() + 0.5);
    }

    public Element element() {
        return getBlockState().getValue(GraveFlameBlock.ELEMENT).element();
    }

    private void call(ServerLevel level, BlockState state) {
        RevenantEntity risen = ModCrypts.REVENANT.get().create(level);
        if (risen == null) {
            return;
        }
        Vec3 at = tomb();
        Direction facing = state.getValue(GraveFlameBlock.FACING);
        risen.serve(element(), worldPosition, chamberMiddle());
        risen.moveTo(at.x, at.y, at.z, facing.toYRot(), 0f);
        EventHooks.finalizeMobSpawn(risen, level, level.getCurrentDifficultyAt(worldPosition), MobSpawnType.STRUCTURE, null);
        risen.startRising();
        level.addFreshEntity(risen);
        called = true;
        revenant = risen.getUUID();
        missing = 0;
        setChanged();
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.5, at.z, 40, 0.6, 0.8, 0.6, 0.05);
        level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 0.5, at.z, 20, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, worldPosition, SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 2f, 0.5f);
        level.playSound(null, worldPosition, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2f, 0.6f);
        Component message = Component.translatable("message.elementalarcana.crypt.revenant").withStyle(ChatFormatting.DARK_PURPLE);
        level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(24)).forEach(player -> player.sendSystemMessage(message));
    }

    /** The Revenant's call at half health: every sealed coffin of the chamber bursts open. */
    public void raiseDead(@Nullable Entity target) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        Vec3 middle = chamberMiddle();
        BlockPos center = BlockPos.containing(middle);
        Player near = target instanceof Player player ? player : null;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-12, -1, -12), center.offset(12, 3, 12))) {
            if (CoffinBlock.waitsSealed(server.getBlockState(pos))) {
                CoffinBlock.open(server, pos.immutable(), near);
            }
        }
    }

    /** The Revenant fell: the flame goes out. */
    public void conquer() {
        if (!(level instanceof ServerLevel server) || !getBlockState().getValue(GraveFlameBlock.LIT)) {
            return;
        }
        server.setBlock(worldPosition, getBlockState().setValue(GraveFlameBlock.LIT, false), Block.UPDATE_ALL);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, worldPosition.getX() + 0.5, worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5,
                30, 0.3, 0.4, 0.3, 0.02);
        server.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.5f, 0.6f);
        Component message = Component.translatable("message.elementalarcana.crypt.silent").withStyle(ChatFormatting.GRAY);
        server.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(32)).forEach(player -> player.sendSystemMessage(message));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("called", called);
        if (revenant != null) {
            tag.putUUID("revenant", revenant);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        called = tag.getBoolean("called");
        revenant = tag.hasUUID("revenant") ? tag.getUUID("revenant") : null;
    }
}
