package com.chappadodle.elementalarcana.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public final class SpellDamage {

    private SpellDamage() {
    }

    /**
     * Damages {@code target} even if it was hit a moment ago. After any hit, Minecraft ignores
     * further damage for 10 ticks (unless it's bigger, and then only the difference counts), so
     * several projectiles from one volley would otherwise land as a single hit.
     */
    public static boolean hurtMultiHit(Entity target, DamageSource source, float amount) {
        target.invulnerableTime = 0;
        return target.hurt(source, amount);
    }
}
