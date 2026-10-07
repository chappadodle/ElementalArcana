package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.NetherCreatureRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.WildRules;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * An Ash Wraith (see the Creatures of the Nether spec): a wraith of soul fire over the soul sand
 * valleys, the Frost Wraith's kin. It circles its prey a few blocks off and casts the fire spells of
 * its rank (it's always Attuned to Fire); once its prey burns, it swoops in, and its touch withers
 * them and puts their fire out in a gust of ash. Hit, it flickers a few blocks away in soul fire;
 * water and ice tear it; fire and lava don't touch it.
 */
public class AshWraithEntity extends Monster implements FlyingAnimal {
    private long nextBlinkAt;
    private long nextTouchAt;

    public AshWraithEntity(EntityType<? extends AshWraithEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 14;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    protected float getFlyingSpeed() {
        return 0.035f;
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
        goalSelector.addGoal(1, new CastMobSpellGoal(this));
        goalSelector.addGoal(2, new TouchGoal());
        goalSelector.addGoal(3, new CircleGoal());
        goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 12f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        // Always Attuned to Fire, at the rank it was born to (an Adept if none).
        CreatureMagic magic = Attunement.get(this);
        Attunement.attune(this, Element.FIRE, magic != null ? magic.rank() : AttunementRank.ADEPT);
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && getRandom().nextInt(3) == 0) {
            level().addParticle(getRandom().nextInt(4) == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.ASH,
                    getRandomX(0.5), getY() + getRandom().nextDouble() * 1.6, getRandomZ(0.5), 0, 0.02, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Element element = SpellDamage.elementOf(source);
        boolean tears = source.is(DamageTypes.DROWN) || source.is(DamageTypeTags.IS_FREEZING)
                || element == Element.WATER || element == Element.ICE;
        boolean hurt = super.hurt(source, NetherCreatureRules.ashWraithDamage(amount, tears));
        if (hurt && isAlive() && level() instanceof ServerLevel level && level.getGameTime() >= nextBlinkAt) {
            nextBlinkAt = level.getGameTime() + WildRules.WRAITH_BLINK_COOLDOWN_TICKS;
            blink(level);
        }
        return hurt;
    }

    /** A flicker: gone in a gust of soul fire, there again a few blocks off in the air. */
    private void blink(ServerLevel level) {
        for (int tries = 0; tries < 10; tries++) {
            double angle = getRandom().nextDouble() * Math.PI * 2;
            double distance = WildRules.WRAITH_BLINK_MIN + getRandom().nextDouble() * (WildRules.WRAITH_BLINK_MAX - WildRules.WRAITH_BLINK_MIN);
            Vec3 to = position().add(Math.cos(angle) * distance, getRandom().nextDouble() * 2, Math.sin(angle) * distance);
            if (level.noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(0.5), getZ(), 12, 0.3, 0.5, 0.3, 0.03);
                teleportTo(to.x, to.y, to.z);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5f, 0.8f);
                return;
            }
        }
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
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4f + getRandom().nextFloat() * 0.1f;
    }

    /** It circles its prey, 5 to 8 blocks off and a little above. */
    private class CircleGoal extends Goal {
        private double angle;

        CircleGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && getTarget().isAlive();
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
            angle += 0.04;
            double radius = 5 + Math.sin(tickCount * 0.05) * 1.5 + 1.5;
            getMoveControl().setWantedPosition(target.getX() + Math.cos(angle) * radius, target.getY() + 2.5,
                    target.getZ() + Math.sin(angle) * radius, 1.0);
            getLookControl().setLookAt(target, 30f, 30f);
        }
    }

    /** Its prey burning: it swoops in, and its touch withers them and puts their fire out in a gust of ash. */
    private class TouchGoal extends Goal {
        private static final int SWOOP_TICKS = 60;
        private int ticks;

        TouchGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && target.isOnFire() && level().getGameTime() >= nextTouchAt;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() != null && getTarget().isAlive() && level().getGameTime() >= nextTouchAt && ticks < SWOOP_TICKS;
        }

        @Override
        public void start() {
            ticks = 0;
            playSound(SoundEvents.VEX_CHARGE, 1.2f, 0.5f);
        }

        @Override
        public void stop() {
            if (ticks >= SWOOP_TICKS) {
                nextTouchAt = level().getGameTime() + 40;
            }
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
            ticks++;
            getMoveControl().setWantedPosition(target.getX(), target.getY(0.6), target.getZ(), 1.6);
            getLookControl().setLookAt(target, 30f, 30f);
            if (distanceTo(target) < 1.8) {
                nextTouchAt = level.getGameTime() + NetherCreatureRules.ASH_TOUCH_COOLDOWN_TICKS;
                target.hurt(SpellDamage.source(level, Element.FIRE, AshWraithEntity.this, AshWraithEntity.this), NetherCreatureRules.ASH_TOUCH_DAMAGE);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, NetherCreatureRules.WITHER_TICKS), AshWraithEntity.this);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, NetherCreatureRules.SLOW_TICKS, 1), AshWraithEntity.this);
                target.clearFire();
                level.sendParticles(ParticleTypes.WHITE_ASH, target.getX(), target.getY(0.6), target.getZ(), 40, 0.4, 0.5, 0.4, 0.02);
                level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY(0.6), target.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5f, 0.6f);
            }
        }
    }
}
