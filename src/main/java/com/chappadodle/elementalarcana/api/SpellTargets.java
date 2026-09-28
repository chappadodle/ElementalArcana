package com.chappadodle.elementalarcana.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Who a spell's area effects (splash, burning ground, whirlpools, bursts) may hurt, given who cast
 * it. A player's magic spares other players (they can still be hit directly by a projectile), and
 * a monster's magic spares other monsters. The caster is never affected.
 */
public final class SpellTargets {

    private SpellTargets() {
    }

    public static boolean canAffect(@Nullable Entity owner, LivingEntity target) {
        if (target == owner || !target.isAlive()) {
            return false;
        }
        if (owner instanceof Enemy) {
            return !(target instanceof Enemy);
        }
        return !(target instanceof Player);
    }
}
