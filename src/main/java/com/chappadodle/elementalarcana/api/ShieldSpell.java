package com.chappadodle.elementalarcana.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by spells that raise a {@link SpellShield}. The framework handles the shield
 * itself: absorbing damage, expiring, ice hearts in the HUD, orbiting shards and syncing.
 * These hooks add the spell's own behavior. All hooks run server-side unless noted.
 */
public interface ShieldSpell {

    /** The shield was just raised (or refreshed) on {@code player}. */
    default void onShieldRaised(ServerPlayer player, SpellShield shield) {
    }

    /** Before the shield absorbs a hit. Return true to cancel the damage entirely (a perfect block, an immunity...). */
    default boolean blocksHit(ServerPlayer player, SpellShield shield, DamageSource source, float amount) {
        return false;
    }

    /** The shield just absorbed {@code absorbed} damage from a hit. */
    default void onShieldHit(ServerPlayer player, SpellShield shield, DamageSource source, float absorbed) {
    }

    /** A projectile is about to hit the shielded player. Return true to stop the impact. */
    default boolean onProjectile(ServerPlayer player, SpellShield shield, Projectile projectile) {
        return false;
    }

    /** The shielded player just hit {@code target} in melee. */
    default void onOwnerAttack(ServerPlayer player, SpellShield shield, LivingEntity target) {
    }

    /** Every tick while the shield is up. */
    default void onShieldTick(ServerPlayer player, SpellShield shield) {
    }

    /** The shield ended. {@code broken} = destroyed by damage, rather than running out or being replaced. */
    default void onShieldEnd(ServerPlayer player, SpellShield shield, boolean broken) {
    }

    /** The shielded player (or one whose shield just broke) is about to die. Return true to prevent it. */
    default boolean preventDeath(ServerPlayer player, SpellShield shield) {
        return false;
    }

    /** Model drawn as shards orbiting the shielded player, same format as {@link ProjectileSpell#model()}. Null = none. */
    @Nullable
    default ResourceLocation shardModel() {
        return null;
    }

    /** Shards orbiting at full strength; they break off as the shield weakens. */
    default int shardCount() {
        return 6;
    }

    /** Client-side, every tick while the shield is up (for anyone the player is visible to). */
    default void shieldParticles(Player player, SpellShield shield) {
    }
}
