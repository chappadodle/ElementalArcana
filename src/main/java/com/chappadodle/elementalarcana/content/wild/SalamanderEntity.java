package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.WildRules;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * An Ember Salamander (see the Creatures of the Wild spec): a long, low lizard of glowing scales in
 * the badlands, deserts and the Nether. It minds its own business until someone comes within 4
 * blocks or hurts it; then it spits fire (a small fireball, after its throat glows) and bites
 * (setting its prey alight). It runs on lava as on stone and heals there; water and rain hurt it.
 */
public class SalamanderEntity extends Monster {
    private static final EntityDataAccessor<Boolean> SPITTING = SynchedEntityData.defineId(SalamanderEntity.class, EntityDataSerializers.BOOLEAN);

    private long nextSpitAt;

    public SalamanderEntity(EntityType<? extends SalamanderEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        setPathfindingMalus(PathType.LAVA, 0f);
        setPathfindingMalus(PathType.DANGER_FIRE, 0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0f);
        setPathfindingMalus(PathType.WATER, -1f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPITTING, false);
    }

    public boolean isSpitting() {
        return entityData.get(SPITTING);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new SpitGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> distanceTo(target) <= WildRules.SALAMANDER_NOTICE));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (getRandom().nextInt(10) == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, getRandomX(0.4), getY() + 0.35, getRandomZ(0.4), 0, 0.01, 0);
            }
            if (isSpitting()) {
                float yaw = yBodyRot * Mth.DEG_TO_RAD;
                level().addParticle(ParticleTypes.FLAME, getX() - Mth.sin(yaw) * 0.9, getY() + 0.4, getZ() + Mth.cos(yaw) * 0.9,
                        0, 0.03, 0);
            }
        } else if (onLava() && tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(1f);
        }
    }

    /** It runs on lava like on stone (as a strider does), and lava heals it. */
    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    private boolean onLava() {
        return isInLava() || level().getFluidState(blockPosition()).is(FluidTags.LAVA)
                || level().getFluidState(blockPosition().below()).is(FluidTags.LAVA);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.igniteForTicks(WildRules.BITE_FIRE_TICKS);
        }
        return hit;
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    /** From a few blocks off: its throat glows, then it spits a ball of fire. */
    private class SpitGoal extends Goal {
        private int windup;

        SpitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && level().getGameTime() >= nextSpitAt
                    && distanceTo(target) > 2.5 && distanceTo(target) <= WildRules.SPIT_RANGE && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            windup = 10;
            entityData.set(SPITTING, true);
        }

        @Override
        public void stop() {
            windup = 0;
            entityData.set(SPITTING, false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null || !(level() instanceof ServerLevel level)) {
                return;
            }
            getLookControl().setLookAt(target, 30f, 30f);
            if (--windup > 0) {
                return;
            }
            float yaw = getYRot() * Mth.DEG_TO_RAD;
            Vec3 mouth = position().add(-Mth.sin(yaw) * 0.9, 0.4, Mth.cos(yaw) * 0.9);
            Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth);
            Vec3 shot = aim.normalize().add(getRandom().triangle(0, 0.06), getRandom().triangle(0, 0.06), getRandom().triangle(0, 0.06));
            SmallFireball fireball = new SmallFireball(level, SalamanderEntity.this, shot);
            fireball.setPos(mouth);
            level.addFreshEntity(fireball);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1f, 1.3f);
            nextSpitAt = level.getGameTime() + WildRules.SPIT_COOLDOWN_TICKS;
            windup = 0;
        }
    }
}
