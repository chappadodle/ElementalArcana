package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Prism Wards (see PrismWardSpell): crystal facets circling a creature or player for a while. A
 * projectile that would hit them (an arrow, a fireball, a spell) is turned back at whoever shot it,
 * a little faster, and is now the warded one's. Server-side, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class PrismWards {
    private static final DustParticleOptions FACET = new DustParticleOptions(new Vector3f(0.82f, 0.55f, 1f), 1f);
    private static final int FACETS = 6;
    private static final double ORBIT = 1.1;

    private static final Map<UUID, Long> WARDS = new HashMap<>();

    private PrismWards() {
    }

    public static void raise(LivingEntity entity, int ticks) {
        WARDS.put(entity.getUUID(), entity.level().getGameTime() + ticks);
        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(FACET, entity.getX(), entity.getY(0.6), entity.getZ(), 30, 0.6, 0.7, 0.6, 0);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.8f);
        }
    }

    public static boolean has(Entity entity) {
        Long ends = WARDS.get(entity.getUUID());
        return ends != null && ends > entity.level().getGameTime();
    }

    @SubscribeEvent
    public static void onImpact(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide() || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof LivingEntity warded) || !has(warded) || projectile.getOwner() == warded) {
            return;
        }
        event.setCanceled(true);
        reflect(projectile, warded);
    }

    /** Turned back at whoever shot it (or straight back, if no one did), a tenth faster, and now the warded one's. */
    private static void reflect(Projectile projectile, LivingEntity warded) {
        Vec3 velocity = projectile.getDeltaMovement();
        double speed = Math.max(0.6, velocity.length()) * 1.1;
        Entity shooter = projectile.getOwner();
        Vec3 back = shooter != null && shooter.isAlive()
                ? shooter.getBoundingBox().getCenter().subtract(projectile.position()).normalize().scale(speed)
                : velocity.scale(-1.1);
        projectile.setDeltaMovement(back);
        projectile.setOwner(warded);
        projectile.hasImpulse = true;
        if (projectile.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, projectile.getX(), projectile.getY(), projectile.getZ(), 6, 0.1, 0.1, 0.1, 0.08);
            level.sendParticles(FACET, projectile.getX(), projectile.getY(), projectile.getZ(), 10, 0.2, 0.2, 0.2, 0);
            level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.6f);
        }
    }

    /** The facets circling each warded one, every other tick; spent wards are let go. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || WARDS.isEmpty() || level.getGameTime() % 2 != 0) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Map.Entry<UUID, Long>> it = WARDS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> ward = it.next();
            Entity entity = level.getEntity(ward.getKey());
            if (entity == null) {
                continue;
            }
            if (ward.getValue() <= now || !entity.isAlive()) {
                it.remove();
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
                continue;
            }
            for (int i = 0; i < FACETS; i++) {
                double angle = now * 0.15 + Math.PI * 2 * i / FACETS;
                level.sendParticles(FACET, entity.getX() + Math.cos(angle) * ORBIT, entity.getY(0.55) + Math.sin(angle * 2) * 0.25,
                        entity.getZ() + Math.sin(angle) * ORBIT, 1, 0, 0, 0, 0);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WARDS.clear();
    }
}
