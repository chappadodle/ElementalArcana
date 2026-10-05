package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * A cantrip that gives its caster an effect for a while (Water Breathing, Featherfall, Night Eye),
 * with a puff of its particles and a sound.
 */
public class EffectCantripSpell extends Spell {
    private final Holder<MobEffect> effect;
    private final int ticks;
    private final ParticleOptions particle;
    private final SoundEvent sound;

    public EffectCantripSpell(int manaCost, int cooldownTicks, Holder<MobEffect> effect, int ticks, ParticleOptions particle, SoundEvent sound) {
        super(ModSchools.ARCANE, manaCost, cooldownTicks);
        this.effect = effect;
        this.ticks = ticks;
        this.particle = particle;
        this.sound = sound;
    }

    @Override
    public CastResult cast(CastContext context) {
        ServerPlayer player = context.caster();
        player.addEffect(new MobEffectInstance(effect, ticks, 0, false, true, true));
        context.level().sendParticles(particle, player.getX(), player.getY(0.6), player.getZ(), 16, 0.35, 0.5, 0.35, 0.02);
        context.level().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundSource.PLAYERS, 0.8f, 1.2f);
        return CastResult.SUCCESS;
    }
}
