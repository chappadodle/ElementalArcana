package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CryptRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.content.tower.KeepDistanceGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A crypt's Revenant (see the Arcane Crypts spec): the crypt's first warden, who would not sleep. A
 * tall skeleton in robes of its element, an Attuned Archmage of the crypt's element (so it has a
 * boss bar and casts that element's spells, keeping its distance). It rises from its tomb over two
 * seconds; struck from close by, it steps through the grave to 6 to 9 blocks away; at half health
 * it raises the dead (the chamber's sealed coffins). It never leaves its chamber, and with no one to
 * fight for 20 seconds it goes back to its tomb and heals. When it falls its grave flame goes out.
 */
public class RevenantEntity extends Monster {
    private static final EntityDataAccessor<Byte> ELEMENT = SynchedEntityData.defineId(RevenantEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(RevenantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RISING = SynchedEntityData.defineId(RevenantEntity.class, EntityDataSerializers.INT);

    @Nullable
    private BlockPos flame;
    @Nullable
    private Vec3 home;
    private long nextStepAt;
    private int calmTicks;
    private boolean raisedDead;

    public RevenantEntity(EntityType<? extends RevenantEntity> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 100)
                .add(Attributes.ARMOR, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.7)
                .add(Attributes.ATTACK_DAMAGE, 6)
                .add(Attributes.SCALE, 1.25);
    }

    public Element element() {
        return Element.values()[Mth.clamp(entityData.get(ELEMENT), 0, Element.values().length - 1)];
    }

    /** Sets its element, its grave flame and its chamber's middle (before it's added to the world). */
    public void serve(Element element, @Nullable BlockPos flame, @Nullable Vec3 home) {
        entityData.set(ELEMENT, (byte) element.ordinal());
        this.flame = flame == null ? null : flame.immutable();
        this.home = home;
        if (home != null) {
            restrictTo(BlockPos.containing(home), (int) CryptRules.LEASH);
        }
    }

    public void startRising() {
        entityData.set(RISING, CryptRules.RISE_TICKS);
    }

    public boolean isRising() {
        return entityData.get(RISING) > 0;
    }

    /** How far through rising it is, 0 (still in the tomb) to 1 (risen). */
    public float riseProgress(float partialTick) {
        int left = entityData.get(RISING);
        return left <= 0 ? 1f : Mth.clamp(1f - (left - partialTick) / CryptRules.RISE_TICKS, 0f, 1f);
    }

    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, (byte) 0);
        builder.define(CASTING, false);
        builder.define(RISING, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CastMobSpellGoal(this));
        goalSelector.addGoal(2, new KeepDistanceGoal(this, 4, 10));
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (flame == null && spawnType != MobSpawnType.STRUCTURE) {
            // From an egg or a command: an element of its own.
            entityData.set(ELEMENT, (byte) getRandom().nextInt(Element.values().length));
        }
        Attunement.attune(this, element(), AttunementRank.ARCHMAGE);
        ItemStack staff = new ItemStack(ModGear.MASTER_STAFF.get());
        staff.set(ModGear.ELEMENT.get(), element());
        setItemSlot(EquipmentSlot.MAINHAND, staff);
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        setPersistenceRequired();
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (isRising() && getRandom().nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SOUL, getRandomX(0.6), getY() + getRandom().nextDouble() * 0.4, getRandomZ(0.6), 0, 0.05, 0);
            }
            return;
        }
        int rising = entityData.get(RISING);
        if (rising > 0) {
            entityData.set(RISING, rising - 1);
            setDeltaMovement(Vec3.ZERO);
            if (rising == 1) {
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 2f, 0.5f);
            }
        }
        if (tickCount % 2 == 0) {
            boolean casting = goalSelector.getAvailableGoals().stream()
                    .anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof CastMobSpellGoal);
            if (casting != isCasting()) {
                entityData.set(CASTING, casting);
            }
        }
    }

    /** Still climbing out of its tomb: no thinking yet. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isRising();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            setTarget(null);
            target = null;
        }
        if (target == null) {
            if (++calmTicks == CryptRules.CALM_TICKS) {
                calm(level);
            }
        } else {
            calmTicks = 0;
        }
        if (home != null && position().distanceTo(home) > CryptRules.LEASH + 4) {
            // Thrown or lured out of its chamber: back through the grave.
            stepTo(level, home);
        }
    }

    /** No one to fight: back to its tomb, whole again. */
    private void calm(ServerLevel level) {
        setHealth(getMaxHealth());
        if (flame != null && level.getBlockEntity(flame) instanceof GraveFlameBlockEntity grave) {
            stepTo(level, grave.tomb());
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isRising()) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && level() instanceof ServerLevel level) {
            Entity attacker = source.getEntity();
            if (!raisedDead && getHealth() <= getMaxHealth() / 2) {
                raisedDead = true;
                raiseDead(level, attacker);
            }
            if (attacker != null && attacker.distanceTo(this) <= CryptRules.GRAVE_STEP_CLOSE && level.getGameTime() >= nextStepAt) {
                nextStepAt = level.getGameTime() + CryptRules.GRAVE_STEP_COOLDOWN_TICKS;
                graveStep(level);
            }
        }
        return hurt;
    }

    /** Half health: it calls the chamber's dead out of their coffins (or, away from a crypt, out of the ground). */
    private void raiseDead(ServerLevel level, @Nullable Entity attacker) {
        LivingEntity target = attacker instanceof LivingEntity living ? living : getTarget();
        if (flame != null && level.getBlockEntity(flame) instanceof GraveFlameBlockEntity grave) {
            grave.raiseDead(target);
        } else {
            for (int i = 0; i < 2; i++) {
                BlockPos at = blockPosition().offset(getRandom().nextInt(7) - 3, 0, getRandom().nextInt(7) - 3);
                CryptDead.raise(level, at, element(), Direction.Plane.HORIZONTAL.getRandomDirection(getRandom()), target);
            }
        }
        level.sendParticles(ParticleTypes.SOUL, getX(), getY(0.6), getZ(), 60, 1.2, 1.2, 1.2, 0.08);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2f, 0.6f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2f, 0.5f);
        Component message = Component.translatable("message.elementalarcana.crypt.raise_dead").withStyle(ChatFormatting.RED);
        level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32)).forEach(player -> player.sendSystemMessage(message));
    }

    /** Sinks into the grave and rises 6 to 9 blocks away, on open floor in its chamber. */
    private void graveStep(ServerLevel level) {
        for (int tries = 0; tries < 16; tries++) {
            double angle = getRandom().nextDouble() * Math.PI * 2;
            double distance = CryptRules.GRAVE_STEP_MIN + getRandom().nextDouble() * (CryptRules.GRAVE_STEP_MAX - CryptRules.GRAVE_STEP_MIN);
            Vec3 to = new Vec3(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance);
            if (home != null && new Vec3(to.x - home.x, 0, to.z - home.z).length() > CryptRules.LEASH) {
                continue;
            }
            for (int dy = 1; dy >= -2; dy--) {
                Vec3 spot = to.add(0, dy, 0);
                BlockPos feet = BlockPos.containing(spot);
                BlockState below = level.getBlockState(feet.below());
                Vec3 standing = new Vec3(spot.x, feet.getY(), spot.z);
                if (below.isFaceSturdy(level, feet.below(), Direction.UP)
                        && level.noCollision(this, getBoundingBox().move(standing.subtract(position())))) {
                    stepTo(level, standing);
                    return;
                }
            }
        }
    }

    private void stepTo(ServerLevel level, Vec3 to) {
        level.sendParticles(ParticleTypes.SOUL, getX(), getY(0.5), getZ(), 30, 0.4, 0.8, 0.4, 0.04);
        level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(0.2), getZ(), 12, 0.4, 0.2, 0.4, 0.02);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5f, 0.7f);
        teleportTo(to.x, to.y, to.z);
        getNavigation().stop();
        level.sendParticles(ParticleTypes.SOUL, getX(), getY(0.5), getZ(), 30, 0.4, 0.8, 0.4, 0.04);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level && flame != null && level.getBlockEntity(flame) instanceof GraveFlameBlockEntity grave) {
            grave.conquer();
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.essence(element()), 3 + getRandom().nextInt(3)));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.7f + getRandom().nextFloat() * 0.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        playSound(SoundEvents.WITHER_SKELETON_STEP, 0.15f, 0.8f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("element", entityData.get(ELEMENT));
        if (flame != null) {
            tag.put("flame", NbtUtils.writeBlockPos(flame));
        }
        if (home != null) {
            tag.putDouble("home_x", home.x);
            tag.putDouble("home_y", home.y);
            tag.putDouble("home_z", home.z);
        }
        tag.putBoolean("raised_dead", raisedDead);
        tag.putInt("rising", entityData.get(RISING));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ELEMENT, tag.getByte("element"));
        flame = NbtUtils.readBlockPos(tag, "flame").orElse(null);
        home = tag.contains("home_x") ? new Vec3(tag.getDouble("home_x"), tag.getDouble("home_y"), tag.getDouble("home_z")) : null;
        if (home != null) {
            restrictTo(BlockPos.containing(home), (int) CryptRules.LEASH);
        }
        raisedDead = tag.getBoolean("raised_dead");
        entityData.set(RISING, tag.getInt("rising"));
    }
}
