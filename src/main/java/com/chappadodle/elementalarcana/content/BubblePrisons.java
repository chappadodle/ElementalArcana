package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Bubble;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Bubble Prison: a creature (or player) floats helpless in a water bubble for a few seconds. Any
 * hit from a player pops it for bonus damage. Mobs have their AI switched off while trapped (a saved
 * marker turns it back on after a restart); a trapped player's own client holds them in place
 * (see ArcanaClient), the server pulls them back if they drift and blocks their actions.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class BubblePrisons {
    public static final int DURATION_TICKS = 80;
    private static final float POP_BONUS = 4f;
    private static final double MAX_DRIFT = 1.5;
    // Saved on mobs whose AI we switched off, so a restart mid-bubble can't leave them frozen.
    private static final String TAG_AI_OFF = "ea_bubble_ai_off";

    private BubblePrisons() {
    }

    public static boolean isTrapped(Entity entity) {
        return entity.hasData(MagicAttachments.BUBBLE);
    }

    /** Whether {@code target} can be put in a bubble: not a boss, not already trapped, no creative/spectator players. */
    public static boolean canTrap(LivingEntity target) {
        if (!target.isAlive() || isTrapped(target) || target.getType().is(Tags.EntityTypes.BOSSES)) {
            return false;
        }
        return !(target instanceof Player player) || !(player.isCreative() || player.isSpectator());
    }

    public static void trap(LivingEntity target) {
        ServerLevel level = (ServerLevel) target.level();
        long now = level.getGameTime();
        target.setData(MagicAttachments.BUBBLE, new Bubble(target.position(), now, now + DURATION_TICKS));
        // A burning target Vaporizes (the fire goes out in a burst of steam), but inside a ball of
        // water it's Wet either way, so Freeze combos always work.
        ElementalReactions.waterHit(target, DURATION_TICKS + 40);
        target.clearFire();
        target.addEffect(new MobEffectInstance(ModContent.WET, DURATION_TICKS + 40));
        if (target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            mob.getPersistentData().putBoolean(TAG_AI_OFF, true);
        }
        target.setDeltaMovement(Vec3.ZERO);
        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.5), target.getZ(), 12, 0.4, 0.5, 0.4, 0.1);
        // A bloop as it closes round them (its gurgle while it holds is each client's, BubbleRenderer).
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.PLAYERS, 1f, 1.2f);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.9f, 0.6f);
    }

    /** Ends the bubble: the creature drops (and a mob gets its AI back). */
    public static void release(LivingEntity target, boolean popped) {
        target.removeData(MagicAttachments.BUBBLE);
        if (target instanceof Mob mob && mob.getPersistentData().getBoolean(TAG_AI_OFF)) {
            mob.setNoAi(false);
            mob.getPersistentData().remove(TAG_AI_OFF);
        }
        target.resetFallDistance();
        if (target.level() instanceof ServerLevel level) {
            // The burst itself is drawn by each client (BubbleRenderer, BubblePrisonEffects#pop).
            // A pop and a splash: a full splash when a hit bursts it, a light one when it runs out.
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS,
                    popped ? 1.2f : 1f, popped ? 0.8f : 1f);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), popped ? SoundEvents.GENERIC_SPLASH : SoundEvents.PLAYER_SPLASH,
                    SoundSource.PLAYERS, popped ? 1f : 0.5f, popped ? 1.1f : 1.5f);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity target) || !(target.level() instanceof ServerLevel level) || !isTrapped(target)) {
            return;
        }
        Bubble bubble = target.getData(MagicAttachments.BUBBLE);
        long now = level.getGameTime();
        if (now >= bubble.endsAt()) {
            release(target, false);
            return;
        }
        Vec3 hold = bubble.holdPoint(now);
        if (target instanceof ServerPlayer player) {
            // The player's own client holds them; only step in if they got away somehow.
            if (player.position().distanceTo(hold) > MAX_DRIFT) {
                player.connection.teleport(hold.x, hold.y, hold.z, player.getYRot(), player.getXRot());
            }
        } else {
            target.setPos(hold);
            target.setDeltaMovement(Vec3.ZERO);
        }
        target.resetFallDistance();
        // Nothing burns inside a ball of water, whatever tries to set it alight.
        if (target.isOnFire()) {
            target.clearFire();
        }
        if (now % 4 == 0) {
            double width = target.getBbWidth() * 0.6;
            level.sendParticles(ParticleTypes.FALLING_WATER, target.getX(), target.getY(), target.getZ(), 1, width * 0.6, 0, width * 0.6, 0);
        }
    }

    /** Any hit from a player pops the bubble for bonus damage. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide() || !isTrapped(target) || !(event.getSource().getEntity() instanceof Player)) {
            return;
        }
        event.setAmount(event.getAmount() + POP_BONUS);
        release(target, true);
    }

    /** A mob whose AI we switched off gets it back if it was saved mid-bubble (the bubble itself isn't saved). */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob
                && mob.getPersistentData().getBoolean(TAG_AI_OFF) && !isTrapped(mob)) {
            mob.setNoAi(false);
            mob.getPersistentData().remove(TAG_AI_OFF);
        }
    }

    // A trapped player can't attack, use items or interact with blocks and entities.

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        cancelIfTrapped(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelIfTrapped(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelIfTrapped(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        cancelIfTrapped(event.getEntity(), event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelIfTrapped(event.getEntity(), event);
    }

    private static void cancelIfTrapped(Player player, ICancellableEvent event) {
        if (isTrapped(player)) {
            event.setCanceled(true);
        }
    }
}
