package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An Ember Sprite (see EmberSpriteSpell): a little fire spirit hanging where it was placed, bobbing
 * and shedding sparks. For 8 seconds it spits a small fire bolt at the nearest hostile creature it
 * can see within 10 blocks, once a second; then it pops in a puff of flame. Its bolts are its
 * caster's (they Melt, ignite and earn XP like any fire spell). One per caster.
 */
public class EmberSprite extends Entity {
    private static final int LIFETIME_TICKS = 160;
    private static final int SHOT_TICKS = 20;
    private static final double RANGE = 10;
    private static final float BOLT_SPEED = 1.2f;
    private static final float BOLT_POWER = 0.5f;
    private static final Map<UUID, UUID> BY_CASTER = new HashMap<>();

    @Nullable
    private UUID owner;
    private float power = 1f;
    private int age;

    public EmberSprite(EntityType<? extends EmberSprite> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Places {@code owner}'s sprite at {@code at}; their older one, if any, pops. */
    public static void place(ServerLevel level, LivingEntity owner, Vec3 at, float power) {
        UUID previous = BY_CASTER.get(owner.getUUID());
        if (previous != null && level.getEntity(previous) instanceof EmberSprite old) {
            old.pop(level);
        }
        EmberSprite sprite = ModContent.EMBER_SPRITE.get().create(level);
        if (sprite == null) {
            return;
        }
        sprite.owner = owner.getUUID();
        sprite.power = power;
        sprite.moveTo(at.x, at.y, at.z, 0, 0);
        level.addFreshEntity(sprite);
        BY_CASTER.put(owner.getUUID(), sprite.getUUID());
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3, at.z, 20, 0.2, 0.2, 0.2, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.6f);
    }

    public int age() {
        return age;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide()) {
            if (age % 3 == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 0.2,
                        getZ() + (random.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        Entity caster = owner == null ? null : level.getEntity(owner);
        if (!(caster instanceof LivingEntity living) || !living.isAlive() || age >= LIFETIME_TICKS) {
            pop(level);
            return;
        }
        if (age % SHOT_TICKS == SHOT_TICKS / 2) {
            LivingEntity target = target(level, living);
            if (target != null) {
                shoot(level, living, target);
            }
        }
    }

    /** The nearest hostile creature within 10 blocks that its caster may hurt and that it can see. */
    @Nullable
    private LivingEntity target(ServerLevel level, LivingEntity caster) {
        Vec3 eye = position().add(0, 0.25, 0);
        LivingEntity best = null;
        double bestDistance = RANGE * RANGE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RANGE),
                candidate -> candidate.isAlive() && candidate instanceof Enemy && SpellTargets.canAffect(caster, candidate))) {
            Vec3 aim = candidate.getBoundingBox().getCenter();
            double distance = aim.distanceToSqr(eye);
            if (distance < bestDistance && level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                    .getType() == HitResult.Type.MISS) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private void shoot(ServerLevel level, LivingEntity caster, LivingEntity target) {
        Vec3 from = position().add(0, 0.25, 0);
        Vec3 direction = target.getBoundingBox().getCenter().subtract(from).normalize();
        SpellProjectile bolt = SpellProjectile.shootFrom(caster, ModSpells.FIREBALL.get(), from, direction.scale(BOLT_SPEED), power * BOLT_POWER);
        bolt.setVisualScale(0.45f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    private void pop(ServerLevel level) {
        level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.25, getZ(), 16, 0.15, 0.15, 0.15, 0.08);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.25, getZ(), 4, 0.1, 0.1, 0.1, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.6f);
        if (owner != null && getUUID().equals(BY_CASTER.get(owner))) {
            BY_CASTER.remove(owner);
        }
        discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        power = tag.getFloat("power");
        age = tag.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        tag.putFloat("power", power);
        tag.putInt("age", age);
    }
}
