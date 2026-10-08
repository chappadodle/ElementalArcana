package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.FarIslesRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * A Stargazer (see the Far Isles spec): a tall robed figure of the End's outer islands with a face
 * of starlight, always Attuned to Radiance at the rank it was born to. It drifts slowly and leaves
 * you be, unless you strike it or look it in the face (as an enderman would have it; a carved
 * pumpkin hides your eyes). Angered, it hangs a few blocks off casting Radiance's spells, swoops now
 * and then to touch you (Levitation, a moment's lift over the void), and blinks away when hurt.
 */
public class StargazerEntity extends Monster implements FlyingAnimal {
    private static final int BLINK_COOLDOWN_TICKS = 60;
    private static final float TOUCH_DAMAGE = 3f;
    private long nextBlinkAt;
    private long nextTouchAt;

    public StargazerEntity(EntityType<? extends StargazerEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 20;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 40)
                .add(Attributes.FLYING_SPEED, 0.5)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    protected float getFlyingSpeed() {
        return 0.03f;
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
        goalSelector.addGoal(3, new HoverGoal());
        goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, this::staredAtBy));
    }

    /** Whether {@code target} (a player) looks it in the face: near, in sight, eyes on its own, no pumpkin over theirs. */
    private boolean staredAtBy(LivingEntity target) {
        if (!(target instanceof Player player) || player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)
                || distanceTo(player) > FarIslesRules.STARE_RANGE) {
            return false;
        }
        Vec3 view = player.getViewVector(1f).normalize();
        Vec3 toFace = new Vec3(getX() - player.getX(), getEyeY() - player.getEyeY(), getZ() - player.getZ());
        double distance = toFace.length();
        return view.dot(toFace.normalize()) > 1 - 0.025 / distance && player.hasLineOfSight(this);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        // Always Attuned to Radiance, at the rank it was born to (an Adept if none).
        CreatureMagic magic = Attunement.get(this);
        Attunement.attune(this, Element.RADIANCE, magic != null ? magic.rank() : AttunementRank.ADEPT);
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            // A halo of motes turning slowly round its head.
            float angle = tickCount * 0.08f + getRandom().nextInt(3) * Mth.TWO_PI / 3;
            if (getRandom().nextInt(2) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX() + Mth.cos(angle) * 0.6, getY() + 2.95, getZ() + Mth.sin(angle) * 0.6,
                        0, 0, 0);
            }
            if (getRandom().nextInt(6) == 0) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.4), getY() + getRandom().nextDouble() * 2.2, getRandomZ(0.4),
                        0, -0.02, 0);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && level() instanceof ServerLevel level && level.getGameTime() >= nextBlinkAt) {
            nextBlinkAt = level.getGameTime() + BLINK_COOLDOWN_TICKS;
            blink(level);
        }
        return hurt;
    }

    /** A blink: gone in a swirl of starlight, there again a few blocks off. */
    private void blink(ServerLevel level) {
        for (int tries = 0; tries < 10; tries++) {
            double angle = getRandom().nextDouble() * Math.PI * 2;
            double distance = 4 + getRandom().nextDouble() * 4;
            Vec3 to = position().add(Math.cos(angle) * distance, getRandom().nextDouble() * 2, Math.sin(angle) * distance);
            if (level.noCollision(this, getBoundingBox().move(to.subtract(position())))) {
                level.sendParticles(ParticleTypes.END_ROD, getX(), getY(0.5), getZ(), 14, 0.3, 0.8, 0.3, 0.04);
                teleportTo(to.x, to.y, to.z);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1.4f);
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
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_CLUSTER_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return 0.5f + getRandom().nextFloat() * 0.2f;
    }

    /** Angered, it hangs 6 to 9 blocks off and a little above, circling slowly. */
    private class HoverGoal extends Goal {
        private double angle;

        HoverGoal() {
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
            angle += 0.025;
            double radius = 7.5 + Math.sin(tickCount * 0.04) * 1.5;
            getMoveControl().setWantedPosition(target.getX() + Math.cos(angle) * radius, target.getY() + 2,
                    target.getZ() + Math.sin(angle) * radius, 0.9);
            getLookControl().setLookAt(target, 30f, 30f);
        }
    }

    /** Now and then it swoops in, and its touch lifts its foe off their feet (Levitation) with a sting of starlight. */
    private class TouchGoal extends Goal {
        private static final int SWOOP_TICKS = 60;
        private int ticks;

        TouchGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && level().getGameTime() >= nextTouchAt && getRandom().nextInt(40) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return getTarget() != null && getTarget().isAlive() && level().getGameTime() >= nextTouchAt && ticks < SWOOP_TICKS;
        }

        @Override
        public void start() {
            ticks = 0;
            playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.6f);
        }

        @Override
        public void stop() {
            if (ticks >= SWOOP_TICKS) {
                nextTouchAt = level().getGameTime() + FarIslesRules.TOUCH_COOLDOWN_TICKS;
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
            getMoveControl().setWantedPosition(target.getX(), target.getY(0.6), target.getZ(), 1.5);
            getLookControl().setLookAt(target, 30f, 30f);
            if (distanceTo(target) < 2.0) {
                nextTouchAt = level.getGameTime() + FarIslesRules.TOUCH_COOLDOWN_TICKS;
                target.hurt(SpellDamage.source(level, Element.RADIANCE, StargazerEntity.this, StargazerEntity.this), TOUCH_DAMAGE);
                target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, FarIslesRules.TOUCH_LEVITATION_TICKS, 2), StargazerEntity.this);
                level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5), target.getZ(), 20, 0.4, 0.6, 0.4, 0.05);
                level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2f, 1.6f);
            }
        }
    }
}
