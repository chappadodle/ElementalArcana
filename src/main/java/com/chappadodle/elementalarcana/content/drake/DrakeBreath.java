package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.DrakeRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A drake's breath (see the Drakes spec): a cone BREATH_RANGE long from its mouth. Each hit hurts
 * everything in the cone (but drakes) with its element and its element's touch: fire burns, frost
 * slows and freezes the wet, lightning hurts more in rain or water and sparks to one more, a
 * torrent soaks and shoves, a gust throws up. The breath is drawn by each client from the drake's
 * state (particles).
 */
public final class DrakeBreath {

    private DrakeBreath() {
    }

    /** One hit of the breath, from {@code mouth} along {@code facing} (a unit vector). */
    public static void hit(ServerLevel level, DrakeEntity drake, Vec3 mouth, Vec3 facing) {
        Element element = drake.element();
        double reach = DrakeRules.BREATH_RANGE;
        AABB area = new AABB(mouth, mouth).inflate(reach + 1);
        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, area, entity -> entity.isAlive()
                && !(entity instanceof DrakeEntity) && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))
                && inCone(mouth, facing, entity));
        for (LivingEntity target : caught) {
            touch(level, drake, target, element, facing, caught);
        }
    }

    private static boolean inCone(Vec3 mouth, Vec3 facing, LivingEntity target) {
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
        return DrakeRules.inBreath(to.x, to.y, to.z, facing.x, facing.y, facing.z);
    }

    private static void touch(ServerLevel level, DrakeEntity drake, LivingEntity target, Element element, Vec3 facing, List<LivingEntity> caught) {
        float damage = DrakeRules.BREATH_DAMAGE;
        switch (element) {
            case FIRE -> damage *= ElementalReactions.fireHit(target);
            case WATER -> damage *= ElementalReactions.waterHit(target, 100);
            case ICE -> ElementalReactions.iceHit(target);
            case LIGHTNING -> {
                if (target.isInWaterRainOrBubble()) {
                    damage *= 1.5f;
                }
            }
            default -> {
            }
        }
        SpellDamage.hurtMultiHit(target, SpellDamage.source(level, element, drake, drake), damage);
        switch (element) {
            case FIRE -> target.igniteForTicks(60);
            case ICE -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                if (target.canFreeze()) {
                    target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 80, target.getTicksFrozen() + 30));
                }
            }
            case WATER -> push(target, facing, 0.35, 0.08);
            case WIND -> {
                push(target, facing, 0.45, 0.0);
                ElementalReactions.launchAirborne(target, 0.45);
            }
            case LIGHTNING -> spark(level, drake, target, caught, damage);
            default -> {
            }
        }
    }

    private static void push(LivingEntity target, Vec3 facing, double strength, double up) {
        target.setDeltaMovement(target.getDeltaMovement().add(facing.x * strength, up, facing.z * strength));
        target.hurtMarked = true;
    }

    /** Lightning leaps from one caught by the breath to one more near it, for half. */
    private static void spark(ServerLevel level, DrakeEntity drake, LivingEntity from, List<LivingEntity> caught, float damage) {
        if (level.getRandom().nextInt(3) != 0) {
            return;
        }
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(4), entity -> entity != from
                && entity.isAlive() && !(entity instanceof DrakeEntity) && !caught.contains(entity)
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator())));
        if (near.isEmpty()) {
            return;
        }
        LivingEntity to = near.get(level.getRandom().nextInt(near.size()));
        SpellDamage.hurtMultiHit(to, SpellDamage.source(level, Element.LIGHTNING, drake, drake), damage * 0.5f);
        Vec3 a = from.position().add(0, from.getBbHeight() * 0.5, 0);
        Vec3 b = to.position().add(0, to.getBbHeight() * 0.5, 0);
        for (int i = 0; i <= 8; i++) {
            Vec3 at = a.lerp(b, i / 8.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
        }
    }

    /** The warning (a glow at its jaws) or the breath (a stream along the cone), drawn on the client. */
    static void particles(DrakeEntity drake, boolean breathing) {
        Level level = drake.level();
        RandomSource random = drake.getRandom();
        Vec3 mouth = drake.mouth();
        if (!breathing) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(drake.handsParticle(), mouth.x + (random.nextDouble() - 0.5) * 0.6, mouth.y + (random.nextDouble() - 0.5) * 0.6,
                        mouth.z + (random.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
            }
            return;
        }
        // Where it aims: its heading, and its pitch (the server turns it to its prey).
        Vec3 ahead = Vec3.directionFromRotation(drake.getXRot(), drake.getYRot());
        ParticleOptions main = switch (drake.element()) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case WATER -> ModContent.HYDRO_DROP.get();
            default -> ParticleTypes.CLOUD;
        };
        for (int i = 0; i < 8; i++) {
            double spread = 0.22;
            Vec3 dir = ahead.add((random.nextDouble() - 0.5) * spread * 2, (random.nextDouble() - 0.5) * spread * 2,
                    (random.nextDouble() - 0.5) * spread * 2).normalize();
            double speed = 0.6 + random.nextDouble() * 0.4;
            level.addParticle(main, mouth.x, mouth.y, mouth.z, dir.x * speed, dir.y * speed, dir.z * speed);
        }
        if (drake.element() == Element.FIRE && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, ahead.x * 0.3, 0.05, ahead.z * 0.3);
        }
        if (drake.element() == Element.ICE) {
            level.addParticle(ModContent.FROST_MIST.get(), mouth.x, mouth.y, mouth.z, ahead.x * 0.5, ahead.y * 0.5, ahead.z * 0.5);
        }
    }
}
