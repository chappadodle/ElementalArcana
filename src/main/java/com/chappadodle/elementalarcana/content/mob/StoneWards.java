package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SpellTargets;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An Earth Archmage's ward: the mob's version of Stone Skin (the player shield system only works
 * on players). It absorbs 14 damage for 10 seconds, shown as stones circling the mob, and
 * bursts when broken, shoving the enemies around it back. Server-side, never saved.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class StoneWards {
    private static final float STRENGTH = 14f;
    private static final int DURATION_TICKS = 200;
    private static final double BURST_RADIUS = 3;
    private static final Map<UUID, Ward> WARDS = new HashMap<>();

    private static final class Ward {
        private float amount;
        private final long endsAt;

        private Ward(float amount, long endsAt) {
            this.amount = amount;
            this.endsAt = endsAt;
        }
    }

    private StoneWards() {
    }

    public static boolean has(LivingEntity entity) {
        return WARDS.containsKey(entity.getUUID());
    }

    public static void raise(Mob mob) {
        WARDS.put(mob.getUUID(), new Ward(STRENGTH, mob.level().getGameTime() + DURATION_TICKS));
        ServerLevel level = (ServerLevel) mob.level();
        level.sendParticles(rubble(), mob.getX(), mob.getY(0.5), mob.getZ(), 24, 0.5, 0.6, 0.5, 0.02);
        MobCasting.play(mob, SoundEvents.STONE_PLACE, 1f, 0.7f);
        MobCasting.play(mob, SoundEvents.GRAVEL_PLACE, 1f, 0.8f);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity mob = event.getEntity();
        Ward ward = WARDS.get(mob.getUUID());
        if (ward == null || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        float absorbed = Math.min(ward.amount, event.getAmount());
        ward.amount -= absorbed;
        event.setAmount(event.getAmount() - absorbed);
        level.sendParticles(rubble(), mob.getX(), mob.getY(0.5), mob.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
        MobCasting.play(mob, SoundEvents.STONE_HIT, 1f, 0.8f);
        if (ward.amount <= 0) {
            WARDS.remove(mob.getUUID());
            shatter(level, mob);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        Ward ward = WARDS.get(mob.getUUID());
        if (ward == null) {
            return;
        }
        if (level.getGameTime() >= ward.endsAt) {
            WARDS.remove(mob.getUUID());
            level.sendParticles(rubble(), mob.getX(), mob.getY(0.5), mob.getZ(), 10, 0.4, 0.4, 0.4, 0.02);
            return;
        }
        if (mob.tickCount % 2 == 0) {
            // Three stones circling the mob at chest height.
            double radius = mob.getBbWidth() * 0.5 + 0.5;
            for (int i = 0; i < 3; i++) {
                float angle = mob.tickCount * 0.2f + i * Mth.TWO_PI / 3;
                level.sendParticles(rubble(), mob.getX() + Mth.cos(angle) * radius, mob.getY(0.6),
                        mob.getZ() + Mth.sin(angle) * radius, 1, 0, 0, 0, 0);
            }
        }
    }

    private static ParticleOptions rubble() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    }

    /** Broken: the ward bursts into stones and shoves the enemies around it back. */
    private static void shatter(ServerLevel level, LivingEntity mob) {
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(BURST_RADIUS),
                entity -> SpellTargets.canAffect(mob, entity) && entity.distanceTo(mob) <= BURST_RADIUS)) {
            Vec3 away = nearby.position().subtract(mob.position()).normalize();
            nearby.knockback(1.0, -away.x, -away.z);
            nearby.hurtMarked = true;
        }
        level.sendParticles(rubble(), mob.getX(), mob.getY(0.5), mob.getZ(), 36, 0.2, 0.3, 0.2, 0.3);
        MobCasting.play(mob, SoundEvents.STONE_BREAK, 1f, 0.7f);
    }

    @SubscribeEvent
    public static void onLeaveLevel(EntityLeaveLevelEvent event) {
        WARDS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        WARDS.clear();
    }
}
