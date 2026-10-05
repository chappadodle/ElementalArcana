package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.WildRules;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Bog Lurker (see the Creatures of the Wild II spec): a broad swamp beast lying in the water with
 * only its eyes and back above it. Someone within 5 blocks of it: it bursts out, bites and holds on,
 * dragging them back toward the water for three seconds (a heavy hit breaks its hold). From the
 * water it spits mud: slowness and a moment's blindness. Slow on land.
 */
public class BogLurkerEntity extends Monster {
    private static final EntityDataAccessor<Boolean> HOLDING = SynchedEntityData.defineId(BogLurkerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SPITTING = SynchedEntityData.defineId(BogLurkerEntity.class, EntityDataSerializers.BOOLEAN);

    private long nextLungeAt;
    private long nextMudAt;
    @Nullable
    private LivingEntity held;
    private int heldTicks;
    /** The water it lunged from, to drag its prey back to. */
    @Nullable
    private Vec3 lair;

    public BogLurkerEntity(EntityType<? extends BogLurkerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 10;
        setPathfindingMalus(PathType.WATER, 0f);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.12f, 0.15f, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 20);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36)
                .add(Attributes.ARMOR, 4)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, WildRules.LUNGE_BITE)
                .add(Attributes.FOLLOW_RANGE, 16);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOLDING, false);
        builder.define(SPITTING, false);
    }

    public boolean isHolding() {
        return entityData.get(HOLDING);
    }

    public boolean isSpitting() {
        return entityData.get(SPITTING);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new LungeGoal());
        goalSelector.addGoal(2, new MudSpitGoal());
        // While it holds its prey, the hold is the threat: no more bites.
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return held == null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return held == null && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(6, new RandomSwimmingGoal(this, 0.8, 160));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> distanceTo(target) <= WildRules.SPIT_MUD_RANGE));
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), travelVector);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        // Lying in wait: it rises until only its eyes and back break the surface.
        if (isInWater() && getTarget() == null && held == null && getFluidHeight(FluidTags.WATER) > getBbHeight() * 0.75) {
            setDeltaMovement(getDeltaMovement().add(0, 0.02, 0));
        }
        if (held != null) {
            tickHold(level);
        }
    }

    /** Holding on: its prey dragged after it, toward the water; let go after three seconds. */
    private void tickHold(ServerLevel level) {
        LivingEntity prey = held;
        if (prey == null) {
            return;
        }
        boolean gone = !prey.isAlive() || prey instanceof Player player && (player.isCreative() || player.isSpectator());
        if (gone || WildRules.holdBroken(++heldTicks, 0) || prey.distanceTo(this) > 4) {
            letGo();
            return;
        }
        // Back to the water it came from, its prey dragged after it (toward the lurker if it came from land).
        Vec3 toward;
        if (lair != null) {
            if (!isInWater()) {
                getNavigation().moveTo(lair.x, lair.y, lair.z, 1.2);
            }
            toward = lair;
        } else {
            toward = position();
        }
        Vec3 drag = toward.subtract(prey.position()).multiply(1, 0, 1);
        double length = drag.length();
        drag = length < 0.5 ? Vec3.ZERO : drag.scale(Math.min(0.3, length) / length);
        prey.setDeltaMovement(drag.x, prey.isInWater() ? -0.06 : Math.min(prey.getDeltaMovement().y, 0.0), drag.z);
        prey.hurtMarked = true;
        if (!prey.isInWater()) {
            prey.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 3, false, false));
        }
        if (heldTicks % 10 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, prey.getX(), prey.getY(0.3), prey.getZ(), 8, 0.3, 0.1, 0.3, 0.1);
        }
    }

    private void letGo() {
        held = null;
        entityData.set(HOLDING, false);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && held != null && WildRules.holdBroken(0, amount)) {
            letGo();
            playSound(SoundEvents.FROG_HURT, 1.4f, 0.5f);
        }
        return hurt;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FROG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FROG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FROG_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.45f + getRandom().nextFloat() * 0.1f;
    }

    /** It bursts out at its prey; within reach, it bites and holds on. */
    private class LungeGoal extends Goal {
        private int ticks;

        LungeGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return held == null && target != null && target.isAlive() && WildRules.ready(nextLungeAt, level().getGameTime())
                    && distanceTo(target) <= WildRules.LUNGE_RANGE && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return held == null && ticks < 20 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = getTarget();
            if (target == null) {
                return;
            }
            ticks = 0;
            lair = isInWater() ? position() : null;
            Vec3 toward = target.position().subtract(position()).multiply(1, 0, 1);
            toward = toward.lengthSqr() < 1.0e-4 ? getLookAngle() : toward.normalize();
            // Out of the water it needs the height to clear the bank.
            setDeltaMovement(toward.x * 1.1, isInWater() ? 0.7 : 0.45, toward.z * 1.1);
            hasImpulse = true;
            nextLungeAt = level().getGameTime() + WildRules.LUNGE_COOLDOWN_TICKS;
            playSound(SoundEvents.FROG_LONG_JUMP, 1.6f, 0.5f);
            if (level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SPLASH, getX(), getY(0.5), getZ(), 20, 0.6, 0.2, 0.6, 0.2);
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
            getLookControl().setLookAt(target, 30f, 30f);
            // A pounce: it keeps driving at its prey through the leap, so it carries over a bank once clear of it.
            if (ticks < 12 && !onGround()) {
                Vec3 toward = target.position().subtract(position()).multiply(1, 0, 1);
                if (toward.lengthSqr() > 1.0e-4) {
                    toward = toward.normalize();
                    setDeltaMovement(toward.x * 0.6, getDeltaMovement().y, toward.z * 0.6);
                }
            }
            if ((distanceTo(target) < 2.6 || isWithinMeleeAttackRange(target)) && doHurtTarget(target)) {
                held = target;
                heldTicks = 0;
                entityData.set(HOLDING, true);
                playSound(SoundEvents.FROG_EAT, 1.6f, 0.5f);
            }
        }
    }

    /** From the water, its throat swells; then a gob of mud: slowness and a moment's blindness. */
    private class MudSpitGoal extends Goal {
        private static final int WINDUP = 10;
        private int windup;

        MudSpitGoal() {
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return held == null && isInWater() && target != null && target.isAlive() && WildRules.ready(nextMudAt, level().getGameTime())
                    && distanceTo(target) > WildRules.LUNGE_RANGE && distanceTo(target) <= WildRules.SPIT_MUD_RANGE && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            windup = WINDUP;
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
            Vec3 from = getEyePosition();
            Vec3 to = target.getEyePosition();
            BlockParticleOption mud = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState());
            for (int i = 0; i <= 10; i++) {
                Vec3 at = from.lerp(to, i / 10.0);
                level.sendParticles(mud, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.02);
            }
            if (hasLineOfSight(target)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), BogLurkerEntity.this);
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0), BogLurkerEntity.this);
                level.sendParticles(mud, to.x, to.y, to.z, 16, 0.25, 0.25, 0.25, 0.1);
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 1.2f, 0.6f);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.MUD_BREAK, SoundSource.HOSTILE, 1f, 0.8f);
            nextMudAt = level.getGameTime() + WildRules.MUD_COOLDOWN_TICKS;
        }
    }
}
