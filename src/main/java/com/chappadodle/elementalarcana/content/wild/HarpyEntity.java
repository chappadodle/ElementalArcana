package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.WildRules;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Gale Harpy (see the Creatures of the Wild II spec): a winged hunter of the heights. It circles
 * high over its prey; now and then it dives screeching, snatches them in its talons, climbs six
 * blocks and lets go; or it hangs in the air and beats its wings at them, a gust that throws them
 * back (and Airborne if they're already off the ground). Hurting it while it carries you makes it
 * let go sooner.
 */
public class HarpyEntity extends Monster implements FlyingAnimal {
    private static final EntityDataAccessor<Boolean> CARRYING = SynchedEntityData.defineId(HarpyEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GUST = SynchedEntityData.defineId(HarpyEntity.class, EntityDataSerializers.INT);

    private long nextSnatchAt;
    private long nextGustAt;
    @Nullable
    private LivingEntity carried;
    private int carriedTicks;
    private float carryDamage;
    private double carryTopY;

    public HarpyEntity(EntityType<? extends HarpyEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 10;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.ATTACK_DAMAGE, 3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CARRYING, false);
        builder.define(GUST, 0);
    }

    public boolean isCarrying() {
        return entityData.get(CARRYING);
    }

    /** Ticks into a gust's windup (0 when it isn't gusting), for the model. */
    public int gustTicks() {
        return entityData.get(GUST);
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    protected float getFlyingSpeed() {
        return 0.045f;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SnatchGoal());
        goalSelector.addGoal(2, new GustGoal());
        goalSelector.addGoal(3, new CircleGoal());
        goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level) || carried == null) {
            return;
        }
        LivingEntity prey = carried;
        boolean gone = !prey.isAlive() || prey instanceof Player player && (player.isCreative() || player.isSpectator());
        if (gone || WildRules.snatchBroken(++carriedTicks, carryDamage) || getY() >= carryTopY) {
            release(level);
            return;
        }
        // Up it goes, the prey held under its talons (no wandering off while it carries them).
        getNavigation().stop();
        getMoveControl().setWantedPosition(getX(), carryTopY + 1, getZ(), 1.0);
        Vec3 hold = position().add(0, -prey.getBbHeight() - 0.15, 0);
        prey.setDeltaMovement(hold.subtract(prey.position()).scale(0.6));
        prey.hurtMarked = true;
        prey.resetFallDistance();
        if (tickCount % 4 == 0) {
            playSound(SoundEvents.PHANTOM_FLAP, 1f, 1.3f);
        }
    }

    private void grab(LivingEntity prey) {
        carried = prey;
        carriedTicks = 0;
        carryDamage = 0;
        carryTopY = getY() + WildRules.SNATCH_LIFT;
        entityData.set(CARRYING, true);
        doHurtTarget(prey);
        playSound(SoundEvents.PARROT_HURT, 1.6f, 0.6f);
    }

    /** Lets go: the fall does the rest. */
    private void release(ServerLevel level) {
        carried = null;
        entityData.set(CARRYING, false);
        nextSnatchAt = level.getGameTime() + WildRules.SNATCH_COOLDOWN_TICKS;
        playSound(SoundEvents.PARROT_AMBIENT, 1.6f, 0.6f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && carried != null) {
            carryDamage += amount;
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PARROT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PARROT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PARROT_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.55f + getRandom().nextFloat() * 0.1f;
    }

    private boolean ready(long readyAt) {
        return WildRules.ready(readyAt, level().getGameTime());
    }

    /** It circles high over its prey, 7 to 11 blocks out. */
    private class CircleGoal extends Goal {
        private double angle;

        CircleGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return carried == null && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            angle = target == null ? 0 : Math.atan2(getZ() - target.getZ(), getX() - target.getX());
            setAggressive(true);
        }

        @Override
        public void stop() {
            setAggressive(false);
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            angle += 0.035;
            double radius = 9 + Math.sin(tickCount * 0.04) * 2;
            getMoveControl().setWantedPosition(target.getX() + Math.cos(angle) * radius, target.getY() + 6,
                    target.getZ() + Math.sin(angle) * radius, 1.0);
            getLookControl().setLookAt(target, 30f, 30f);
        }
    }

    /** The dive: straight at its prey, screeching; within reach, it grabs them. */
    private class SnatchGoal extends Goal {
        private int ticks;

        SnatchGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return carried == null && target != null && target.isAlive() && ready(nextSnatchAt)
                    && distanceTo(target) < 24 && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = getTarget();
            return carried == null && ticks < 80 && target != null && target.isAlive();
        }

        @Override
        public void start() {
            ticks = 0;
            playSound(SoundEvents.PHANTOM_SWOOP, 1.6f, 1.3f);
        }

        @Override
        public void stop() {
            if (carried == null) {
                // Missed: it climbs away and tries again a little later.
                nextSnatchAt = level().getGameTime() + 40;
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            ticks++;
            getMoveControl().setWantedPosition(target.getX(), target.getY() + target.getBbHeight() + 0.3, target.getZ(), 1.9);
            getLookControl().setLookAt(target, 30f, 30f);
            if (distanceTo(target) < 2.0) {
                grab(target);
            }
        }
    }

    /** Hanging in the air, wings spread, then a beat that throws its prey back. */
    private class GustGoal extends Goal {
        private static final int WINDUP = 12;
        private int windup;

        GustGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return carried == null && target != null && target.isAlive() && ready(nextGustAt)
                    && distanceTo(target) > 3 && distanceTo(target) <= WildRules.GUST_RANGE && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            windup = WINDUP;
            entityData.set(GUST, 1);
            playSound(SoundEvents.PHANTOM_FLAP, 2f, 0.8f);
        }

        @Override
        public void stop() {
            windup = 0;
            entityData.set(GUST, 0);
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
            getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.5);
            getLookControl().setLookAt(target, 30f, 30f);
            entityData.set(GUST, WINDUP - windup + 1);
            if (--windup > 0) {
                return;
            }
            Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? getLookAngle() : away.normalize();
            boolean aloft = !target.onGround();
            target.push(away.x * 1.4, 0.35, away.z * 1.4);
            target.hurtMarked = true;
            if (aloft) {
                ElementalReactions.launchAirborne(target, 0.5);
            }
            Vec3 from = position().add(0, getBbHeight() * 0.6, 0);
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0);
            for (int i = 0; i <= 8; i++) {
                Vec3 at = from.lerp(to, i / 8.0);
                level.sendParticles(ModContent.WIND_STREAK.get(), at.x, at.y, at.z, 1, 0.15, 0.15, 0.15, 0.02);
            }
            level.sendParticles(ParticleTypes.GUST, to.x, to.y, to.z, 1, 0, 0, 0, 0);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1f, 0.9f);
            nextGustAt = level.getGameTime() + WildRules.GUST_COOLDOWN_TICKS;
        }
    }
}
