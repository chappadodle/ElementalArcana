package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** Small helpers shared by the mob spells. */
public final class MobCasting {

    private MobCasting() {
    }

    public static double distance(Mob caster, LivingEntity target) {
        return caster.distanceTo(target);
    }

    /** Where to aim at a creature: the middle of its body. */
    public static Vec3 aimPoint(LivingEntity target) {
        return target.getBoundingBox().getCenter();
    }

    /** Where a spell leaves the caster: just in front of its face, toward {@code aim}. */
    public static Vec3 castOrigin(Mob caster, Vec3 aim) {
        Vec3 eye = caster.getEyePosition();
        return eye.add(aim.subtract(eye).normalize().scale(0.6));
    }

    /** Fires a player spell's projectile (its plain Lv 1 form) straight at {@code target}, at mob power. */
    public static <S extends Spell & ProjectileSpell> SpellProjectile shoot(Mob caster, S spell, LivingEntity target) {
        Vec3 aim = aimPoint(target);
        Vec3 from = castOrigin(caster, aim);
        return SpellProjectile.shootFrom(caster, spell, from, aim.subtract(from).normalize().scale(spell.releaseSpeed(1f)), MobSpells.POWER);
    }

    public static void play(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
    }

    /** One tick of the wind-up: the element gathers at the caster's hands. */
    public static void windup(Mob caster, Element element) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        float yaw = caster.getYRot() * Mth.DEG_TO_RAD;
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 chest = caster.getEyePosition().add(caster.getLookAngle().scale(0.4)).add(0, -0.5, 0);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 hand = chest.add(right.scale(0.35 * side));
            level.sendParticles(handsParticle(element), hand.x, hand.y, hand.z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    public static ParticleOptions handsParticle(Element element) {
        return switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ModContent.HYDRO_DROP.get();
            case ICE -> ModContent.FROST_SPARKLE.get();
            case WIND -> ModContent.WIND_STREAK.get();
        };
    }
}
