package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class MagicEvents {
    private static final int TICKS_TO_MEDITATE = 40;
    private static final float MEDITATION_MULTIPLIER = 3f;
    private static final float SICKNESS_MULTIPLIER = 0.5f;

    private MagicEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) {
            return;
        }
        CastingService.tickHold(player);
        Conjuring.tick(player);
        MagicData data = MagicAttachments.get(player);
        boolean changed = data.updateMeditation(player.getX(), player.getZ(), player.isShiftKeyDown(), player.onGround(), TICKS_TO_MEDITATE);

        float regen = data.regenPerSecond() / 20f;
        if (data.meditating()) {
            regen *= MEDITATION_MULTIPLIER;
            if (player.tickCount % 8 == 0) {
                float angle = player.tickCount * 0.3f;
                player.serverLevel().sendParticles(ParticleTypes.ENCHANT,
                        player.getX() + Mth.cos(angle) * 0.8, player.getY() + 1.2, player.getZ() + Mth.sin(angle) * 0.8,
                        3, 0.1, 0.3, 0.1, 0.4);
            }
        }
        if (player.hasEffect(ModContent.MANA_SICKNESS)) {
            regen *= SICKNESS_MULTIPLIER;
        }
        changed |= data.regenerate(regen);

        // Landing in water or starting to fly also ends a dash; LivingFallEvent never fires for those.
        if (data.fallImmune() && (player.isInWater() || player.getAbilities().flying)) {
            data.setFallImmune(false);
        }
        if (changed) {
            MagicAttachments.sync(player);
        }
    }

    // A full night's sleep restores all mana. Skipping the night wakes players with wakeImmediately = false.
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (!event.wakeImmediately() && event.getEntity() instanceof ServerPlayer player) {
            MagicAttachments.get(player).fillMana();
            MagicAttachments.sync(player);
        }
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagicData data = MagicAttachments.get(player);
            if (data.fallImmune()) {
                data.setFallImmune(false);
                event.setCanceled(true);
            }
        }
    }

    // Synced player attachments aren't always re-sent on join/respawn/dimension change in 1.21.1
    // (NeoForge issue #2510), so push the data explicitly.
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncIfServer(event);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncIfServer(event);
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CastingService.cancelHold(player);
        }
        syncIfServer(event);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CastingService.cancelHold(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CastingService.forget(player);
        }
    }

    private static void syncIfServer(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerStats.apply(player);
            MagicAttachments.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ArcanaCommand.register(event.getDispatcher());
    }
}
