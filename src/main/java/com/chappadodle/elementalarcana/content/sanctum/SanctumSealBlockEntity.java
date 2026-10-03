package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Runs a sanctum's Seal (see the sanctums spec). Sealed: when someone (not a spectator) comes
 * within 10 blocks, the Sovereign rises out of it, everyone near sees its name, and the seal is
 * awake. When the Sovereign falls the seal is restored ({@link #restore}), for good. A Sovereign that
 * vanishes without falling (removed by a command) leaves the seal sealed again after half a minute.
 */
public class SanctumSealBlockEntity extends BlockEntity {
    private static final double WAKE_RADIUS = 10;
    private static final double NEWS_RADIUS = 48;
    private static final int MISSING_CHECKS = 30;

    @Nullable
    private UUID sovereign;
    private int missing;

    public SanctumSealBlockEntity(BlockPos pos, BlockState state) {
        super(ModSanctums.SANCTUM_SEAL_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SanctumSealBlockEntity seal) {
        if (level.getGameTime() % 20 != 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        switch (state.getValue(SanctumSealBlock.STATE)) {
            case SEALED -> {
                if (server.getDifficulty() != Difficulty.PEACEFUL && server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        WAKE_RADIUS, EntitySelector.NO_SPECTATORS) != null) {
                    seal.wake(server, state);
                }
            }
            case AWAKE -> {
                if (seal.sovereign != null && server.getEntity(seal.sovereign) != null) {
                    seal.missing = 0;
                } else if (++seal.missing >= MISSING_CHECKS) {
                    seal.sovereign = null;
                    seal.missing = 0;
                    seal.setChanged();
                    server.setBlockAndUpdate(pos, state.setValue(SanctumSealBlock.STATE, SealState.SEALED));
                }
            }
            case RESTORED -> {
            }
        }
    }

    private void wake(ServerLevel level, BlockState state) {
        Element element = state.getValue(SanctumSealBlock.KIND).element();
        if (!SovereignRules.hasSovereign(element)) {
            return;
        }
        SovereignEntity rising = ModSanctums.sovereign(element).create(level);
        if (rising == null) {
            return;
        }
        BlockPos at = worldPosition.above(2);
        rising.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
        rising.bindTo(worldPosition);
        EventHooks.finalizeMobSpawn(rising, level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        level.addFreshEntity(rising);
        sovereign = rising.getUUID();
        missing = 0;
        setChanged();
        level.setBlockAndUpdate(worldPosition, state.setValue(SanctumSealBlock.STATE, SealState.AWAKE));

        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1;
        double z = worldPosition.getZ() + 0.5;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, x, y + 1.5, z, 120, 0.6, 1.6, 0.6, 0.15);
        level.sendParticles(ParticleTypes.FLASH, x, y + 1.5, z, 1, 0, 0, 0, 0);
        level.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2f, 0.9f);
        // Its name, for everyone near.
        Component title = rising.getDisplayName().copy().withColor(element.color());
        Component subtitle = Component.translatable("title.elementalarcana.sovereign." + element.name().toLowerCase(Locale.ROOT));
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(NEWS_RADIUS))) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    /** Seals it again (for tests and operators: /arcana sanctum reset): its Sovereign, if any, is gone. */
    public void reset() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (sovereign != null && server.getEntity(sovereign) instanceof SovereignEntity rising) {
            rising.discard();
        }
        sovereign = null;
        missing = 0;
        setChanged();
        server.setBlockAndUpdate(worldPosition, getBlockState().setValue(SanctumSealBlock.STATE, SealState.SEALED));
    }

    /** The Sovereign fell: the seal holds again, for good. */
    public void restore() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(SanctumSealBlock.STATE) || state.getValue(SanctumSealBlock.STATE) == SealState.RESTORED) {
            return;
        }
        sovereign = null;
        setChanged();
        server.setBlockAndUpdate(worldPosition, state.setValue(SanctumSealBlock.STATE, SealState.RESTORED));
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1;
        double z = worldPosition.getZ() + 0.5;
        server.sendParticles(ParticleTypes.END_ROD, x, y, z, 100, 0.4, 2.5, 0.4, 0.2);
        server.sendParticles(ParticleTypes.FLASH, x, y, z, 1, 0, 0, 0, 0);
        server.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 0.7f);
        server.playSound(null, worldPosition, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1f, 1f);
        Element element = state.getValue(SanctumSealBlock.KIND).element();
        Component message = Component.translatable("message.elementalarcana.sanctum.restored." + element.name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        server.getEntitiesOfClass(ServerPlayer.class, new AABB(worldPosition).inflate(NEWS_RADIUS)).forEach(player -> player.sendSystemMessage(message));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (sovereign != null) {
            tag.putUUID("sovereign", sovereign);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sovereign = tag.hasUUID("sovereign") ? tag.getUUID("sovereign") : null;
    }
}
