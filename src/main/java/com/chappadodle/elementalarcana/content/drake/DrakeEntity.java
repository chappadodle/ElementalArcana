package com.chappadodle.elementalarcana.content.drake;

import com.chappadodle.elementalarcana.api.DrakeRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * An Elemental Drake (see the Drakes spec): a wyvern of its element. It soars round its home, hunts
 * players it sees (not its kin, unless they hurt it), swoops to rake them and hovers to breathe its
 * element, lands to rest when it has nothing to hunt, and fights on the ground once badly hurt. It
 * flies by steering its own motion toward where it wants to be (no pathfinding in the air); on the
 * ground it walks like any creature.
 */
public class DrakeEntity extends Monster implements DrakeLike {
    public enum State { SOARING, HUNTING, SWOOPING, CLIMBING, HOVERING, BREATHING, LANDING, RESTING, GROUNDED }

    /** No breath, the warning (jaws open, glowing), or the breath itself. */
    public static final int BREATH_NONE = 0;
    public static final int BREATH_WINDUP = 1;
    public static final int BREATH_ON = 2;

    private static final EntityDataAccessor<Byte> STATE = SynchedEntityData.defineId(DrakeEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> BREATH = SynchedEntityData.defineId(DrakeEntity.class, EntityDataSerializers.BYTE);
    private static final double MOUTH_AHEAD = 3.4;
    private static final double MOUTH_UP = 1.3;

    private final Element element;
    @Nullable
    private BlockPos home;
    private double soarHeight;
    private double soarAngle;
    private int stateTicks;
    private int calmTicks;
    private int breathTicks;
    private long nextSwoopAt;
    private long nextBreathAt;
    private long nextClawAt;
    private Vec3 climbTo = Vec3.ZERO;
    private Vec3 hoverAt = Vec3.ZERO;
    // The client's smoothed pose.
    private float breathOpen;
    private float breathOpenO;
    private float pitch;
    private float pitchO;
    private float bank;
    private float bankO;
    private float wingBeatO;

    public DrakeEntity(EntityType<? extends DrakeEntity> type, Level level, Element element) {
        super(type, level);
        this.element = element;
        this.xpReward = 80;
        this.soarHeight = DrakeRules.soarHeight(getRandom().nextDouble());
        this.soarAngle = getRandom().nextDouble() * Math.PI * 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DrakeRules.MAX_HEALTH)
                .add(Attributes.ARMOR, DrakeRules.ARMOR)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, DrakeRules.SIGHT)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ATTACK_DAMAGE, DrakeRules.CLAW_DAMAGE);
    }

    @Override
    public Element element() {
        return element;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, (byte) State.SOARING.ordinal());
        builder.define(BREATH, (byte) BREATH_NONE);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isPrey));
    }

    /** Players it hunts unprovoked: not its kin (a mage of its element's family), and not too far from home. */
    private boolean isPrey(LivingEntity target) {
        return target instanceof Player player && !MagicAttachments.get(player).holdsFamily(element)
                && (home == null || target.position().distanceTo(homeVec()) <= DrakeRules.LEASH);
    }

    public State state() {
        return State.values()[Mth.clamp(entityData.get(STATE), 0, State.values().length - 1)];
    }

    private void setState(State next) {
        entityData.set(STATE, (byte) next.ordinal());
        stateTicks = 0;
        setNoGravity(flies(next));
        if (next != State.BREATHING && breathPhase() != BREATH_NONE && next != State.GROUNDED) {
            entityData.set(BREATH, (byte) BREATH_NONE);
        }
    }

    private static boolean flies(State state) {
        return state != State.RESTING && state != State.GROUNDED;
    }

    public int breathPhase() {
        return entityData.get(BREATH);
    }

    /** Whether it holds its flying pose (wings out), as the client sees it. */
    @Override
    public boolean isFlyingPose() {
        return flies(state());
    }

    @Override
    public boolean isResting() {
        return state() == State.RESTING;
    }

    @Override
    public boolean isClimbing() {
        State state = state();
        return state == State.CLIMBING || state == State.SWOOPING && getDeltaMovement().y > 0.1 || getDeltaMovement().y > 0.2;
    }

    /** How far its jaws are open for a breath, 0 to 1. */
    @Override
    public float breathOpen(float partialTick) {
        return Mth.lerp(partialTick, breathOpenO, breathOpen);
    }

    /** Its body's pitch in flight, degrees (nose down positive). */
    @Override
    public float bodyPitch(float partialTick) {
        return Mth.lerp(partialTick, pitchO, pitch);
    }

    /** Its roll as it turns, degrees. */
    @Override
    public float bank(float partialTick) {
        return Mth.lerp(partialTick, bankO, bank);
    }

    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    private Vec3 homeVec() {
        BlockPos at = home != null ? home : blockPosition();
        return Vec3.atBottomCenterOf(at);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (home == null) {
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockPosition());
            home = ground;
        }
        CreatureLevels.setBaseLevel(this, CreatureLevels.zoneLevelAt(level.getLevel(), blockPosition()) + DrakeRules.LEVEL_BONUS);
        setHealth(getMaxHealth());
        setPersistenceRequired();
        setNoGravity(true);
        return result;
    }

    // ---- Thinking ----

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        stateTicks++;
        LivingEntity target = validTarget();
        if (DrakeRules.grounded(getHealth(), getMaxHealth()) && state() != State.GROUNDED) {
            setState(State.GROUNDED);
            roar(level, 1.2f);
        }
        long now = level.getGameTime();
        switch (state()) {
            case SOARING -> soar(target);
            case HUNTING -> hunt(level, target, now);
            case SWOOPING -> swoop(level, target, now);
            case CLIMBING -> {
                fly(climbTo, DrakeRules.HUNT_SPEED);
                if (stateTicks > 30) {
                    setState(target != null ? State.HUNTING : State.SOARING);
                }
            }
            case HOVERING -> hover(level, target);
            case BREATHING -> breathe(level, target, now, true);
            case LANDING -> land(target);
            case RESTING -> rest(level, target);
            case GROUNDED -> ground(level, target, now);
        }
    }

    @Nullable
    private LivingEntity validTarget() {
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target.level() != level() || target instanceof Player player && (player.isCreative() || player.isSpectator())
                || home != null && target.position().distanceTo(homeVec()) > DrakeRules.LEASH + 16)) {
            setTarget(null);
            return null;
        }
        return target;
    }

    private void soar(@Nullable LivingEntity target) {
        if (target != null) {
            calmTicks = 0;
            setState(State.HUNTING);
            return;
        }
        if (++calmTicks > DrakeRules.CALM_TICKS) {
            calmTicks = 0;
            setState(State.LANDING);
            return;
        }
        soarAngle += DrakeRules.SOAR_SPEED / DrakeRules.SOAR_RADIUS;
        Vec3 center = homeVec();
        double x = center.x + Math.cos(soarAngle) * DrakeRules.SOAR_RADIUS;
        double z = center.z + Math.sin(soarAngle) * DrakeRules.SOAR_RADIUS;
        // Its height over its home, or over the ground beneath if that's higher (a peak in its way).
        fly(new Vec3(x, Math.max(center.y, groundY(x, z)) + soarHeight, z), DrakeRules.SOAR_SPEED);
    }

    private void hunt(ServerLevel level, @Nullable LivingEntity target, long now) {
        if (target == null) {
            setState(State.SOARING);
            return;
        }
        if (stateTicks > 20 && DrakeRules.ready(nextBreathAt, now)) {
            hoverAt = hoverPoint(target);
            setState(State.HOVERING);
            return;
        }
        if (stateTicks > 20 && DrakeRules.ready(nextSwoopAt, now) && distanceTo(target) > 6) {
            setState(State.SWOOPING);
            roar(level, 1.4f);
            return;
        }
        double angle = tickCount * 0.035 + getId();
        Vec3 around = target.position().add(Math.cos(angle) * 14, 8 + Math.sin(tickCount * 0.05) * 2, Math.sin(angle) * 14);
        fly(leashed(around), DrakeRules.HUNT_SPEED);
    }

    private void swoop(ServerLevel level, @Nullable LivingEntity target, long now) {
        if (target == null) {
            startClimb(null);
            return;
        }
        fly(target.position().add(0, target.getBbHeight() * 0.5, 0), DrakeRules.SWOOP_SPEED);
        if (distanceTo(target) <= DrakeRules.SWOOP_REACH) {
            claw(level, target);
            nextSwoopAt = now + DrakeRules.SWOOP_COOLDOWN_TICKS;
            startClimb(target);
        } else if (stateTicks > 80) {
            nextSwoopAt = now + DrakeRules.SWOOP_COOLDOWN_TICKS / 2;
            startClimb(target);
        }
    }

    private void startClimb(@Nullable LivingEntity from) {
        Vec3 away = from == null ? getLookAngle() : position().subtract(from.position());
        away = new Vec3(away.x, 0, away.z);
        away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
        climbTo = leashed(position().add(away.scale(12)).add(0, 10, 0));
        setState(State.CLIMBING);
    }

    private void hover(ServerLevel level, @Nullable LivingEntity target) {
        if (target == null) {
            setState(State.SOARING);
            return;
        }
        if (stateTicks % 20 == 0) {
            hoverAt = hoverPoint(target);
        }
        fly(hoverAt, DrakeRules.HUNT_SPEED);
        if (position().distanceTo(hoverAt) < 2.5 || stateTicks > 80) {
            setState(State.BREATHING);
            breathTicks = 0;
            entityData.set(BREATH, (byte) BREATH_WINDUP);
            roar(level, 1.0f);
        }
    }

    /** Where to hover to breathe on {@code target}: HOVER_DISTANCE off on the side it's on, a little above. */
    private Vec3 hoverPoint(LivingEntity target) {
        Vec3 from = position().subtract(target.position());
        from = new Vec3(from.x, 0, from.z);
        from = from.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : from.normalize();
        return leashed(target.position().add(from.scale(DrakeRules.HOVER_DISTANCE)).add(0, DrakeRules.HOVER_HEIGHT, 0));
    }

    /** The warning, then the breath: on the wing (hovering in place) or on the ground. */
    private void breathe(ServerLevel level, @Nullable LivingEntity target, long now, boolean flying) {
        if (flying) {
            setDeltaMovement(getDeltaMovement().scale(0.7));
        }
        if (target != null) {
            faceTarget(target);
        }
        breathTicks++;
        if (breathTicks == DrakeRules.WINDUP_TICKS) {
            entityData.set(BREATH, (byte) BREATH_ON);
            level.playSound(null, getX(), getY(), getZ(), breathSound(), SoundSource.HOSTILE, 2f, 0.8f);
        }
        if (breathPhase() == BREATH_ON && breathTicks % DrakeRules.BREATH_HIT_TICKS == 0) {
            DrakeBreath.hit(level, this, mouth(), facing(target));
        }
        if (breathTicks >= DrakeRules.WINDUP_TICKS + DrakeRules.BREATH_TICKS) {
            setXRot(0f);
            entityData.set(BREATH, (byte) BREATH_NONE);
            nextBreathAt = now + DrakeRules.BREATH_COOLDOWN_TICKS;
            breathTicks = 0;
            if (flying) {
                setState(target != null ? State.HUNTING : State.SOARING);
            }
        }
    }

    private void land(@Nullable LivingEntity target) {
        if (target != null) {
            setState(State.HUNTING);
            return;
        }
        Vec3 spot = homeVec();
        fly(spot.add(0, 0.5, 0), DrakeRules.SOAR_SPEED * 0.7);
        if (onGround() || position().distanceTo(spot) < 2 || stateTicks > 400) {
            setState(State.RESTING);
        }
    }

    private void rest(ServerLevel level, @Nullable LivingEntity target) {
        setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
        if (tickCount % 20 == 0 && getHealth() < getMaxHealth()) {
            heal(2f);
        }
        boolean disturbed = level.getNearestPlayer(getX(), getY(), getZ(), DrakeRules.WAKE_RADIUS,
                entity -> entity instanceof Player player && !player.isSpectator() && !player.isCreative()) != null;
        if (target != null || disturbed || stateTicks > DrakeRules.REST_TICKS) {
            takeOff(level, target);
        }
    }

    private void takeOff(ServerLevel level, @Nullable LivingEntity target) {
        setState(target != null ? State.HUNTING : State.SOARING);
        setDeltaMovement(getDeltaMovement().add(0, 0.7, 0));
        roar(level, 1.2f);
    }

    /** Too hurt to fly: it walks, rakes and breathes on the ground. */
    private void ground(ServerLevel level, @Nullable LivingEntity target, long now) {
        if (breathTicks > 0 || breathPhase() != BREATH_NONE) {
            getNavigation().stop();
            breathe(level, target, now, false);
            return;
        }
        if (target == null) {
            getNavigation().stop();
            return;
        }
        getLookControl().setLookAt(target, 30f, 30f);
        double distance = distanceTo(target);
        if (distance > 3) {
            getNavigation().moveTo(target, 1.0);
        } else {
            getNavigation().stop();
        }
        if (distance <= 3.6 && DrakeRules.ready(nextClawAt, now)) {
            claw(level, target);
            nextClawAt = now + 30;
        } else if (distance <= DrakeRules.BREATH_RANGE - 1 && DrakeRules.ready(nextBreathAt, now)) {
            breathTicks = 0;
            entityData.set(BREATH, (byte) BREATH_WINDUP);
            roar(level, 1.0f);
            breathe(level, target, now, false);
        }
    }

    private void claw(ServerLevel level, LivingEntity target) {
        target.hurt(damageSources().mobAttack(this), DrakeRules.CLAW_DAMAGE);
        Vec3 away = target.position().subtract(position());
        away = new Vec3(away.x, 0, away.z);
        away = away.lengthSqr() < 1.0e-4 ? getLookAngle() : away.normalize();
        target.setDeltaMovement(target.getDeltaMovement().add(away.x * 1.1, 0.4, away.z * 1.1));
        target.hurtMarked = true;
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.5f, 0.9f);
    }

    private void roar(ServerLevel level, float pitch) {
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2.5f, 1.3f * pitch);
    }

    private SoundEvent breathSound() {
        return switch (element) {
            case FIRE -> SoundEvents.BLAZE_SHOOT;
            case ICE -> SoundEvents.POWDER_SNOW_BREAK;
            case LIGHTNING -> SoundEvents.TRIDENT_THUNDER.value();
            case WATER -> SoundEvents.GENERIC_SPLASH;
            default -> SoundEvents.BREEZE_WIND_CHARGE_BURST.value();
        };
    }

    // ---- Moving ----

    /** Steers toward {@code goal} at up to {@code speed} blocks a tick, turning to face where it flies. */
    private void fly(Vec3 goal, double speed) {
        Vec3 to = goal.subtract(position());
        double distance = to.length();
        Vec3 want = distance < 1.0e-3 ? Vec3.ZERO : to.scale(Math.min(speed, distance * 0.25) / distance);
        Vec3 motion = getDeltaMovement().lerp(want, 0.08);
        setDeltaMovement(motion);
        double flat = motion.horizontalDistance();
        if (flat > 0.03) {
            float yaw = (float) (Mth.atan2(motion.z, motion.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 6f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    /** Turns to its prey, and aims its head (its pitch, which the clients see) at it. */
    private void faceTarget(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 10f));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth());
        float pitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
        setXRot(Mth.approachDegrees(getXRot(), Mth.clamp(pitch, -60f, 70f), 8f));
    }

    /** Where its breath comes from: its mouth, ahead of its body. */
    public Vec3 mouth() {
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return position().add(-Mth.sin(yaw) * MOUTH_AHEAD, MOUTH_UP, Mth.cos(yaw) * MOUTH_AHEAD);
    }

    /** Which way it breathes: at its prey if it has one, else ahead and a little down. */
    private Vec3 facing(@Nullable LivingEntity target) {
        if (target != null) {
            Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(mouth());
            if (to.lengthSqr() > 1.0e-4) {
                return to.normalize();
            }
        }
        float yaw = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), -0.35, Mth.cos(yaw)).normalize();
    }

    /** A point pulled back within LEASH of home. */
    private Vec3 leashed(Vec3 point) {
        if (home == null) {
            return point;
        }
        Vec3 center = homeVec();
        Vec3 flat = new Vec3(point.x - center.x, 0, point.z - center.z);
        if (flat.length() > DrakeRules.LEASH) {
            flat = flat.normalize().scale(DrakeRules.LEASH);
        }
        return new Vec3(center.x + flat.x, point.y, center.z + flat.z);
    }

    private double groundY(double x, double z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }

    @Override
    public void travel(Vec3 input) {
        if (isNoGravity() && isControlledByLocalInstance()) {
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.96));
            calculateEntityAnimation(false);
            return;
        }
        super.travel(input);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    // ---- The client's pose, wing beats and breath ----

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) {
            return;
        }
        breathOpenO = breathOpen;
        pitchO = pitch;
        bankO = bank;
        int phase = breathPhase();
        float wantOpen = phase == BREATH_ON ? 1f : phase == BREATH_WINDUP ? 0.7f : 0f;
        breathOpen += (wantOpen - breathOpen) * 0.25f;
        double dy = getY() - yo;
        pitch += ((float) Mth.clamp(-dy * 70, -35, 35) - pitch) * 0.15f;
        float turn = Mth.wrapDegrees(getYRot() - yRotO);
        bank += (Mth.clamp(-turn * 4f, -30f, 30f) - bank) * 0.15f;
        if (isFlyingPose()) {
            float rate = isClimbing() ? 0.45f : 0.22f;
            float beat = Mth.sin(tickCount * rate);
            if (wingBeatO > -0.3f && beat <= -0.3f && !isSilent()) {
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, getSoundSource(), 1.2f, 1.3f, false);
            }
            wingBeatO = beat;
        }
        if (phase != BREATH_NONE) {
            DrakeBreath.particles(this, element, mouth(), Vec3.directionFromRotation(getXRot(), getYRot()), 1f, phase == BREATH_ON);
        }
    }

    // ---- Being hurt, dying ----

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && state() == State.RESTING && level() instanceof ServerLevel level) {
            takeOff(level, source.getEntity() instanceof LivingEntity attacker ? attacker : null);
        }
        return hurt;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModDrakes.scale(element), 2 + getRandom().nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.essence(element), 2 + getRandom().nextInt(3)));
        if (element == Element.WATER && getRandom().nextFloat() < 0.1f) {
            // Tide drakes don't nest: now and then one leaves an egg.
            spawnAtLocation(new ItemStack(ModDrakes.eggItem(element)));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isResting() ? null : SoundEvents.ENDER_DRAGON_AMBIENT;
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
        return 1.35f + getRandom().nextFloat() * 0.1f;
    }

    @Override
    protected float getSoundVolume() {
        return 2.5f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("home", NbtUtils.writeBlockPos(home));
        }
        tag.putDouble("soar_height", soarHeight);
        tag.putByte("state", entityData.get(STATE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        home = NbtUtils.readBlockPos(tag, "home").orElse(null);
        if (tag.contains("soar_height")) {
            soarHeight = tag.getDouble("soar_height");
        }
        State saved = State.values()[Mth.clamp(tag.getByte("state"), 0, State.values().length - 1)];
        // Mid-attack states don't survive a reload: back to the sky (or the ground if it's down).
        setState(saved == State.RESTING || saved == State.GROUNDED ? saved : State.SOARING);
    }
}
