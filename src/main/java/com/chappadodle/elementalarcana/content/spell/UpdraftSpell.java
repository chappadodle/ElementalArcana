package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** A column of wind: throws the caster and nearby mobs skyward. The caster floats down safely. */
public class UpdraftSpell extends Spell {
    private static final double RADIUS = 4.0;

    public UpdraftSpell() {
        super(ModSchools.WIND, 30, 160, 5);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();

        Vec3 motion = caster.getDeltaMovement();
        caster.setDeltaMovement(motion.x * 0.5, 1.3 * context.power(), motion.z * 0.5);
        caster.hurtMarked = true;
        caster.resetFallDistance();
        caster.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100));
        MagicAttachments.get(caster).setFallImmune(true);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(RADIUS),
                entity -> entity != caster && entity.isAlive() && entity.distanceTo(caster) <= RADIUS)) {
            target.setDeltaMovement(target.getDeltaMovement().x, 1.0 * context.power(), target.getDeltaMovement().z);
            target.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, caster.getX(), caster.getY(), caster.getZ(), 1, 0, 0, 0, 0);
        for (int height = 0; height < 6; height++) {
            level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + height * 0.7, caster.getZ(), 6, 0.4, 0.2, 0.4, 0.02);
        }
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1f, 0.7f);
        return CastResult.SUCCESS;
    }
}
