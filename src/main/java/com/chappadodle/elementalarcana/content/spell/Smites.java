package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Smite's pillars of light: a strike waits 15 ticks over its mark (a ring of light rising from the
 * ground), then falls: 7 damage times the caster's power within 2.5 blocks, half again as much for
 * the undead, 3 seconds alight and 5 of glowing. Server-side, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Smites {
    private static final int DELAY_TICKS = 15;
    private static final double RADIUS = 2.5;
    private static final double HEIGHT = 12;
    private static final float DAMAGE = 7f;
    private static final float UNDEAD = 1.5f;
    private static final DustParticleOptions GLOW = new DustParticleOptions(new Vector3f(1f, 0.92f, 0.62f), 1.2f);

    private record Strike(ServerLevel level, Vec3 at, UUID caster, float power, Predicate<LivingEntity> affects, long fallsAt) {
    }

    private static final List<Strike> STRIKES = new ArrayList<>();

    private Smites() {
    }

    public static void strike(ServerLevel level, Vec3 at, Entity caster, float power, Predicate<LivingEntity> affects) {
        STRIKES.add(new Strike(level, at, caster.getUUID(), power, affects, level.getGameTime() + DELAY_TICKS));
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.8f);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || STRIKES.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        List<Strike> falling = new ArrayList<>();
        for (Iterator<Strike> it = STRIKES.iterator(); it.hasNext(); ) {
            Strike strike = it.next();
            if (strike.level() != level) {
                continue;
            }
            if (now >= strike.fallsAt()) {
                it.remove();
                falling.add(strike);
            } else if (now % 2 == 0) {
                mark(level, strike.at());
            }
        }
        // After the loop: whatever the hits set off can't touch the list mid-walk.
        falling.forEach(strike -> fall(level, strike));
    }

    /** The mark: a ring of light on the ground, rising. */
    private static void mark(ServerLevel level, Vec3 at) {
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12 + level.getGameTime() * 0.2;
            level.sendParticles(ParticleTypes.END_ROD, at.x + Math.cos(angle) * RADIUS, at.y + 0.1, at.z + Math.sin(angle) * RADIUS,
                    0, 0, 0.6, 0, 0.08);
        }
    }

    private static void fall(ServerLevel level, Strike strike) {
        Vec3 at = strike.at();
        Entity caster = level.getEntity(strike.caster());
        // The pillar, falling from the sky to the mark: a bright core in a wider, softer glow.
        for (double y = HEIGHT; y >= 0; y -= 0.25) {
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + y, at.z, 3, 0.12, 0.08, 0.12, 0.01);
            level.sendParticles(GLOW, at.x, at.y + y, at.z, 2, 0.35, 0.1, 0.35, 0);
        }
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.3, at.z, 40, RADIUS / 2, 0.2, RADIUS / 2, 0.15);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 2f);
        AABB area = new AABB(at, at).inflate(RADIUS, 2.5, RADIUS);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != caster && e.distanceToSqr(at) <= (RADIUS + 1) * (RADIUS + 1) && strike.affects().test(e))) {
            float damage = DAMAGE * strike.power() * (target.getType().is(EntityTypeTags.UNDEAD) ? UNDEAD : 1f);
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.RADIANCE, caster, caster), damage);
            target.igniteForSeconds(3);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STRIKES.clear();
    }
}
