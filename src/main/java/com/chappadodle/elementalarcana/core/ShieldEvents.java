package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.ShieldSpell;
import com.chappadodle.elementalarcana.api.SpellShield;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runs every {@link SpellShield}: absorbing damage, expiring, and calling its spell's hooks. */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ShieldEvents {
    // The shield each player had when last hit, so a death on that same tick can still be prevented
    // even though the killing blow just broke the shield.
    private static final Map<UUID, ShieldedHit> LAST_SHIELDED_HIT = new HashMap<>();

    private record ShieldedHit(SpellShield shield, long gameTime) {
    }

    private ShieldEvents() {
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (event.getEntity() instanceof ServerPlayer player) {
            SpellShield shield = SpellShield.of(player);
            ShieldSpell spell = shield.shieldSpell();
            if (shield.isActive() && spell != null) {
                LAST_SHIELDED_HIT.put(player.getUUID(), new ShieldedHit(shield.copy(), player.level().getGameTime()));
                if (spell.blocksHit(player, shield, source, event.getAmount())) {
                    event.setCanceled(true);
                    return;
                }
                float absorbed = Math.min(event.getAmount(), shield.amount());
                shield.setAmount(shield.amount() - absorbed);
                event.setAmount(event.getAmount() - absorbed);
                spell.onShieldHit(player, shield, source, absorbed);
                if (shield.amount() <= 0) {
                    SpellShield.end(player, true);
                } else {
                    SpellShield.sync(player);
                }
            }
        }
        // A shielded player hitting something in melee.
        if (source.getEntity() instanceof ServerPlayer attacker && source.getDirectEntity() == attacker && event.getEntity() != attacker) {
            SpellShield shield = SpellShield.of(attacker);
            ShieldSpell spell = shield.shieldSpell();
            if (shield.isActive() && spell != null) {
                spell.onOwnerAttack(attacker, shield, event.getEntity());
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof ServerPlayer player) {
            SpellShield shield = SpellShield.of(player);
            ShieldSpell spell = shield.shieldSpell();
            if (shield.isActive() && spell != null && spell.onProjectile(player, shield, event.getProjectile())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SpellShield shield = SpellShield.of(player);
        if (!shield.isActive()) {
            return;
        }
        if (player.level().getGameTime() >= shield.endsAt()) {
            SpellShield.end(player, false);
            return;
        }
        ShieldSpell spell = shield.shieldSpell();
        if (spell != null) {
            spell.onShieldTick(player, shield);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SpellShield shield = SpellShield.of(player);
        if (!shield.isActive()) {
            ShieldedHit last = LAST_SHIELDED_HIT.get(player.getUUID());
            if (last == null || last.gameTime() != player.level().getGameTime()) {
                return;
            }
            shield = last.shield();
        }
        ShieldSpell spell = shield.shieldSpell();
        if (spell != null && spell.preventDeath(player, shield)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SHIELDED_HIT.remove(event.getEntity().getUUID());
    }
}
