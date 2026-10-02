package com.chappadodle.elementalarcana.content.tower;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A mage tower's people (see the mage towers spec): the Acolyte, and the Magister who rules the
 * tower. Both are illager-like casters Attuned to the tower's element (an Acolyte an Adept or now
 * and then a Magus, the Magister an Archmage), keep their distance and cast that element's spells,
 * raising their arms as they do. The Magister also blinks away when hurt, calls two wisps once
 * below half health, and when it dies the tower's heart goes dark and it leaves a Guardian Core.
 */
public class TowerMageEntity extends AbstractIllager {
    private static final EntityDataAccessor<Byte> ELEMENT = SynchedEntityData.defineId(TowerMageEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(TowerMageEntity.class, EntityDataSerializers.BOOLEAN);
    private static final int BLINK_COOLDOWN_TICKS = 80;

    private final boolean magister;
    @Nullable
    private BlockPos heart;
    private long nextBlinkAt;
    private boolean calledWisps;

    public TowerMageEntity(EntityType<? extends TowerMageEntity> type, Level level, boolean magister) {
        super(type, level);
        this.magister = magister;
        this.xpReward = magister ? 50 : 10;
    }

    public static AttributeSupplier.Builder createAcolyteAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    public static AttributeSupplier.Builder createMagisterAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.SCALE, 1.15);
    }

    public boolean isMagister() {
        return magister;
    }

    public Element element() {
        return Element.values()[Mth.clamp(entityData.get(ELEMENT), 0, Element.values().length - 1)];
    }

    /** Sets the element (before it's added to the world) and the tower heart it belongs to, if any. */
    public void serve(Element element, @Nullable BlockPos heart) {
        entityData.set(ELEMENT, (byte) element.ordinal());
        this.heart = heart == null ? null : heart.immutable();
    }

    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ELEMENT, (byte) 0);
        builder.define(CASTING, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CastMobSpellGoal(this));
        goalSelector.addGoal(2, new KeepDistanceGoal(this, magister ? 5 : 4, 10));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, AbstractIllager.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isIntruder));
    }

    /** Anyone who isn't kin to the tower's element: a tower is no place to wander into. */
    private boolean isIntruder(LivingEntity target) {
        return target instanceof Player player && !MagicAttachments.get(player).holdsFamily(element());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        if (heart == null && spawnType != MobSpawnType.STRUCTURE) {
            // From an egg or a command: an element of its own.
            entityData.set(ELEMENT, (byte) getRandom().nextInt(Element.values().length));
        }
        AttunementRank rank = magister ? AttunementRank.ARCHMAGE
                : getRandom().nextFloat() < 0.25f ? AttunementRank.MAGUS : AttunementRank.ADEPT;
        Attunement.attune(this, element(), rank);
        setPersistenceRequired();
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 2 == 0) {
            boolean casting = goalSelector.getAvailableGoals().stream()
                    .anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof CastMobSpellGoal);
            if (casting != isCasting()) {
                entityData.set(CASTING, casting);
            }
        }
    }

    @Override
    public IllagerArmPose getArmPose() {
        return isCasting() ? IllagerArmPose.SPELLCASTING : IllagerArmPose.CROSSED;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && magister && isAlive() && level() instanceof ServerLevel level) {
            if (!calledWisps && getHealth() < getMaxHealth() / 2) {
                calledWisps = true;
                callWisps(level);
            }
            if (level.getGameTime() >= nextBlinkAt) {
                nextBlinkAt = level.getGameTime() + BLINK_COOLDOWN_TICKS;
                blink(level);
            }
        }
        return hurt;
    }

    /** Teleports 4 to 8 blocks away, onto open floor at about the same height (the same floor of its tower). */
    private void blink(ServerLevel level) {
        Vec3 from = position();
        for (int tries = 0; tries < 12; tries++) {
            double angle = getRandom().nextDouble() * Math.PI * 2;
            double distance = 4 + getRandom().nextDouble() * 4;
            BlockPos target = BlockPos.containing(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance);
            if (heart != null && target.distSqr(heart) > 6.5 * 6.5 + 9) {
                continue;
            }
            if (level.getBlockState(target.below()).isSolid() && level.noCollision(this, getBoundingBox().move(Vec3.atBottomCenterOf(target).subtract(from)))) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(1.0), getZ(), 30, 0.3, 0.6, 0.3, 0.05);
                teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.8f);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(1.0), getZ(), 30, 0.3, 0.6, 0.3, 0.05);
                return;
            }
        }
    }

    private void callWisps(ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            BlockPos at = blockPosition().offset(getRandom().nextInt(5) - 2, 1 + getRandom().nextInt(2), getRandom().nextInt(5) - 2);
            WispEntity wisp = WispSpawner.spawnAt(level, element(), at, MobSpawnType.MOB_SUMMONED);
            if (wisp != null && getTarget() != null) {
                wisp.setTarget(getTarget());
            }
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.2f, 1f);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (magister && level() instanceof ServerLevel level && heart != null
                && level.getBlockEntity(heart) instanceof TowerHeartBlockEntity tower) {
            tower.conquer();
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (magister) {
            spawnAtLocation(GuardianCoreItem.of(element()));
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean unused) {
    }

    @Override
    public boolean canJoinRaid() {
        return false;
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("element", entityData.get(ELEMENT));
        if (heart != null) {
            tag.put("heart", NbtUtils.writeBlockPos(heart));
        }
        tag.putBoolean("called_wisps", calledWisps);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(ELEMENT, tag.getByte("element"));
        heart = NbtUtils.readBlockPos(tag, "heart").orElse(null);
        calledWisps = tag.getBoolean("called_wisps");
    }
}
