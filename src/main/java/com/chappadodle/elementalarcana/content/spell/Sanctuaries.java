package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Sanctuaries (see SanctuarySpell): circles of light 4 blocks across that last 8 seconds. Each
 * second everyone inside who isn't the caster's foe (the caster too) is mended by a heart times its
 * power, and the caster's undead foes inside burn and take as much radiant damage. Server-side,
 * never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Sanctuaries {
    public static final double RADIUS = 4;
    private static final int DURATION_TICKS = 160;
    private static final float HEAL = 2f;
    private static final float SEAR = 2f;

    private record Sanctuary(ServerLevel level, Vec3 center, UUID caster, float power, long endsAt, Predicate<LivingEntity> foes) {
    }

    private static final List<Sanctuary> SANCTUARIES = new ArrayList<>();

    private Sanctuaries() {
    }

    public static void place(ServerLevel level, Vec3 center, LivingEntity caster, float power, Predicate<LivingEntity> foes) {
        SANCTUARIES.add(new Sanctuary(level, center, caster.getUUID(), power, level.getGameTime() + DURATION_TICKS, foes));
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.2, center.z, 40, RADIUS / 2, 0.1, RADIUS / 2, 0.05);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.4f);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || SANCTUARIES.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Sanctuary> it = SANCTUARIES.iterator(); it.hasNext(); ) {
            Sanctuary sanctuary = it.next();
            if (sanctuary.level() != level) {
                continue;
            }
            Vec3 center = sanctuary.center();
            if (now >= sanctuary.endsAt()) {
                it.remove();
                level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 1.4f);
                continue;
            }
            if (now % 4 == 0) {
                draw(level, center, now);
            }
            if ((sanctuary.endsAt() - now) % 20 == 0) {
                pulse(level, sanctuary);
            }
        }
    }

    /** The circle's rim turning slowly, and motes of light rising inside it. */
    private static void draw(ServerLevel level, Vec3 center, long now) {
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2 * i / 16 + now * 0.05;
            level.sendParticles(ParticleTypes.END_ROD, center.x + Math.cos(angle) * RADIUS, center.y + 0.1, center.z + Math.sin(angle) * RADIUS,
                    1, 0, 0.02, 0, 0);
        }
        level.sendParticles(ParticleTypes.WAX_OFF, center.x, center.y + 0.5, center.z, 4, RADIUS / 2, 0.3, RADIUS / 2, 0.05);
    }

    private static void pulse(ServerLevel level, Sanctuary sanctuary) {
        Vec3 center = sanctuary.center();
        Entity caster = level.getEntity(sanctuary.caster());
        AABB area = new AABB(center, center).inflate(RADIUS, 3, RADIUS);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && Math.hypot(living.getX() - center.x, living.getZ() - center.z) <= RADIUS)) {
            if (!sanctuary.foes().test(living)) {
                if (living.getHealth() < living.getMaxHealth()) {
                    living.heal(HEAL * sanctuary.power());
                    level.sendParticles(ParticleTypes.HEART, living.getX(), living.getY(1.0) + 0.3, living.getZ(), 1, 0.2, 0.1, 0.2, 0);
                }
            } else if (living.getType().is(EntityTypeTags.UNDEAD)) {
                SpellDamage.hurtMultiHit(living, SpellDamage.source(level, Element.RADIANCE, caster, caster), SEAR * sanctuary.power());
                living.igniteForSeconds(2);
                level.sendParticles(ParticleTypes.END_ROD, living.getX(), living.getY(0.5), living.getZ(), 6, 0.2, 0.4, 0.2, 0.05);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SANCTUARIES.clear();
    }
}
