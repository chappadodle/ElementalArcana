package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.api.WildRules;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.mob.MobSpells;
import com.chappadodle.elementalarcana.content.spell.PrismBoltSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * A Crystal Crawler (see the Creatures of the Wild II spec): six legs, a back of violet crystals that
 * glow in the dark, and it climbs walls like a spider. From a distance it hunches and flings three
 * crystal shards (prism bolts); up close it bites. When it dies its crystals burst, cutting anyone
 * near.
 */
public class CrystalCrawlerEntity extends Monster {
    private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(CrystalCrawlerEntity.class, EntityDataSerializers.BYTE);
    private static final int CLIMBING = 1;
    private static final int HUNCHED = 2;

    private long nextVolleyAt;

    public CrystalCrawlerEntity(EntityType<? extends CrystalCrawlerEntity> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.FOLLOW_RANGE, 20);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FLAGS, (byte) 0);
    }

    private boolean flag(int bit) {
        return (entityData.get(FLAGS) & bit) != 0;
    }

    private void setFlag(int bit, boolean on) {
        byte flags = entityData.get(FLAGS);
        entityData.set(FLAGS, (byte) (on ? flags | bit : flags & ~bit));
    }

    public boolean isClimbing() {
        return flag(CLIMBING);
    }

    /** Hunched for a volley, for the model. */
    public boolean isHunched() {
        return flag(HUNCHED);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new VolleyGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, false));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            setFlag(CLIMBING, horizontalCollision);
        } else if (getRandom().nextInt(12) == 0) {
            level().addParticle(ParticleTypes.GLOW, getRandomX(0.4), getY(0.9), getRandomZ(0.4), 0, 0.01, 0);
        }
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    @Override
    public void makeStuckInBlock(BlockState state, Vec3 motionMultiplier) {
        if (!state.is(Blocks.COBWEB)) {
            super.makeStuckInBlock(state, motionMultiplier);
        }
    }

    /** Its crystals burst as it dies: shards fly out and cut anyone close. */
    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel level && !isRemoved() && !dead) {
            Vec3 centre = getBoundingBox().getCenter();
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_CLUSTER.defaultBlockState()),
                    centre.x, centre.y, centre.z, 40, 0.5, 0.4, 0.5, 0.25);
            level.sendParticles(ParticleTypes.END_ROD, centre.x, centre.y, centre.z, 12, 0.3, 0.3, 0.3, 0.15);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.5f, 0.8f);
            for (LivingEntity near : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(WildRules.BURST_RADIUS),
                    e -> e != this && !(e instanceof CrystalCrawlerEntity) && e.isAlive() && e.distanceTo(this) <= WildRules.BURST_RADIUS)) {
                SpellDamage.hurtMultiHit(near, SpellDamage.source(level, Element.CRYSTAL, this, this), WildRules.BURST_DAMAGE);
            }
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SPIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SPIDER_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        playSound(SoundEvents.AMETHYST_BLOCK_STEP, 0.3f, 1.4f);
    }

    @Override
    public float getVoicePitch() {
        return 1.4f + getRandom().nextFloat() * 0.2f;
    }

    /** It stops, hunches with its crystals humming, and flings three shards in a fan. */
    private class VolleyGoal extends Goal {
        private static final int WINDUP = 15;
        private int windup;

        VolleyGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && WildRules.ready(nextVolleyAt, level().getGameTime())
                    && distanceTo(target) > 4 && distanceTo(target) <= WildRules.VOLLEY_RANGE && hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 && getTarget() != null && getTarget().isAlive();
        }

        @Override
        public void start() {
            windup = WINDUP;
            setFlag(HUNCHED, true);
            getNavigation().stop();
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.2f);
        }

        @Override
        public void stop() {
            windup = 0;
            setFlag(HUNCHED, false);
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
            PrismBoltSpell bolt = ModSpells.PRISM_BOLT.get();
            Vec3 from = position().add(0, getBbHeight() * 0.8, 0);
            Vec3 aim = target.getBoundingBox().getCenter().subtract(from).normalize();
            for (float offset : WildRules.volleyOffsets()) {
                Vec3 direction = aim.yRot(offset * Mth.DEG_TO_RAD);
                SpellProjectile.shootFrom(CrystalCrawlerEntity.this, bolt, from.add(direction.scale(0.5)),
                        direction.scale(bolt.releaseSpeed(1f)), MobSpells.POWER * WildRules.VOLLEY_POWER);
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.HOSTILE, 1.4f, 1.2f);
            nextVolleyAt = level.getGameTime() + WildRules.VOLLEY_COOLDOWN_TICKS;
        }
    }
}
