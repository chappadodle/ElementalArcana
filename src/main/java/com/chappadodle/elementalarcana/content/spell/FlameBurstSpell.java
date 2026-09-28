package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** A ring of fire around the caster: burns and shoves back everything close. Never damages blocks. */
public class FlameBurstSpell extends Spell {
    private static final double RADIUS = 4.0;

    public FlameBurstSpell() {
        super(ModSchools.FIRE, 35, 100, 5);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(RADIUS),
                entity -> entity != caster && entity.isAlive() && entity.distanceTo(caster) <= RADIUS)) {
            target.hurt(SpellDamage.source(level, Element.FIRE, caster, caster), 5f * context.power());
            target.igniteForTicks(80);
            Vec3 away = target.position().subtract(caster.position()).normalize();
            target.knockback(0.6 * context.power(), -away.x, -away.z);
        }

        for (double radius = 1.0; radius <= RADIUS; radius += 1.0) {
            for (int step = 0; step < 36; step++) {
                float angle = step * Mth.TWO_PI / 36;
                level.sendParticles(ParticleTypes.FLAME, caster.getX() + Mth.cos(angle) * radius, caster.getY() + 0.2,
                        caster.getZ() + Mth.sin(angle) * radius, 1, 0, 0.05, 0, 0.02);
            }
        }
        level.sendParticles(ParticleTypes.LAVA, caster.getX(), caster.getY() + 0.5, caster.getZ(), 12, 0.6, 0.2, 0.6, 0);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 0.7f);
        return CastResult.SUCCESS;
    }
}
