package com.chappadodle.elementalarcana.api;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    /** The model for a projectile of this look (see SpellProjectile#variant). Defaults to {@link #model()}. */
    @Nullable
    default ResourceLocation model(int variant) {
        return model();
    }

    /** Every model this spell's projectiles can use, so they all get loaded. */
    default List<ResourceLocation> models() {
        return model() == null ? List.of() : List.of(model());
    }

    /** The glow halo around a projectile of this look, or null for none. */
    @Nullable
    default Glow glow(int variant) {
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

    /**
     * Where a held projectile floats, relative to the caster's eyes (x = right, y = up,
     * z = forward). {@code slot} is its index among the {@code count} projectiles held together.
     */
    default Vec3 holdOffset(int slot, int count) {
        return new Vec3(0.55, 0.15, 0.9);
    }

    // ---- client-side visuals ----

    /**
     * How much light (0-15) the projectile gives off when a dynamic lights mod is installed
     * (LambDynamicLights or Sodium Dynamic Lights): it lights up what's around it as it flies.
     */
    default int luminance(SpellProjectile projectile) {
        return 0;
    }

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

    /** On the client, the first tick after a held projectile is thrown (a burst at the hand). */
    default void releaseParticles(SpellProjectile projectile) {
    }

    /** On the client, the tick a held projectile becomes fully grown. */
    default void grownParticles(SpellProjectile projectile) {
    }

    // ---- server-side hooks ----

    /** A held projectile has just been thrown (after any release delay). */
    default void onRelease(SpellProjectile projectile) {
    }

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
