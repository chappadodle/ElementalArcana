package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Up close (within 2.5 blocks), every Attuned creature can burst its element in your face: a
 * little damage and a shove back out of melee range, so it can go back to casting. Fire sets you
 * alight, water soaks you, ice frosts and slows you, wind throws you further, earth slows you.
 */
final class CloseBurst implements MobSpell {
    private static final double RANGE = 2.5;
    private static final float DAMAGE = 2f;

    private final Element element;

    CloseBurst(Element element) {
        this.element = element;
    }

    @Override
    public AttunementRank rank() {
        return AttunementRank.ADEPT;
    }

    @Override
    public int cooldownTicks() {
        return 100;
    }

    @Override
    public boolean canCast(Mob caster, LivingEntity target) {
        return MobCasting.distance(caster, target) <= RANGE;
    }

    @Override
    public void cast(Mob caster, LivingEntity target) {
        ServerLevel level = (ServerLevel) caster.level();
        Vec3 away = target.position().subtract(caster.position()).multiply(1, 0, 1);
        if (away.lengthSqr() < 1e-4) {
            away = caster.getLookAngle().multiply(1, 0, 1);
        }
        away = away.normalize();

        float damage = DAMAGE * MobSpells.POWER;
        switch (element) {
            case FIRE -> damage *= ElementalReactions.fireHit(target);
            case WATER -> damage *= ElementalReactions.waterHit(target, 100);
            case ICE -> ElementalReactions.iceHit(target);
            case WIND, EARTH -> {
            }
        }
        // The burst decides the knockback, not the damage.
        Vec3 motion = target.getDeltaMovement();
        SpellDamage.hurtMultiHit(target, SpellDamage.source(level, element, caster, caster), damage);
        double push = element == Element.WIND ? 1.4 : 1.0;
        target.setDeltaMovement(motion.add(away.x * push, 0.35, away.z * push));
        target.hurtMarked = true;
        switch (element) {
            case FIRE -> target.igniteForTicks(60);
            case ICE -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                if (target.canFreeze()) {
                    target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 40));
                }
            }
            case EARTH -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            default -> {
            }
        }

        Vec3 at = caster.getEyePosition().add(away.scale(0.8)).add(0, -0.4, 0);
        level.sendParticles(MobCasting.handsParticle(element), at.x, at.y, at.z, 20, 0.3, 0.3, 0.3, 0.12);
        switch (element) {
            case FIRE -> level.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0);
            case WATER -> level.sendParticles(ParticleTypes.SPLASH, at.x, at.y, at.z, 30, 0.4, 0.3, 0.4, 0.2);
            case ICE -> level.sendParticles(ModContent.ICE_SHARD.get(), at.x, at.y, at.z, 16, 0.2, 0.2, 0.2, 0.2);
            case WIND -> level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            case EARTH -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at.x, at.y, at.z, 18, 0.3, 0.3, 0.3, 0.1);
            case CRYSTAL -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()), at.x, at.y, at.z, 18, 0.3, 0.3, 0.3, 0.1);
            case LIGHTNING -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 24, 0.4, 0.4, 0.4, 0.3);
            case RADIANCE -> level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
        MobCasting.play(caster, sound(), 1f, 1.1f);
    }

    private SoundEvent sound() {
        return switch (element) {
            case FIRE -> SoundEvents.FIRECHARGE_USE;
            case WATER -> SoundEvents.GENERIC_SPLASH;
            case ICE -> SoundEvents.GLASS_BREAK;
            case WIND -> SoundEvents.WIND_CHARGE_BURST.value();
            case EARTH -> SoundEvents.STONE_BREAK;
            case CRYSTAL -> SoundEvents.AMETHYST_CLUSTER_BREAK;
            case LIGHTNING -> SoundEvents.TRIDENT_THUNDER.value();
            case RADIANCE -> SoundEvents.BEACON_DEACTIVATE;
        };
    }
}
