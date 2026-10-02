package com.chappadodle.elementalarcana.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Who a spell's area effects (splash, burning ground, whirlpools, bursts) may hurt, given who cast
 * it. A player's magic spares other players (they can still be hit directly by a projectile), and
 * a monster's magic spares other monsters. The caster is never affected. Also: which creature a
 * caster is aiming at.
 */
public final class SpellTargets {

    private SpellTargets() {
    }

    public static boolean canAffect(@Nullable Entity owner, LivingEntity target) {
        if (target == owner || !target.isAlive()) {
            return false;
        }
        if (owner instanceof Enemy) {
            return !(target instanceof Enemy);
        }
        return !(target instanceof Player);
    }

    /**
     * The nearest creature {@code caster} is looking at within {@code range} blocks, in sight (walls
     * stop the aim), that {@code allowed} accepts, or null. {@code leeway} widens every creature's box
     * a little, so small ones are easy to aim at.
     */
    @Nullable
    public static LivingEntity underCrosshair(LivingEntity caster, double range, double leeway, Predicate<LivingEntity> allowed) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        BlockHitResult wall = caster.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : caster.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(leeway + 1),
                e -> e != caster && !e.isSpectator() && allowed.test(e))) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(leeway).clip(eye, end);
            if (hit.isPresent() && hit.get().distanceToSqr(eye) < bestDistance) {
                best = entity;
                bestDistance = hit.get().distanceToSqr(eye);
            }
        }
        return best;
    }
}
