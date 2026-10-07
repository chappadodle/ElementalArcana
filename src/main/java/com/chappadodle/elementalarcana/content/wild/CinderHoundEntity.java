package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.NetherCreatureRules;
import com.chappadodle.elementalarcana.content.Attunement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * A Cinder Hound (see the Creatures of the Nether spec): a lean hound of cinders that hunts the hot
 * wastes in packs. It runs its prey down and leaps at it, and its bite sets it alight; hurt, it howls,
 * and the pack within 16 blocks comes for whoever hurt it. Fire and lava don't touch it; water and
 * rain hurt it. It's always Attuned to Fire.
 */
public class CinderHoundEntity extends Monster {
    private long nextHowlAt;

    public CinderHoundEntity(EntityType<? extends CinderHoundEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        setPathfindingMalus(PathType.LAVA, 8f);
        setPathfindingMalus(PathType.DANGER_FIRE, 0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 18)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new LeapAtTargetGoal(this, 0.45f));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        CreatureMagic magic = Attunement.get(this);
        Attunement.attune(this, Element.FIRE, magic != null ? magic.rank() : AttunementRank.ADEPT);
        return result;
    }

    /** Its bite sets its prey alight. */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.igniteForSeconds(NetherCreatureRules.HOUND_BURN_SECONDS);
        }
        return hit;
    }

    /** Hurt, it howls, and its pack comes (HurtByTargetGoal's alert): everyone hears it. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && level() instanceof ServerLevel level && level.getGameTime() >= nextHowlAt
                && source.getEntity() instanceof LivingEntity attacker) {
            nextHowlAt = level.getGameTime() + 60;
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOLF_HOWL, SoundSource.HOSTILE, 1.4f, 0.6f);
            for (CinderHoundEntity hound : level.getEntitiesOfClass(CinderHoundEntity.class,
                    new AABB(blockPosition()).inflate(NetherCreatureRules.HOWL_RADIUS), hound -> hound != this && hound.getTarget() == null)) {
                hound.setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel && tickCount % 20 == 0 && isInWaterRainOrBubble()) {
            hurt(damageSources().drown(), NetherCreatureRules.HOUND_WATER_DAMAGE);
        } else if (level().isClientSide() && getRandom().nextInt(4) == 0) {
            level().addParticle(getRandom().nextInt(3) == 0 ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
                    getRandomX(0.6), getY() + getRandom().nextDouble() * 0.9, getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.WOLF_STEP, 0.15f, 0.8f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? SoundEvents.WOLF_GROWL : SoundEvents.WOLF_PANT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f + getRandom().nextFloat() * 0.1f;
    }
}
