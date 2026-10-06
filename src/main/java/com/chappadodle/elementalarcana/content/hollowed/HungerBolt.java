package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.api.HollowedRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A Hunger Bolt (see the Hollowed spec): a slow orb of the Hollow's dark, flying straight (no
 * gravity) for three seconds. What it hits takes hunger's harm; a player hit loses mana to its
 * thrower, which heals. Drawn as its item (the Hunger Orb), always bright, trailing violet.
 */
public class HungerBolt extends ThrowableItemProjectile {
    private static final int LIFETIME = 60;

    public HungerBolt(EntityType<? extends HungerBolt> type, Level level) {
        super(type, level);
    }

    public HungerBolt(Level level, LivingEntity thrower) {
        super(ModHollowed.HUNGER_BOLT.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModHollowed.HUNGER_ORB.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            level().addParticle(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 0.1, getZ(), 0, 0, 0);
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, 0, 0);
            }
        } else if (tickCount > LIFETIME) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof HollowedEntity);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (level() instanceof ServerLevel level && hit.getEntity() instanceof LivingEntity target) {
            Entity thrower = getOwner();
            SpellDamage.hurtMultiHit(target, ModHollowed.hunger(level, this, thrower), HollowedRules.BOLT_DAMAGE);
            if (thrower instanceof HollowedEntity hollowed) {
                hollowed.eat(target, HollowedRules.BOLT_MANA, HollowedRules.BOLT_HEAL);
            }
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(), getZ(), 24, 0.2, 0.2, 0.2, 0.15);
            level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(), getZ(), 6, 0.15, 0.15, 0.15, 0.02);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, 1f, 0.6f);
            discard();
        }
    }
}
