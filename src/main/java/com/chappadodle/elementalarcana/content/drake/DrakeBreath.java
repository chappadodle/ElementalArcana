package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.content.mob.MobCasting;
import net.minecraft.world.entity.Entity;
import java.util.function.Predicate;
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

    /** One hit of a wild drake's breath, from {@code mouth} along {@code facing} (a unit vector): all but drakes. */
    public static void hit(ServerLevel level, DrakeEntity drake, Vec3 mouth, Vec3 facing) {
        hit(level, drake, drake.element(), mouth, facing, entity -> !(entity instanceof DrakeEntity));
    }

    /** One hit of a breath of {@code element} from {@code breather}, on whatever {@code mayHit} allows (and never creative players). */
    public static void hit(ServerLevel level, LivingEntity breather, Element element, Vec3 mouth, Vec3 facing, Predicate<LivingEntity> mayHit) {
        double reach = DrakeRules.BREATH_RANGE;
        Predicate<LivingEntity> allowed = entity -> entity.isAlive() && entity != breather && mayHit.test(entity)
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()));
        AABB area = new AABB(mouth, mouth).inflate(reach + 1);
        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, area, entity -> allowed.test(entity) && inCone(mouth, facing, entity));
        for (LivingEntity target : caught) {
            touch(level, breather, target, element, facing, caught, allowed);
        }
    }

    private static boolean inCone(Vec3 mouth, Vec3 facing, LivingEntity target) {
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
        return DrakeRules.inBreath(to.x, to.y, to.z, facing.x, facing.y, facing.z);
    }

    private static void touch(ServerLevel level, LivingEntity drake, LivingEntity target, Element element, Vec3 facing, List<LivingEntity> caught,
                              Predicate<LivingEntity> allowed) {
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
            case LIGHTNING -> spark(level, drake, target, caught, damage, allowed);
            default -> {
            }
        }
    }

    private static void push(LivingEntity target, Vec3 facing, double strength, double up) {
        target.setDeltaMovement(target.getDeltaMovement().add(facing.x * strength, up, facing.z * strength));
        target.hurtMarked = true;
    }

    /** Lightning leaps from one caught by the breath to one more near it, for half. */
    private static void spark(ServerLevel level, LivingEntity drake, LivingEntity from, List<LivingEntity> caught, float damage,
                              Predicate<LivingEntity> allowed) {
        if (level.getRandom().nextInt(3) != 0) {
            return;
        }
        List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(4), entity -> entity != from
                && allowed.test(entity) && !caught.contains(entity));
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

    /**
     * The warning (a glow at its jaws) or the breath (a stream from {@code mouth} along {@code ahead}),
     * drawn on the client; {@code scale} for a young drake's smaller breath.
     */
    static void particles(Entity drake, Element element, Vec3 mouth, Vec3 ahead, float scale, boolean breathing) {
        Level level = drake.level();
        RandomSource random = drake.getRandom();
        if (!breathing) {
            for (int i = 0; i < 2; i++) {
                level.addParticle(MobCasting.handsParticle(element), mouth.x + (random.nextDouble() - 0.5) * 0.6 * scale,
                        mouth.y + (random.nextDouble() - 0.5) * 0.6 * scale, mouth.z + (random.nextDouble() - 0.5) * 0.6 * scale, 0, 0.02, 0);
            }
            return;
        }
        ParticleOptions main = switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case WATER -> ModContent.HYDRO_DROP.get();
            default -> ParticleTypes.CLOUD;
        };
        int count = Math.max(2, Math.round(8 * scale));
        for (int i = 0; i < count; i++) {
            double spread = 0.22;
            Vec3 dir = ahead.add((random.nextDouble() - 0.5) * spread * 2, (random.nextDouble() - 0.5) * spread * 2,
                    (random.nextDouble() - 0.5) * spread * 2).normalize();
            double speed = (0.6 + random.nextDouble() * 0.4) * (0.4 + 0.6 * scale);
            level.addParticle(main, mouth.x, mouth.y, mouth.z, dir.x * speed, dir.y * speed, dir.z * speed);
        }
        if (element == Element.FIRE && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, mouth.x, mouth.y, mouth.z, ahead.x * 0.3, 0.05, ahead.z * 0.3);
        }
        if (element == Element.ICE) {
            level.addParticle(ModContent.FROST_MIST.get(), mouth.x, mouth.y, mouth.z, ahead.x * 0.5, ahead.y * 0.5, ahead.z * 0.5);
        }
    }
}
