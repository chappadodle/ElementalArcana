package com.chappadodle.elementalarcana.api;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by spells that fire a {@link SpellProjectile}. The projectile entity is shared
 * by every projectile spell; it calls back into these hooks, so a new projectile spell never
 * needs its own entity type or renderer.
 */
public interface ProjectileSpell {
    ParticleOptions trailParticle();

    default float projectileSpeed() {
        return 1.6f;
    }

    /** Ticks of flight before the projectile fizzles out. */
    default int lifetimeTicks() {
        return 60;
    }

    /**
     * Optional 3D model drawn for the projectile, as a model id: {@code yourmod:spell/icicle}
     * loads {@code assets/yourmod/models/spell/icicle.json}. The model points along +Z. Its
     * textures must be in the block atlas (under {@code textures/block/}). Null = particles only.
     */
    @Nullable
    default ResourceLocation model() {
        return null;
    }

    // ---- held projectiles (see SpellProjectile#summonHeld) ----

    /** Ticks a held projectile takes to reach full charge; 0 = no charging. */
    default int chargeTicks() {
        return 0;
    }

    /** Launch speed for a held projectile released at {@code charge} (0..1). */
    default float releaseSpeed(float charge) {
        return projectileSpeed();
    }

    /** Where a held projectile floats, relative to the caster's eyes: x = right, y = up, z = forward. */
    default Vec3 holdOffset() {
        return new Vec3(0.55, 0.15, 0.9);
    }

    // ---- client-side visuals ----

    /** Every tick while held, on the client. {@code charge} runs 0..1. */
    default void heldParticles(SpellProjectile projectile, float charge) {
        if (projectile.tickCount % 3 == 0) {
            projectile.spawnParticleAround(trailParticle(), 0.3, Vec3.ZERO);
        }
    }

    /** Every tick in flight, on the client. */
    default void flightParticles(SpellProjectile projectile) {
        for (int i = 0; i < 3; i++) {
            projectile.spawnParticleAround(trailParticle(), 0.1, Vec3.ZERO);
        }
    }

    // ---- server-side hooks ----

    /** Every tick in flight (not while held). Discard the projectile here to end it early. */
    default void onTick(SpellProjectile projectile) {
    }

    /** The projectile is discarded right after. */
    default void onHitEntity(SpellProjectile projectile, EntityHitResult hit) {
    }

    /** The projectile is discarded right after. */
    default void onHitBlock(SpellProjectile projectile, BlockHitResult hit) {
    }
}
