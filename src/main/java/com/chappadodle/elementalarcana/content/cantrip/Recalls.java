package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CantripRules;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Recall's channel (see the Cantrips spec): motes swirl up round the caster for four seconds; if they
 * stand still and nothing hurts them, they're taken to their bed or respawn anchor (its charge left
 * alone), or else to the world's spawn. Moving or being hurt breaks it and gives the cooldown back.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Recalls {
    private record Channel(Vec3 start, long endsAt, ResourceLocation spell) {
    }

    private static final Map<UUID, Channel> CHANNELS = new HashMap<>();

    private Recalls() {
    }

    public static boolean isRecalling(ServerPlayer player) {
        return CHANNELS.containsKey(player.getUUID());
    }

    public static void begin(ServerPlayer player, Spell spell) {
        CHANNELS.put(player.getUUID(), new Channel(player.position(), player.level().getGameTime() + CantripRules.RECALL_CHANNEL_TICKS, spell.id()));
        player.displayClientMessage(Component.translatable("message.elementalarcana.cantrip.recall_start"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, 1.5f, 1.4f);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Channel channel = CHANNELS.get(player.getUUID());
        if (channel == null) {
            return;
        }
        if (!player.isAlive() || CantripRules.recallBroken(player.position().distanceToSqr(channel.start()), false)) {
            interrupt(player, channel);
            return;
        }
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        if (now >= channel.endsAt()) {
            CHANNELS.remove(player.getUUID());
            arrive(player);
            return;
        }
        // Two motes spiralling up round the caster, faster as it nears.
        float progress = 1f - (channel.endsAt() - now) / (float) CantripRules.RECALL_CHANNEL_TICKS;
        for (int i = 0; i < 2; i++) {
            float angle = now * (0.3f + progress * 0.4f) + i * Mth.PI;
            double y = player.getY() + (now % 20) / 20.0 * 2.0;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX() + Mth.cos(angle) * 0.8, y, player.getZ() + Mth.sin(angle) * 0.8,
                    1, 0, 0, 0, 0);
        }
        if (now % 10 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY(1.0), player.getZ(), 6, 0.4, 0.6, 0.4, 0.4);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Channel channel = CHANNELS.get(player.getUUID());
            if (channel != null) {
                interrupt(player, channel);
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHANNELS.remove(event.getEntity().getUUID());
    }

    /** Broken: the cooldown comes back (the mana is spent). */
    private static void interrupt(ServerPlayer player, Channel channel) {
        CHANNELS.remove(player.getUUID());
        MagicAttachments.get(player).reduceCooldown(channel.spell(), player.level().getGameTime(), 1f);
        MagicAttachments.sync(player);
        player.displayClientMessage(Component.translatable("message.elementalarcana.cantrip.recall_broken"), true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.4f);
    }

    /** Home: the bed or anchor the player last slept or set their spawn at, else the world's spawn. */
    private static void arrive(ServerPlayer player) {
        ServerLevel from = player.serverLevel();
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1.0), player.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        from.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f);
        ServerLevel home = player.server.getLevel(player.getRespawnDimension());
        BlockPos respawn = player.getRespawnPosition();
        Optional<Vec3> spot = Optional.empty();
        if (home != null && respawn != null) {
            BlockState state = home.getBlockState(respawn);
            if (state.getBlock() instanceof BedBlock) {
                spot = BedBlock.findStandUpPosition(EntityType.PLAYER, home, respawn, state.getValue(BedBlock.FACING), player.getRespawnAngle());
            } else if (state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) > 0) {
                spot = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, home, respawn);
            }
        }
        if (spot.isEmpty()) {
            home = player.server.overworld();
            BlockPos spawn = home.getSharedSpawnPos();
            spot = Optional.of(new Vec3(spawn.getX() + 0.5, home.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ()),
                    spawn.getZ() + 0.5));
        }
        Vec3 to = spot.get();
        player.teleportTo(home, to.x, to.y, to.z, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        home.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 40, 0.4, 0.8, 0.4, 0.1);
        home.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
    }
}
