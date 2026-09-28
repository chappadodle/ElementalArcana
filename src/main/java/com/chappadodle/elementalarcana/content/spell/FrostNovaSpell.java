package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Predicate;

/** Flash-freezes everything around the caster and the water they stand near. */
public class FrostNovaSpell extends Spell {
    private static final double RADIUS = 5.0;

    public FrostNovaSpell() {
        super(ModSchools.ICE, 35, 120, 5);
    }

    @Override
    public CastResult cast(CastContext context) {
        burst(context.level(), context.caster(), context.power(), target -> true);
        return CastResult.SUCCESS;
    }

    /**
     * The nova itself: damages, slows and frosts everything around {@code caster} that
     * {@code affects} allows (never the caster), freezes nearby water, and plays the burst. Attuned
     * creatures cast it too.
     */
    public static void burst(ServerLevel level, LivingEntity caster, float power, Predicate<LivingEntity> affects) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(RADIUS),
                entity -> entity != caster && entity.isAlive() && entity.distanceTo(caster) <= RADIUS && affects.test(entity))) {
            ElementalReactions.iceHit(target);
            target.hurt(SpellDamage.source(level, Element.ICE, caster, caster), 3f * power);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(100 * power), 2));
            if (target.canFreeze()) {
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 120));
            }
        }
        Freezing.freezeWater(level, caster.blockPosition().below(), (int) RADIUS - 1);

        for (int step = 0; step < 48; step++) {
            float angle = step * Mth.TWO_PI / 48;
            double dx = Mth.cos(angle);
            double dz = Mth.sin(angle);
            level.sendParticles(ParticleTypes.SNOWFLAKE, caster.getX() + dx, caster.getY() + 0.4, caster.getZ() + dz,
                    0, dx, 0.02, dz, 0.35);
        }
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, caster.getX(), caster.getY() + 0.5, caster.getZ(), 30, RADIUS * 0.4, 0.3, RADIUS * 0.4, 0.1);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 0.6f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1f, 0.8f);
    }
}
