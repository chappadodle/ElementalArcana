package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Cross-element reactions. An entity's "aura" is the element it currently carries (frosted over
 * or frozen = cryo, burning = pyro); reactions read and spread those auras.
 */
public final class ElementalReactions {
    private static final double SWIRL_RADIUS = 4.0;
    private static final float SWIRL_DAMAGE = 2f;
    private static final float MELT_MULTIPLIER = 1.75f;

    /** Elements an entity can carry, with the color their reactions show in. */
    public enum Aura {
        CRYO(0x9EE6FF),
        PYRO(0xFF7A1F);

        private final int color;

        Aura(int color) {
            this.color = color;
        }

        public int color() {
            return color;
        }
    }

    private ElementalReactions() {
    }

    @Nullable
    public static Aura auraOf(LivingEntity entity) {
        if (entity.hasEffect(ModContent.FROZEN) || entity.getTicksFrozen() > 0) {
            return Aura.CRYO;
        }
        if (entity.isOnFire()) {
            return Aura.PYRO;
        }
        return null;
    }

    /**
     * Swirl: if {@code target} carries an element, wind spreads it to every other creature within
     * 4 blocks (not players) with a small bonus hit, and a ring of that element's color. Returns
     * whether anything swirled.
     */
    public static boolean swirl(LivingEntity target, @Nullable Entity attacker, float power) {
        Aura aura = auraOf(target);
        if (aura == null || !(target.level() instanceof ServerLevel level)) {
            return false;
        }
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SWIRL_RADIUS),
                e -> e != target && e != attacker && !(e instanceof Player) && e.isAlive() && e.distanceTo(target) <= SWIRL_RADIUS)) {
            switch (aura) {
                case CRYO -> {
                    nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    if (nearby.canFreeze()) {
                        nearby.setTicksFrozen(Math.max(nearby.getTicksFrozen(), nearby.getTicksRequiredToFreeze() + 40));
                    }
                    SpellDamage.hurtMultiHit(nearby, level.damageSources().freeze(), SWIRL_DAMAGE * power);
                }
                case PYRO -> {
                    nearby.igniteForTicks(80);
                    SpellDamage.hurtMultiHit(nearby, level.damageSources().inFire(), SWIRL_DAMAGE * power);
                }
            }
        }
        ColorParticleOption color = ColorParticleOption.create(ModContent.SWIRL.get(), aura.color());
        for (int i = 0; i < 32; i++) {
            float angle = i * Mth.TWO_PI / 32;
            double dx = Mth.cos(angle);
            double dz = Mth.sin(angle);
            // Count 0 = a directional particle: the offsets become its velocity, flung outward in a ring.
            level.sendParticles(color, target.getX() + dx * 0.5, target.getY(0.5), target.getZ() + dz * 0.5,
                    0, dx - dz * 0.6, 0.05, dz + dx * 0.6, 0.35);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8f, 1.3f);
        return true;
    }

    /**
     * Melt: fire hitting a frozen or frosted target thaws it in a burst of steam. Returns the
     * damage multiplier for the hit (1.75, or 1 when there was no ice to melt).
     */
    public static float melt(LivingEntity target) {
        if (auraOf(target) != Aura.CRYO || !(target.level() instanceof ServerLevel level)) {
            return 1f;
        }
        target.removeEffect(ModContent.FROZEN);
        target.setTicksFrozen(0);
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.6), target.getZ(), 14, 0.4, 0.4, 0.4, 0.06);
        level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1f, 1.1f);
        return MELT_MULTIPLIER;
    }

    /** Launches {@code target} into the air as Airborne: +25% damage from everything until it lands (see AirborneEvents). */
    public static void launchAirborne(LivingEntity target, double upward) {
        target.setDeltaMovement(target.getDeltaMovement().x, Math.max(target.getDeltaMovement().y, upward), target.getDeltaMovement().z);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(ModContent.AIRBORNE, 60));
    }
}
