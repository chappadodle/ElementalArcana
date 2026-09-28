package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * A spell an Attuned creature can cast. Mobs don't use the player spell classes; these reuse their
 * projectiles and effects instead. Cast by CastMobSpellGoal after a short wind-up.
 */
public interface MobSpell {

    /** The lowest rank that knows this spell. */
    AttunementRank rank();

    int cooldownTicks();

    /** Whether casting now makes sense (range, situation). The target is alive and in sight. */
    boolean canCast(Mob caster, LivingEntity target);

    /** Runs when the wind-up ends. */
    void cast(Mob caster, LivingEntity target);
}
