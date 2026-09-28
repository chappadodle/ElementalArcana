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
 * or frozen = cryo, burning = pyro, wet or standing in water or rain = hydro); reactions read and
 * spread those auras.
 *
 * <pre>
 * Melt      fire on cryo         x1.75, thaws it
 * Vaporize  water on pyro        x1.5, puts the fire out
 *           fire on hydro        x1.5, dries it off
 * Freeze    ice on hydro,        frozen solid for 2.5s
 *           water on cryo
 * Swirl     wind on any aura     spreads it around
 * </pre>
 */
public final class ElementalReactions {
    private static final double SWIRL_RADIUS = 4.0;
    private static final float SWIRL_DAMAGE = 2f;
    private static final float MELT_MULTIPLIER = 1.75f;
    private static final float VAPORIZE_MULTIPLIER = 1.5f;
    private static final int FREEZE_TICKS = 50;
    // After a Freeze wears off, the target can't be re-frozen by a reaction for a few seconds.
    private static final int FREEZE_IMMUNITY_TICKS = 60;
    private static final String TAG_FREEZE_IMMUNE_UNTIL = "ea_freeze_immune_until";

    /** Elements an entity can carry, with the color their reactions show in. */
    public enum Aura {
        CRYO(0x9EE6FF),
        PYRO(0xFF7A1F),
        HYDRO(0x3F9CFF);

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
        if (entity.hasEffect(ModContent.WET) || entity.isInWaterRainOrBubble()) {
            return Aura.HYDRO;
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
                case HYDRO -> {
                    nearby.addEffect(new MobEffectInstance(ModContent.WET, 100));
                    SpellDamage.hurtMultiHit(nearby, level.damageSources().magic(), SWIRL_DAMAGE * power);
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

    /**
     * A fire hit's reaction: Melt on a frozen or frosted target, Vaporize on a wet one. Returns the
     * damage multiplier (1 when there was nothing to react with).
     */
    public static float fireHit(LivingEntity target) {
        return switch (auraOf(target)) {
            case CRYO -> melt(target);
            case HYDRO -> vaporize(target);
            case null, default -> 1f;
        };
    }

    /**
     * A water hit: puts out a burning target (Vaporize), freezes a frosted one (Freeze), and
     * otherwise soaks it for {@code wetTicks}. Returns the damage multiplier for the hit.
     */
    public static float waterHit(LivingEntity target, int wetTicks) {
        if (!(target.level() instanceof ServerLevel)) {
            return 1f;
        }
        Aura aura = auraOf(target);
        if (aura == Aura.PYRO) {
            target.clearFire();
            return vaporize(target);
        }
        if (aura == Aura.CRYO) {
            freeze(target);
        }
        if (!target.hasEffect(ModContent.FROZEN)) {
            MobEffectInstance wet = target.getEffect(ModContent.WET);
            if (wet == null || wet.getDuration() < wetTicks) {
                target.addEffect(new MobEffectInstance(ModContent.WET, wetTicks));
            }
        }
        return 1f;
    }

    /** An ice hit: a wet target freezes solid. Returns whether it froze. */
    public static boolean iceHit(LivingEntity target) {
        return auraOf(target) == Aura.HYDRO && freeze(target);
    }

    /** Vaporize: a hiss and a burst of steam; the target dries off. Returns the damage multiplier. */
    public static float vaporize(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) {
            return 1f;
        }
        target.removeEffect(ModContent.WET);
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.6), target.getZ(), 18, 0.45, 0.5, 0.45, 0.08);
        level.sendParticles(ParticleTypes.WHITE_SMOKE, target.getX(), target.getY(0.8), target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.PLAYERS, 0.9f, 1.2f);
        return VAPORIZE_MULTIPLIER;
    }

    /**
     * Freeze: the target is frozen solid for 2.5s and dries off. Doesn't stack: a target that is
     * frozen, or was just thawed, can't be frozen again by a reaction for a few seconds.
     */
    public static boolean freeze(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level) || target.hasEffect(ModContent.FROZEN)
                || level.getGameTime() < target.getPersistentData().getLong(TAG_FREEZE_IMMUNE_UNTIL)) {
            return false;
        }
        target.removeEffect(ModContent.WET);
        target.addEffect(new MobEffectInstance(ModContent.FROZEN, FREEZE_TICKS));
        target.getPersistentData().putLong(TAG_FREEZE_IMMUNE_UNTIL, level.getGameTime() + FREEZE_TICKS + FREEZE_IMMUNITY_TICKS);
        level.sendParticles(ModContent.ICE_SHARD.get(), target.getX(), target.getY(0.5), target.getZ(), 16, 0.3, 0.4, 0.3, 0.08);
        level.sendParticles(ModContent.FROST_MIST.get(), target.getX(), target.getY(0.3), target.getZ(), 4, 0.4, 0.3, 0.4, 0.01);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1f, 0.6f);
        return true;
    }

    /** Launches {@code target} into the air as Airborne: +25% damage from everything until it lands (see AirborneEvents). */
    public static void launchAirborne(LivingEntity target, double upward) {
        target.setDeltaMovement(target.getDeltaMovement().x, Math.max(target.getDeltaMovement().y, upward), target.getDeltaMovement().z);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(ModContent.AIRBORNE, 60));
    }
}
