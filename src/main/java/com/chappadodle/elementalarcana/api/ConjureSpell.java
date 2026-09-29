package com.chappadodle.elementalarcana.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * A projectile spell you conjure one projectile at a time (see core/Conjuring). Each press of the
 * cast key conjures one more, up to {@link #maxConjured}; they hover and grow while held (costing
 * upkeep), and "launch one" or "launch all" throws them. The spell only says how to make, throw
 * and fuse its projectiles; counting, mana, upkeep and cooldown are handled for it.
 *
 * <p>Implemented by a {@link Spell} that is also a {@link ProjectileSpell}. Its
 * {@link Spell#cast} is never called: casting goes through conjuring.
 */
public interface ConjureSpell {

    /** The most projectiles a caster can hold at {@code spellLevel}. */
    int maxConjured(int spellLevel);

    /** Mana to conjure one more when {@code alreadyHeld} are held (0 = the first of a set). */
    int conjureCost(int spellLevel, int alreadyHeld);

    /**
     * Creates one held projectile (see {@link SpellProjectile#summonHeld}) with whatever the spell
     * stores on it (level, branches...). {@code seed} is the same for every projectile of one set.
     * Its formation slot is set by the caller afterwards.
     */
    SpellProjectile conjure(CastContext context, int seed);

    /** Throws {@code projectiles} at {@code aim}. Override for special timing (e.g. a ripple). */
    default void launch(ServerPlayer caster, List<SpellProjectile> projectiles, Vec3 aim) {
        for (SpellProjectile projectile : projectiles) {
            projectile.release(aim);
        }
    }

    /** Whether a full, fully grown set can fuse (holding the cast key for a second). */
    default boolean canFuse(int spellLevel, Map<Integer, String> branches) {
        return false;
    }

    /**
     * Fuses the held set into one projectile, discarding the others, and places it (its formation
     * slot); returns the one that's left.
     */
    default SpellProjectile fuse(ServerPlayer caster, List<SpellProjectile> projectiles) {
        throw new UnsupportedOperationException("This spell doesn't fuse");
    }

    /** A held projectile just finished growing (a chime, a sparkle). */
    default void onFullyGrown(SpellProjectile projectile) {
    }

    /** A held projectile fizzles out without being thrown (death, logout...). Default: it vanishes. */
    default void fizzle(SpellProjectile projectile) {
        projectile.discard();
    }
}
