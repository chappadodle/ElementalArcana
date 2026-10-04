package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.GolemRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.FireField;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.spell.Tremors;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * An Elemental Golem (see the Golems spec): a heavy, slow guardian of its element's lands. It
 * ignores the dormant and its own element's kin, but a mage of another element who comes within 12
 * blocks is a trespasser, and anyone who hurts it a foe. Near its target it raises both arms (the
 * warning) and slams them down: damage of its element all round where they land, and its element's
 * effect: Fire leaves burning ground, Water throws foes back soaked, Wind throws them into the air,
 * Earth sends a Tremor at its target. Drawn by client/GolemRenderer, which reads the raised arms
 * from the slam count kept here.
 */
public class GolemEntity extends Monster implements ElementalOrb {
    /** Goes up by one at each slam's start: the client starts its swing when it changes. */
    private static final EntityDataAccessor<Integer> SLAMS = SynchedEntityData.defineId(GolemEntity.class, EntityDataSerializers.INT);

    private final Element element;
    /** Client: the tick its current swing began (or a long time ago). */
    private int swingStart = -1000;

    public GolemEntity(EntityType<? extends GolemEntity> type, Level level, Element element) {
        super(type, level);
        this.element = element;
        this.xpReward = GolemRules.EXPERIENCE;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, GolemRules.HEALTH)
                .add(Attributes.ARMOR, GolemRules.ARMOR)
                .add(Attributes.MOVEMENT_SPEED, GolemRules.SPEED)
                .add(Attributes.ATTACK_DAMAGE, GolemRules.SLAM_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, GolemRules.KNOCKBACK_RESISTANCE)
                .add(Attributes.FOLLOW_RANGE, 24)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public Element element() {
        return element;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SLAMS, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (SLAMS.equals(key) && level().isClientSide()) {
            swingStart = tickCount;
        }
    }

    /** Client: ticks since the current swing began (arms rising, then the blow), or a large number. */
    public float swingTicks(float partialTicks) {
        return tickCount - swingStart + partialTicks;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SlamGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isTrespasser));
    }

    /** A mage of another element near enough to be in its territory. The dormant are no threat, kin are kin. */
    private boolean isTrespasser(LivingEntity target) {
        if (!(target instanceof Player player) || player.distanceTo(this) > GolemRules.TERRITORY) {
            return false;
        }
        MagicData data = MagicAttachments.get(player);
        return data.isAwakened() && !data.holdsFamily(element);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1f, 0.6f);
    }

    @Override
    protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) {
        return switch (element) {
            case WATER -> SoundEvents.DROWNED_HURT_WATER;
            case WIND -> SoundEvents.BREEZE_HURT;
            default -> SoundEvents.IRON_GOLEM_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 1.2f;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f + random.nextFloat() * 0.1f;
    }

    /** The arms go up: everyone sees the warning. */
    private void raise() {
        entityData.set(SLAMS, entityData.get(SLAMS) + 1);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.6f, 1.6f);
    }

    /** The blow: damage of its element all round where its arms land, and its element's effect. */
    private void slam(LivingEntity target) {
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = Vec3.directionFromRotation(0, getYRot());
        Vec3 at = position().add(look.scale(GolemRules.SLAM_AHEAD));
        double radius = GolemRules.SLAM_RADIUS;
        float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, 2, radius),
                e -> e != this && e.isAlive() && SpellTargets.canAffect(this, e) && e.position().distanceTo(at) <= radius + e.getBbWidth() / 2)) {
            SpellDamage.hurtMultiHit(victim, SpellDamage.source(level, element, this, this), damage);
            effect(level, victim, at);
        }
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, getY() - 0.5, at.z));
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, getY() + 0.1, at.z, 40, radius / 2, 0.1, radius / 2, 0.2);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.4f, 0.7f);
        switch (element) {
            case FIRE -> {
                FireField.spawn(level, new Vec3(at.x, getY(), at.z), radius, 80, this);
                level.sendParticles(ParticleTypes.LAVA, at.x, getY() + 0.3, at.z, 12, radius / 2, 0.1, radius / 2, 0);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1f, 0.7f);
            }
            case WATER -> {
                level.sendParticles(ParticleTypes.SPLASH, at.x, getY() + 0.3, at.z, 60, radius / 2, 0.3, radius / 2, 0.3);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
            case WIND -> {
                level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, at.x, getY() + 0.5, at.z, 1, 0, 0, 0, 0);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 1.2f, 0.7f);
            }
            default -> Tremors.start(this, target.position().subtract(position()), 1f, e -> e != this && SpellTargets.canAffect(this, e));
        }
    }

    /** Its element's mark on one it struck. */
    private void effect(ServerLevel level, LivingEntity victim, Vec3 at) {
        double hold = Math.max(0, 1 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        switch (element) {
            case FIRE -> victim.igniteForSeconds(4);
            case WATER -> {
                Vec3 away = victim.position().subtract(at);
                away = new Vec3(away.x, 0, away.z);
                away = away.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, getYRot()) : away.normalize();
                victim.setDeltaMovement(away.scale(1.4 * hold).add(0, 0.4 * hold, 0));
                victim.hurtMarked = true;
                victim.addEffect(new MobEffectInstance(ModContent.WET, 200));
            }
            case WIND -> {
                victim.setDeltaMovement(victim.getDeltaMovement().add(0, 1.1 * hold, 0));
                victim.hurtMarked = true;
                victim.addEffect(new MobEffectInstance(ModContent.AIRBORNE, 40));
            }
            default -> {
            }
        }
    }

    /** Walks to its target; within reach, raises its arms and, after the warning, slams them down. */
    private final class SlamGoal extends Goal {
        private int windup;
        private int cooldown;
        private int repath;

        SlamGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return windup > 0 || canUse();
        }

        @Override
        public void stop() {
            windup = 0;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target != null) {
                getLookControl().setLookAt(target, 30f, 30f);
            }
            if (windup > 0) {
                getNavigation().stop();
                if (--windup == 0 && target != null) {
                    slam(target);
                    cooldown = GolemRules.SLAM_COOLDOWN_TICKS;
                }
                return;
            }
            if (cooldown > 0) {
                cooldown--;
            }
            if (target == null) {
                return;
            }
            if (distanceTo(target) <= GolemRules.REACH && cooldown <= 0) {
                windup = GolemRules.WINDUP_TICKS;
                raise();
            } else if (--repath <= 0) {
                repath = 10;
                getNavigation().moveTo(target, 1.0);
            }
        }
    }
}
