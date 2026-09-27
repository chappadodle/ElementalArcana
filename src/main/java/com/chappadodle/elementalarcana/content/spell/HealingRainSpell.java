package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** A short healing shower over the caster and nearby players; also puts out anyone burning. */
public class HealingRainSpell extends Spell {
    private static final double RADIUS = 6.0;

    public HealingRainSpell() {
        super(ModSchools.WATER, 40, 300, 5);
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer caster = context.caster();
        ServerLevel level = context.level();
        int duration = Math.round(100 * context.power());

        for (Player player : level.getEntitiesOfClass(Player.class, caster.getBoundingBox().inflate(RADIUS),
                player -> player.isAlive() && player.distanceTo(caster) <= RADIUS)) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 1));
            player.clearFire();
            level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(2.1), player.getZ(), 3, 0.3, 0.2, 0.3, 0);
        }

        level.sendParticles(ParticleTypes.FALLING_WATER, caster.getX(), caster.getY() + 3.5, caster.getZ(), 120, RADIUS * 0.6, 0.3, RADIUS * 0.6, 0);
        level.sendParticles(ParticleTypes.SPLASH, caster.getX(), caster.getY() + 0.2, caster.getZ(), 40, RADIUS * 0.5, 0.1, RADIUS * 0.5, 0.1);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 1f, 1.2f);
        return CastResult.SUCCESS;
    }
}
