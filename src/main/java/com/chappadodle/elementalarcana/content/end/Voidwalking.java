package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.FarIslesRules;
import com.chappadodle.elementalarcana.content.wild.WildCharms;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Voidwalker's Charm (see the Far Isles spec): carried (or in the Charm Pouch), a fall into the
 * void carries its bearer back to the last ground they stood on, with Slow Falling, once every five
 * minutes. The last ground is noted as they walk (in memory only: a fall right after logging in
 * finds none, and isn't saved).
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Voidwalking {
    private static final String LAST_USED = ElementalArcana.MODID + ":voidwalked_at";
    private static final Map<UUID, Ground> LAST_GROUND = new HashMap<>();

    private record Ground(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private Voidwalking() {
    }

    /** Makes the charm ready again (for tests: /arcana cooldowns reset). */
    public static void ready(ServerPlayer player) {
        player.getPersistentData().remove(LAST_USED);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0 || player.isSpectator() || !player.onGround()) {
            return;
        }
        BlockPos feet = player.blockPosition();
        if (player.level().getBlockState(feet.below()).isFaceSturdy(player.level(), feet.below(), Direction.UP)) {
            LAST_GROUND.put(player.getUUID(), new Ground(player.level().dimension(), feet));
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_GROUND.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)
                || !WildCharms.carries(player, ModEnd.VOIDWALKER_CHARM.get())) {
            return;
        }
        ServerLevel level = player.serverLevel();
        long lastUsed = player.getPersistentData().contains(LAST_USED) ? player.getPersistentData().getLong(LAST_USED) : -1;
        Ground ground = LAST_GROUND.get(player.getUUID());
        if (ground == null || ground.dimension() != level.dimension() || !FarIslesRules.voidwalkReady(level.getGameTime(), lastUsed)) {
            return;
        }
        event.setCanceled(true);
        player.getPersistentData().putLong(LAST_USED, level.getGameTime());
        Vec3 from = player.position();
        Vec3 to = Vec3.atBottomCenterOf(ground.pos());
        player.teleportTo(to.x, to.y, to.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, FarIslesRules.VOIDWALK_SLOW_FALL_TICKS));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, from.x, from.y, from.z, 30, 0.4, 1.0, 0.4, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, to.x, to.y + 1, to.z, 24, 0.4, 0.8, 0.4, 0.04);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.7f);
        player.displayClientMessage(Component.translatable("message.elementalarcana.voidwalker.saved").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
