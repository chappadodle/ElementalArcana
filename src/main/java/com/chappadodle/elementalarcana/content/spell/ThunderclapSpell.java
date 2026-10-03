package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Lightning's second spell: a thunderclap around the caster. Everything within 5 blocks it may hurt
 * takes 4 damage times its power (half again as much if wet), is thrown back and stunned for a
 * second and a half (three if wet: slowed to a crawl, weakened), a spark arcing out to each.
 * Lightning wisps of Magus rank clap too.
 */
public class ThunderclapSpell extends Spell {
    private static final double RADIUS = 5;
    private static final float DAMAGE = 4f;
    private static final float WET = 1.5f;
    private static final int STUN_TICKS = 30;

    public ThunderclapSpell() {
        super(ModSchools.LIGHTNING, 30, 240, 10);
    }

    @Override
    public CastResult cast(CastContext context) {
        clap(context.level(), context.caster(), context.power(), target -> SpellTargets.canAffect(context.caster(), target));
        return CastResult.SUCCESS;
    }

    public static void clap(ServerLevel level, LivingEntity caster, float power, Predicate<LivingEntity> affects) {
        Vec3 center = caster.getBoundingBox().getCenter();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(RADIUS),
                target -> target != caster && target.isAlive() && target.distanceTo(caster) <= RADIUS && affects.test(target))) {
            boolean wet = ElementalReactions.auraOf(target) == ElementalReactions.Aura.HYDRO;
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.LIGHTNING, caster, caster), DAMAGE * power * (wet ? WET : 1f));
            int stun = wet ? STUN_TICKS * 2 : STUN_TICKS;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 4));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 1));
            Vec3 away = target.position().subtract(caster.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(away.scale(0.7)).add(0, 0.25, 0));
            target.hurtMarked = true;
            ChainLightningSpell.bolt(level, center, target.getBoundingBox().getCenter(), 0);
        }
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        for (int ring = 1; ring <= 2; ring++) {
            double r = RADIUS * ring / 2;
            int points = 12 * ring;
            for (int i = 0; i < points; i++) {
                double angle = Math.PI * 2 * i / points;
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x + Math.cos(angle) * r, center.y - 0.4, center.z + Math.sin(angle) * r,
                        2, 0.05, 0.05, 0.05, 0.1);
            }
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.2f, 1.3f);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1f, 1.5f);
    }
}
