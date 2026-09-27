package com.chappadodle.elementalarcana.api;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

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

    default int lifetimeTicks() {
        return 60;
    }

    /** Server-side, every tick in flight. Discard the projectile here to end it early. */
    default void onTick(SpellProjectile projectile) {
    }

    /** Server-side. The projectile is discarded right after. */
    default void onHitEntity(SpellProjectile projectile, EntityHitResult hit) {
    }

    /** Server-side. The projectile is discarded right after. */
    default void onHitBlock(SpellProjectile projectile, BlockHitResult hit) {
    }
}
