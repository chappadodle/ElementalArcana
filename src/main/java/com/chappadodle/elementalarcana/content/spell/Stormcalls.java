package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.StormcloudOptions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Stormcall's clouds (see StormcallSpell): a storm cloud over its caster for 10 seconds, seven blocks
 * up (lower under a roof). It rains under it: what the caster may hurt there is soaked and put out.
 * Every 15 ticks a bolt leaps from it to a foe within 12 blocks of the caster: the one struck least
 * lately (so the bolts spread among them), a soaked one first, then the nearest. 5 damage times the
 * caster's power, half again as much if Wet (and slowed a moment), 2 to what stands close by.
 * Worked out on the server and never saved; each client draws the cloud and its rain itself from
 * one particle sent when it's called (StormcloudOptions), the bolts with Chain Lightning's.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Stormcalls {
    private static final int DURATION_TICKS = 200;
    /** Ticks between strikes, the first this long after the call (the cloud flashes on the same beat). */
    public static final int STRIKE_GAP_TICKS = 15;
    public static final int FIRST_STRIKE_TICKS = 5;
    /** How far away players are sent the cloud to draw. */
    private static final double SEEN = 96;
    private static final int HEIGHT = 7;
    private static final double LOWEST = 2.5;
    private static final double CLOUD = 3.5;
    private static final double REACH = 12;
    private static final double SPLASH = 1.5;
    private static final float DAMAGE = 5f;
    private static final float SPLASH_DAMAGE = 2f;
    private static final float WET = 1.5f;
    private static final int SOAK_TICKS = 80;

    private static final class Storm {
        final ServerLevel level;
        final UUID caster;
        final float power;
        final long endsAt;
        /** When each foe was last struck (entity id to game time). */
        final Map<Integer, Long> struck = new HashMap<>();
        double height = HEIGHT;

        Storm(ServerLevel level, UUID caster, float power, long endsAt) {
            this.level = level;
            this.caster = caster;
            this.power = power;
            this.endsAt = endsAt;
        }
    }

    private static final List<Storm> ACTIVE = new ArrayList<>();

    private Stormcalls() {
    }

    public static void start(LivingEntity caster, float power) {
        if (!(caster.level() instanceof ServerLevel level)) {
            return;
        }
        ACTIVE.removeIf(storm -> storm.caster.equals(caster.getUUID()));
        Storm storm = new Storm(level, caster.getUUID(), power, level.getGameTime() + DURATION_TICKS);
        storm.height = cloudHeight(level, caster);
        ACTIVE.add(storm);
        StormcloudOptions drawn = new StormcloudOptions(caster.getId(), DURATION_TICKS);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(caster) < SEEN * SEEN) {
                level.sendParticles(player, drawn, true, caster.getX(), caster.getY() + storm.height, caster.getZ(), 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, caster.getX(), caster.getY() + storm.height, caster.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.PLAYERS, 0.7f, 0.7f);
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || ACTIVE.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (Iterator<Storm> it = ACTIVE.iterator(); it.hasNext(); ) {
            Storm storm = it.next();
            if (storm.level != level) {
                continue;
            }
            Entity entity = level.getEntity(storm.caster);
            if (!(entity instanceof LivingEntity caster) || !caster.isAlive() || now >= storm.endsAt) {
                it.remove();
                if (entity != null) {
                    level.playSound(null, entity.getX(), entity.getY() + storm.height, entity.getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                            SoundSource.PLAYERS, 0.4f, 1.6f);
                }
                continue;
            }
            rage(level, caster, storm, now);
        }
    }

    /** Seven blocks over the caster's head, or a block under the roof if there's one lower (never below 2.5). Both sides use it. */
    public static double cloudHeight(Level level, Entity caster) {
        BlockPos feet = caster.blockPosition();
        for (int up = 2; up <= HEIGHT + 1; up++) {
            if (!level.getBlockState(feet.above(up)).getCollisionShape(level, feet.above(up)).isEmpty()) {
                return Math.max(LOWEST, up - 1);
            }
        }
        return HEIGHT;
    }

    private static void rage(ServerLevel level, LivingEntity caster, Storm storm, long now) {
        if (now % 10 == 0) {
            storm.height = cloudHeight(level, caster);
        }
        Vec3 cloud = caster.position().add(0, storm.height, 0);
        if (now % 20 == 0) {
            soak(level, caster, cloud);
            level.playSound(null, cloud.x, caster.getY(), cloud.z, SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.5f, 0.9f);
        }
        if ((now - (storm.endsAt - DURATION_TICKS)) % STRIKE_GAP_TICKS != FIRST_STRIKE_TICKS) {
            return;
        }
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(REACH),
                        other -> other != caster && other.isAlive() && other.distanceTo(caster) <= REACH && SpellTargets.canAffect(caster, other))
                .stream()
                .min(Comparator.<LivingEntity>comparingLong(other -> storm.struck.getOrDefault(other.getId(), Long.MIN_VALUE))
                        .thenComparingInt(other -> ElementalReactions.auraOf(other) == ElementalReactions.Aura.HYDRO ? 0 : 1)
                        .thenComparingDouble(other -> other.distanceToSqr(caster)))
                .orElse(null);
        if (target == null) {
            level.playSound(null, cloud.x, cloud.y, cloud.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.25f, 1.8f);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, cloud.x, cloud.y, cloud.z, 20, CLOUD * 0.4, 0.2, CLOUD * 0.4, 0.08);
            return;
        }
        strike(level, caster, storm, cloud, target);
    }

    private static void soak(ServerLevel level, LivingEntity caster, Vec3 cloud) {
        AABB under = new AABB(cloud.x - CLOUD, cloud.y - HEIGHT - 3, cloud.z - CLOUD, cloud.x + CLOUD, cloud.y, cloud.z + CLOUD);
        for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, under,
                other -> other != caster && other.isAlive() && SpellTargets.canAffect(caster, other)
                        && Math.hypot(other.getX() - cloud.x, other.getZ() - cloud.z) <= CLOUD)) {
            other.addEffect(new MobEffectInstance(ModContent.WET, SOAK_TICKS));
            other.clearFire();
        }
    }

    private static void strike(ServerLevel level, LivingEntity caster, Storm storm, Vec3 cloud, LivingEntity target) {
        RandomSource random = level.getRandom();
        Vec3 from = cloud.add((random.nextDouble() - 0.5) * CLOUD, -0.3, (random.nextDouble() - 0.5) * CLOUD);
        Vec3 to = target.getBoundingBox().getCenter();
        boolean wet = ElementalReactions.auraOf(target) == ElementalReactions.Aura.HYDRO;
        storm.struck.put(target.getId(), level.getGameTime());
        SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.LIGHTNING, caster, caster), DAMAGE * storm.power * (wet ? WET : 1f));
        if (wet) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
        }
        for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(SPLASH),
                other -> other != target && other != caster && other.isAlive() && SpellTargets.canAffect(caster, other))) {
            SpellDamage.hurtMultiHit(near, SpellDamage.source(level, Element.LIGHTNING, caster, caster), SPLASH_DAMAGE * storm.power);
        }
        ChainLightnings.bolt(level, from, to, 0, 1.6f);
        level.sendParticles(ParticleTypes.FLASH, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 16, 0.4, 0.4, 0.4, 0.15);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.9f, 1.2f);
        level.playSound(null, cloud.x, cloud.y, cloud.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.35f, 1.4f);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        ACTIVE.clear();
    }
}
