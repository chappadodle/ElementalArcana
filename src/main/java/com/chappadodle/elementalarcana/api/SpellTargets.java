package com.chappadodle.elementalarcana.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Who a spell's area effects (splash, burning ground, whirlpools, bursts) may hurt, given who cast
 * it. A player's magic spares other players (they can still be hit directly by a projectile) and
 * their own pets, a familiar's is its master's, and a monster's magic spares other monsters. The
 * mages' allies (the Circle's mages, MageAlly) stand with players: a player's magic never touches
 * them, and theirs touches only monsters and whatever they're fighting. Two who are fighting a duel
 * (the Circle's) are fair game for each other, whatever else holds. The caster is never affected.
 * Also: whom a spell spares even with a direct hit, and which creature a caster is aiming at.
 */
public final class SpellTargets {

    /** Whether two creatures are fighting a duel with each other (set by content code: the Circle's duels). */
    private static BiPredicate<Entity, Entity> duelling = (a, b) -> false;

    private SpellTargets() {
    }

    /** Sets who is fighting a duel with whom (the Circle's duels). */
    public static void duels(BiPredicate<Entity, Entity> test) {
        duelling = test;
    }

    public static boolean canAffect(@Nullable Entity owner, LivingEntity target) {
        if (target == owner || !target.isAlive()) {
            return false;
        }
        if (owner != null && duelling.test(owner, target)) {
            return true;
        }
        if (spares(owner, target)) {
            return false;
        }
        // A familiar's magic is its master's.
        if (owner instanceof OwnableEntity pet && pet.getOwner() instanceof Player master) {
            return canAffect(master, target);
        }
        if (owner instanceof Enemy) {
            return !(target instanceof Enemy);
        }
        if (owner instanceof MageAlly) {
            return target instanceof Enemy || owner instanceof Mob mob && target == mob.getTarget();
        }
        return !(target instanceof Player);
    }

    /**
     * Whom a spell never touches, even with a direct hit: a player's own pets and the mages' allies
     * (a familiar's master too, and whom the master's magic spares), and for an ally's magic,
     * players, their pets and the other allies.
     */
    public static boolean spares(@Nullable Entity owner, Entity target) {
        if (owner != null && duelling.test(owner, target)) {
            return false;
        }
        if (owner instanceof OwnableEntity pet && pet.getOwner() instanceof Player master) {
            return target == master || spares(master, target);
        }
        if (owner instanceof MageAlly) {
            return target instanceof Player || target instanceof MageAlly || target instanceof OwnableEntity pet && pet.getOwnerUUID() != null;
        }
        if (owner instanceof Player player) {
            return target instanceof MageAlly || target instanceof OwnableEntity pet && player.getUUID().equals(pet.getOwnerUUID());
        }
        return false;
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
