package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.DrakeRidingRules;
import com.chappadodle.elementalarcana.api.DrakeRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ElementalEssenceItem;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A drake raised from an egg (see the Drake Riding spec): it belongs to whoever was near when it
 * hatched, follows them, sits when told, and fights beside them (bites, and from a juvenile on, its
 * breath). It grows from a hatchling (a quarter of a drake's size) to a juvenile after a day and an
 * adult after three, faster when fed its element's Essence or meat. A saddled adult carries its
 * owner: walking, and in the air flying where they look (forward faster, back to a hover, jump to
 * climb), breathing when they press the cast key, tiring as it flies. A rider's client moves it (as
 * a horse's does); the server only checks and shows it.
 */
public class TamedDrakeEntity extends TamableAnimal implements DrakeLike {
    private static final EntityDataAccessor<Byte> STAGE = SynchedEntityData.defineId(TamedDrakeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(TamedDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLYING = SynchedEntityData.defineId(TamedDrakeEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> BREATH = SynchedEntityData.defineId(TamedDrakeEntity.class, EntityDataSerializers.BYTE);
    private static final ResourceLocation STAGE_HEALTH = ElementalArcana.id("drake_stage_health");

    private final Element element;
    private int growth;
    private boolean stageApplied;
    private long nextBreathAt;
    private int breathTicks;
    @Nullable
    private Vec3 breathAim;
    // The rider's client's flight.
    private boolean riderClimb;
    private int stamina = DrakeRidingRules.MAX_STAMINA;
    // The client's smoothed pose.
    private float breathOpen;
    private float breathOpenO;
    private float pitch;
    private float pitchO;
    private float bank;
    private float bankO;

    public TamedDrakeEntity(EntityType<? extends TamedDrakeEntity> type, Level level, Element element) {
        super(type, level);
        this.element = element;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, DrakeRules.MAX_HEALTH)
                .add(Attributes.ARMOR, DrakeRules.ARMOR)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.ATTACK_DAMAGE, 8)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    public Element element() {
        return element;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STAGE, (byte) 0);
        builder.define(SADDLED, false);
        builder.define(FLYING, false);
        builder.define(BREATH, (byte) DrakeEntity.BREATH_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 10f, 3f));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8f));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    // ---- Growing up ----

    public int stage() {
        return entityData.get(STAGE);
    }

    public boolean isAdult() {
        return stage() >= 2;
    }

    public int growth() {
        return growth;
    }

    /** Sets how grown it is (and so its stage, size and health; the first time, whatever the stage). */
    public void setGrowth(int growth) {
        this.growth = Math.max(0, growth);
        int stage = DrakeRidingRules.stage(this.growth);
        if (stage != stage() || !stageApplied) {
            stageApplied = true;
            entityData.set(STAGE, (byte) stage);
            applyStage(stage);
        }
    }

    private void applyStage(int stage) {
        AttributeInstance health = getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            float share = getHealth() / getMaxHealth();
            health.addOrUpdateTransientModifier(new AttributeModifier(STAGE_HEALTH, DrakeRidingRules.health(stage) - DrakeRules.MAX_HEALTH,
                    AttributeModifier.Operation.ADD_VALUE));
            setHealth(getMaxHealth() * share);
        }
        refreshDimensions();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STAGE.equals(key)) {
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(DrakeRidingRules.scale(stage()));
    }

    /** How big it's drawn, by its stage. */
    public float renderScale() {
        return DrakeRidingRules.scale(stage());
    }

    // ---- Owner, food, saddle, riding ----

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target instanceof AbstractVillager || target instanceof IronGolem) {
            return false;
        }
        if (target instanceof OwnableEntity pet && owner.getUUID().equals(pet.getOwnerUUID())) {
            return false;
        }
        return SpellTargets.canAffect(owner, target);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isOwnedBy(player)) {
            return InteractionResult.PASS;
        }
        boolean server = !level().isClientSide();
        int food = stack.getItem() instanceof ElementalEssenceItem essence && essence.element() == element ? DrakeRidingRules.ESSENCE_GROWTH
                : stack.is(ItemTags.MEAT) ? DrakeRidingRules.MEAT_GROWTH : 0;
        if (food > 0 && (!isAdult() || getHealth() < getMaxHealth())) {
            if (server) {
                setGrowth(growth + food);
                heal(DrakeRidingRules.FEED_HEAL);
                stack.consume(1, player);
                ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.6), getZ(), 8, 0.4, 0.4, 0.4, 0);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1f, 0.8f);
            }
            return InteractionResult.sidedSuccess(!server);
        }
        if (stack.is(ModDrakes.SADDLE.get()) && isAdult() && !isSaddled()) {
            if (server) {
                entityData.set(SADDLED, true);
                stack.consume(1, player);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.HORSE_SADDLE, SoundSource.NEUTRAL, 1f, 0.8f);
            }
            return InteractionResult.sidedSuccess(!server);
        }
        if (stack.isEmpty() && player.isShiftKeyDown()) {
            if (server) {
                setOrderedToSit(!isOrderedToSit());
                setInSittingPose(isOrderedToSit());
                navigation.stop();
                setTarget(null);
            }
            return InteractionResult.sidedSuccess(!server);
        }
        if (stack.isEmpty() && isAdult() && isSaddled() && !isVehicle()) {
            if (server) {
                setOrderedToSit(false);
                setInSittingPose(false);
                player.startRiding(this);
                if (player instanceof net.minecraft.server.level.ServerPlayer rider) {
                    MagicTriggers.fire(rider, "drake_ride", element.name().toLowerCase(java.util.Locale.ROOT), 1);
                }
            }
            return InteractionResult.sidedSuccess(!server);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isSaddled() {
        return entityData.get(SADDLED);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return isSaddled() && getFirstPassenger() instanceof Player rider && isOwnedBy(rider) ? rider : null;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        setYRot(rider.getYRot());
        yRotO = getYRot();
        setXRot(rider.getXRot() * 0.5f);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 input) {
        return new Vec3(rider.xxa * 0.5f, 0, rider.zza);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    /** The rider's client says whether its rider holds jump (climb or take off). */
    public void setRiderClimb(boolean climb) {
        this.riderClimb = climb;
    }

    public int stamina() {
        return stamina;
    }

    public boolean isFlying() {
        return entityData.get(FLYING);
    }

    @Override
    public void travel(Vec3 input) {
        LivingEntity rider = getControllingPassenger();
        if (rider == null || !isControlledByLocalInstance()) {
            super.travel(input);
            return;
        }
        boolean flying = !onGround() || riderClimb && stamina >= DrakeRidingRules.TAKE_OFF_STAMINA;
        stamina = DrakeRidingRules.stamina(stamina, flying && !onGround(), onGround());
        if (!flying) {
            super.travel(input);
            return;
        }
        Vec3 look = rider.getLookAngle();
        double speed = input.z > 0 ? DrakeRidingRules.FLY_SPEED : input.z < 0 ? DrakeRidingRules.HOVER_SPEED : DrakeRidingRules.CRUISE_SPEED;
        Vec3 want = look.scale(speed);
        if (onGround()) {
            want = new Vec3(want.x, DrakeRidingRules.TAKE_OFF, want.z);
        } else if (riderClimb && stamina > 0) {
            want = want.add(0, DrakeRidingRules.CLIMB, 0);
        }
        if (stamina <= 0) {
            // Spent: it glides down.
            want = new Vec3(want.x, Math.min(want.y, -DrakeRidingRules.SINK), want.z);
        }
        Vec3 motion = getDeltaMovement().lerp(want, 0.1);
        setDeltaMovement(motion);
        move(MoverType.SELF, motion);
        calculateEntityAnimation(false);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0, dimensions.height() * 0.92, -0.2 * scale);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && !onGround() && passenger instanceof Player rider) {
            rider.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0));
        }
    }

    // ---- Breath ----

    /** The rider's cast key: a breath along their look, if it's grown and rested. */
    public void riderBreath(Player rider) {
        if (!isAdult() || breathPhase() != DrakeEntity.BREATH_NONE || level().getGameTime() < nextBreathAt) {
            return;
        }
        startBreath();
    }

    private void startBreath() {
        breathTicks = 0;
        entityData.set(BREATH, (byte) DrakeEntity.BREATH_WINDUP);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.NEUTRAL, 1.5f, 1.5f);
    }

    public int breathPhase() {
        return entityData.get(BREATH);
    }

    private void tickBreath(ServerLevel level) {
        if (breathPhase() == DrakeEntity.BREATH_NONE) {
            return;
        }
        breathTicks++;
        if (breathTicks == DrakeRidingRules.RIDER_BREATH_WINDUP) {
            entityData.set(BREATH, (byte) DrakeEntity.BREATH_ON);
        }
        if (breathPhase() == DrakeEntity.BREATH_ON && breathTicks % DrakeRules.BREATH_HIT_TICKS == 0) {
            LivingEntity owner = getOwner();
            DrakeBreath.hit(level, this, element, mouth(), aim(), target -> target != this && target != owner
                    && !(target instanceof OwnableEntity pet && owner != null && owner.getUUID().equals(pet.getOwnerUUID()))
                    && (owner == null || SpellTargets.canAffect(owner, target)));
        }
        if (breathTicks >= DrakeRidingRules.RIDER_BREATH_WINDUP + DrakeRidingRules.RIDER_BREATH_TICKS) {
            entityData.set(BREATH, (byte) DrakeEntity.BREATH_NONE);
            nextBreathAt = level.getGameTime() + (isVehicle() ? DrakeRidingRules.RIDER_BREATH_COOLDOWN : DrakeRidingRules.FIGHT_BREATH_COOLDOWN);
        }
    }

    /** Where it breathes: where its rider looks, else at its target, else ahead. */
    private Vec3 aim() {
        if (getControllingPassenger() instanceof Player rider) {
            return rider.getLookAngle();
        }
        LivingEntity target = getTarget();
        if (target != null) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth());
            if (to.lengthSqr() > 1.0e-4) {
                return to.normalize();
            }
        }
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    /** Its mouth: ahead of its body, scaled with it. */
    public Vec3 mouth() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        float scale = renderScale();
        return position().add(-Mth.sin(yaw) * 3.4 * scale, 1.3 * scale, Mth.cos(yaw) * 3.4 * scale);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel level) {
            if (tickCount % 20 == 0 && !isOrderedToSit()) {
                setGrowth(growth + 20);
            }
            entityData.set(FLYING, isVehicle() && !onGround());
            tickBreath(level);
            LivingEntity target = getTarget();
            if (stage() >= 1 && target != null && !isVehicle() && breathPhase() == DrakeEntity.BREATH_NONE
                    && level.getGameTime() >= nextBreathAt && distanceTo(target) < 8 * renderScale() + 2) {
                startBreath();
            }
            return;
        }
        breathOpenO = breathOpen;
        pitchO = pitch;
        bankO = bank;
        int phase = breathPhase();
        breathOpen += ((phase == DrakeEntity.BREATH_ON ? 1f : phase == DrakeEntity.BREATH_WINDUP ? 0.7f : 0f) - breathOpen) * 0.25f;
        double dy = getY() - yo;
        pitch += ((float) Mth.clamp(-dy * 70, -35, 35) - pitch) * 0.15f;
        float turn = Mth.wrapDegrees(getYRot() - yRotO);
        bank += (Mth.clamp(-turn * 4f, -30f, 30f) - bank) * 0.15f;
        if (phase != DrakeEntity.BREATH_NONE) {
            DrakeBreath.particles(this, element, mouth(), Vec3.directionFromRotation(getXRot(), getYRot()), renderScale(),
                    phase == DrakeEntity.BREATH_ON);
        }
    }

    // ---- DrakeLike ----

    @Override
    public boolean isFlyingPose() {
        return isFlying() || !onGround() && stage() >= 1 && !isInWater();
    }

    @Override
    public boolean isResting() {
        return isInSittingPose();
    }

    @Override
    public boolean isClimbing() {
        return getDeltaMovement().y > 0.15;
    }

    @Override
    public float breathOpen(float partialTick) {
        return Mth.lerp(partialTick, breathOpenO, breathOpen);
    }

    @Override
    public float bodyPitch(float partialTick) {
        return Mth.lerp(partialTick, pitchO, pitch);
    }

    @Override
    public float bank(float partialTick) {
        return Mth.lerp(partialTick, bankO, bank);
    }

    // ---- The rest ----

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isInSittingPose() ? null : SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_GROWL;
    }

    @Override
    public float getVoicePitch() {
        return 1.3f + (2 - stage()) * 0.35f + getRandom().nextFloat() * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6f + stage() * 0.6f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("growth", growth);
        tag.putBoolean("saddled", isSaddled());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SADDLED, tag.getBoolean("saddled"));
        growth = tag.getInt("growth");
        int stage = DrakeRidingRules.stage(growth);
        entityData.set(STAGE, (byte) stage);
        applyStage(stage);
        stageApplied = true;
    }
}
