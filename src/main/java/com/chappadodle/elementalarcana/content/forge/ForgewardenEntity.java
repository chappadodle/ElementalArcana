package com.chappadodle.elementalarcana.content.forge;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ForgewardenRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.FireField;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.creature.GolemEntity;
import com.chappadodle.elementalarcana.content.mob.MobCasting;
import com.chappadodle.elementalarcana.content.mob.MobSpells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Forgewarden, keeper of a Cinder Forge (see the Cinder Forges spec): a golem of blackstone and
 * magma nearly twice a golem's size, Attuned to Fire. It walks at its foe and slams the floor (a
 * golem's slam, wider: a ring of cinders and burning ground), hurls magma (a Meteor) at a foe
 * farther off, and every so often fire bursts from its seams all round it. At half its health it
 * turns molten: faster, and leaving burning ground behind it. Fire and lava don't touch it, and it
 * keeps to its forge.
 */
public class ForgewardenEntity extends GolemEntity {
    private static final EntityDataAccessor<Boolean> MOLTEN = SynchedEntityData.defineId(ForgewardenEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation MOLTEN_SPEED = ElementalArcana.id("forgewarden_molten");

    @Nullable
    private BlockPos heart;
    private int hurlCooldown = 60;
    private int ventCooldown = 120;
    private int trail;

    public ForgewardenEntity(EntityType<? extends ForgewardenEntity> type, Level level) {
        super(type, level, Element.FIRE);
        this.xpReward = ForgewardenRules.EXPERIENCE;
        // It wades through lava and fire as a strider does.
        setPathfindingMalus(PathType.LAVA, 0f);
        setPathfindingMalus(PathType.DANGER_FIRE, 0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return GolemEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, ForgewardenRules.HEALTH)
                .add(Attributes.ARMOR, ForgewardenRules.ARMOR)
                .add(Attributes.ARMOR_TOUGHNESS, ForgewardenRules.TOUGHNESS)
                .add(Attributes.MOVEMENT_SPEED, ForgewardenRules.SPEED)
                .add(Attributes.ATTACK_DAMAGE, ForgewardenRules.SLAM_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.5);
    }

    /** Client and server: whether it has turned molten (its seams blaze; ForgewardenRenderer). */
    public boolean isMolten() {
        return entityData.get(MOLTEN);
    }

    /** Keeps it to the forge whose heart is at {@code heart}. */
    public void guard(BlockPos heart) {
        this.heart = heart.immutable();
        restrictTo(this.heart, ForgewardenRules.GUARD_RADIUS);
    }

    @Override
    protected double reach() {
        return ForgewardenRules.REACH;
    }

    @Override
    protected double slamRadius() {
        return ForgewardenRules.SLAM_RADIUS;
    }

    @Override
    protected double slamAhead() {
        return 2.0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MOLTEN, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Anyone in its forge is its foe, magic or none (a golem minds only mages of other elements).
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                player -> heart == null || player.blockPosition().closerThan(heart, ForgewardenRules.WAKE_RADIUS)));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        Attunement.attune(this, Element.FIRE, AttunementRank.ADEPT);
        setPersistenceRequired();
        return result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        // Brought without a spawn's setting up (a /summon with data skips it): Attuned now.
        if (Attunement.get(this) == null) {
            Attunement.attune(this, Element.FIRE, AttunementRank.ADEPT);
        }
        boolean molten = ForgewardenRules.molten(getHealth(), getMaxHealth());
        if (molten != isMolten()) {
            melt(level, molten);
        }
        if (hurlCooldown > 0) {
            hurlCooldown--;
        }
        if (ventCooldown > 0) {
            ventCooldown--;
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            double distance = distanceTo(target);
            if (hurlCooldown <= 0 && ForgewardenRules.hurls(distance) && getSensing().hasLineOfSight(target)) {
                hurl(level, target);
                hurlCooldown = ForgewardenRules.HURL_COOLDOWN_TICKS;
            }
            if (ventCooldown <= 0 && distance <= ForgewardenRules.VENT_RADIUS + 1) {
                vent(level);
                ventCooldown = ForgewardenRules.VENT_COOLDOWN_TICKS;
            }
        }
        if (molten && onGround() && ++trail >= ForgewardenRules.TRAIL_TICKS) {
            trail = 0;
            FireField.spawn(level, position(), 1.5, 60, this);
        }
    }

    /** Molten (or cooled again, healed past half): faster, its seams ablaze. */
    private void melt(ServerLevel level, boolean molten) {
        entityData.set(MOLTEN, molten);
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOLTEN_SPEED);
            if (molten) {
                speed.addTransientModifier(new AttributeModifier(MOLTEN_SPEED, ForgewardenRules.MOLTEN_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }
        if (molten) {
            level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 2, getZ(), 30, 1, 1, 1, 0);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.5f);
        }
    }

    /** Magma hurled at a foe far off: a Meteor that comes down on them. */
    private void hurl(ServerLevel level, LivingEntity target) {
        ModSpells.FIREBALL.get().shootForMob(this, MobCasting.aimPoint(target), MobSpells.POWER * 1.5f, true);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    /** Fire from its seams all round it: everyone near burned, set alight and thrown back. */
    private void vent(ServerLevel level) {
        double radius = ForgewardenRules.VENT_RADIUS;
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius, 2, radius),
                e -> e != this && e.isAlive() && SpellTargets.canAffect(this, e) && e.distanceTo(this) <= radius + e.getBbWidth() / 2)) {
            SpellDamage.hurtMultiHit(victim, SpellDamage.source(level, Element.FIRE, this, this), ForgewardenRules.VENT_DAMAGE);
            victim.igniteForSeconds(ForgewardenRules.VENT_BURN_SECONDS);
            Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? Vec3.directionFromRotation(0, getYRot()) : away.normalize();
            double hold = Math.max(0, 1 - victim.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
            victim.setDeltaMovement(victim.getDeltaMovement().add(away.scale(1.2 * hold)).add(0, 0.4 * hold, 0));
            victim.hurtMarked = true;
        }
        for (int i = 0; i < 24; i++) {
            double angle = Math.PI * 2 * i / 24;
            level.sendParticles(ParticleTypes.FLAME, getX() + Math.cos(angle) * 1.5, getY() + 1.5, getZ() + Math.sin(angle) * 1.5, 0,
                    Math.cos(angle), 0.05, Math.sin(angle), 0.4);
        }
        level.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.5, getZ(), 16, 0.8, 0.8, 0.8, 0);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5f, 0.6f);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 1f, 0.5f);
    }

    /** Its Ember Cores: one, and a second on Hard half the time (the rest of its loot is its loot table's). */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModForge.EMBER_CORE.get(),
                ForgewardenRules.cores(level.getDifficulty() == Difficulty.HARD, getRandom().nextDouble())));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (heart != null) {
            tag.put("heart", NbtUtils.writeBlockPos(heart));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "heart").ifPresent(this::guard);
    }
}
