package com.chappadodle.elementalarcana.content.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.chappadodle.elementalarcana.api.ForgewardenRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;

/**
 * A Cinder Forge's heart (see the Cinder Forges spec), under the anvil's dais. The first time
 * someone (not a spectator) comes within 24 blocks, the forge's keeper stirs: the Forgewarden rises
 * out of the hall's floor, south of the anvil. It comes once; a forge cleared stays cleared.
 */
public class ForgeHeartBlockEntity extends BlockEntity {
    private boolean awake;

    public ForgeHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModForge.FORGE_HEART_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ForgeHeartBlockEntity heart) {
        if (heart.awake || level.getGameTime() % 20 != 0 || !(level instanceof ServerLevel server)
                || server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ForgewardenRules.WAKE_RADIUS,
                EntitySelector.NO_SPECTATORS) == null) {
            return;
        }
        heart.awake = true;
        heart.setChanged();
        heart.wake(server);
    }

    /** The Forgewarden rises out of the hall's floor eight blocks south of the anvil (past the lava ring), in fire. */
    private void wake(ServerLevel level) {
        BlockPos at = worldPosition.above().south(8);
        ForgewardenEntity warden = ModForge.FORGEWARDEN.get().create(level);
        if (warden == null) {
            return;
        }
        warden.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
        warden.guard(worldPosition);
        EventHooks.finalizeMobSpawn(warden, level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        level.addFreshEntity(warden);
        level.sendParticles(ParticleTypes.LAVA, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 40, 1.2, 0.4, 1.2, 0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 30, 1, 1, 1, 0.02);
        level.playSound(null, at, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.4f);
        level.playSound(null, at, SoundEvents.LAVA_POP, SoundSource.HOSTILE, 2f, 0.6f);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5) < 48 * 48) {
                player.sendSystemMessage(Component.translatable("message.elementalarcana.forge.wakes").withStyle(ChatFormatting.GOLD));
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("awake", awake);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        awake = tag.getBoolean("awake");
    }
}
