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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Launches the caster where they're looking and blows nearby mobs away. No fall damage on landing. */
public class GaleDashSpell extends Spell {
    private static final double PUSH_RADIUS = 3.0;

    public GaleDashSpell() {
        super(ModSchools.WIND, 20, 60);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        Vec3 look = context.look();

        // Never aim straight down into the ground; always give a little lift.
        Vec3 direction = new Vec3(look.x, Math.max(look.y, 0.1), look.z).normalize();
        caster.setDeltaMovement(direction.scale(1.5 * context.power()).add(0, 0.35, 0));
        caster.hurtMarked = true;
        caster.resetFallDistance();
        MagicAttachments.get(caster).setFallImmune(true);

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(PUSH_RADIUS),
                entity -> entity != caster && entity.isAlive())) {
            Vec3 away = target.position().subtract(caster.position()).normalize();
            target.knockback(1.0 * context.power(), -away.x, -away.z);
            target.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, caster.getX(), caster.getY(), caster.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.2, caster.getZ(), 15, 0.4, 0.1, 0.4, 0.05);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1f, 1f);
        return CastResult.SUCCESS;
    }
}
