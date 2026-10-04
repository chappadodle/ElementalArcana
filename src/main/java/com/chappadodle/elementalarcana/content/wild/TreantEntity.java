package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.WildRules;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A Thornwood Treant (see the Creatures of the Wild spec): asleep it stands among the trees as one
 * of them, still and silent. Someone who isn't its kin coming within 6 blocks, or a hit, wakes it:
 * it walks after them, sweeps its branch-arms, and now and then stamps to burst roots up under its
 * target (Rooted: held fast for two seconds). Fire burns it badly; it heals in sunlight on the
 * earth; with no one to chase it walks back and takes root again.
 */
public class TreantEntity extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(TreantEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STAMP = SynchedEntityData.defineId(TreantEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> WOOD = SynchedEntityData.defineId(TreantEntity.class, EntityDataSerializers.INT);

    /** The tree it passes for, from its forest: its bark and leaves, and the logs it leaves. */
    public enum Wood {
        OAK(Items.OAK_LOG, Items.OAK_SAPLING),
        BIRCH(Items.BIRCH_LOG, Items.BIRCH_SAPLING),
        SPRUCE(Items.SPRUCE_LOG, Items.SPRUCE_SAPLING),
        DARK_OAK(Items.DARK_OAK_LOG, Items.DARK_OAK_SAPLING);

        private final Item log;
        private final Item sapling;

        Wood(Item log, Item sapling) {
            this.log = log;
            this.sapling = sapling;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        static Wood of(Holder<Biome> biome, boolean coin) {
            if (biome.is(Biomes.DARK_FOREST)) {
                return DARK_OAK;
            }
            if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
                return BIRCH;
            }
            if (biome.is(Biomes.TAIGA) || biome.is(Biomes.OLD_GROWTH_PINE_TAIGA) || biome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
                    || biome.is(Biomes.SNOWY_TAIGA)) {
                return SPRUCE;
            }
            // A plain forest grows oaks with a birch here and there.
            return coin ? BIRCH : OAK;
        }
    }

    @Nullable
    private BlockPos home;
    private int calmTicks;
    private long nextRootAt;
    @Nullable
    private LivingEntity rootTarget;

    public TreantEntity(EntityType<? extends TreantEntity> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 80)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, WildRules.TREANT_GRASP)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AWAKE, false);
        builder.define(STAMP, 0);
        builder.define(WOOD, 0);
    }

    public Wood wood() {
        Wood[] woods = Wood.values();
        return woods[Math.floorMod(entityData.get(WOOD), woods.length)];
    }

    public void setWood(Wood wood) {
        entityData.set(WOOD, wood.ordinal());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> isAwake() && !isKin(target)));
    }

    /** The Earth family's mages walk past it unbothered. */
    private static boolean isKin(LivingEntity target) {
        return target instanceof Player player && MagicAttachments.get(player).holdsFamily(Element.EARTH);
    }

    public boolean isAwake() {
        return entityData.get(AWAKE);
    }

    /** Ticks into its stamp (0 when it isn't stamping), for the model. */
    public int stampTicks() {
        return entityData.get(STAMP);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        home = blockPosition();
        setWood(Wood.of(level.getBiome(blockPosition()), getRandom().nextInt(5) == 0));
        // A tree faces any way, square to the world.
        setYRot(getRandom().nextInt(4) * 90f);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        return super.finalizeSpawn(level, difficulty, spawnType, spawnData);
    }

    private void wake(@Nullable LivingEntity by) {
        if (by != null && !canAttack(by)) {
            by = null;
        }
        if (isAwake()) {
            if (by != null && getTarget() == null) {
                setTarget(by);
            }
            return;
        }
        entityData.set(AWAKE, true);
        calmTicks = 0;
        if (by != null) {
            setTarget(by);
        }
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_AMBIENT, SoundSource.HOSTILE, 1.2f, 0.5f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()),
                    getX(), getY(0.8), getZ(), 30, 0.8, 0.5, 0.8, 0.05);
        }
    }

    /** Back at its spot with no one to chase: it takes root again. */
    private void settle() {
        entityData.set(AWAKE, false);
        getNavigation().stop();
        setTarget(null);
        setYRot(Math.round(getYRot() / 90f) * 90f);
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    /** Asleep it doesn't think (see isImmobile), so this keeps watch for it and lets it heal. */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel level)) {
            return;
        }
        if (!isAwake()) {
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            if (tickCount % 10 == 0) {
                Player near = level.getNearestPlayer(getX(), getY(), getZ(), WildRules.TREANT_WAKE,
                        entity -> entity instanceof Player player && !player.isSpectator() && !player.isCreative() && !isKin(player));
                if (near != null) {
                    wake(near);
                }
            }
        }
        if (tickCount % 40 == 0 && level.isDay() && level.canSeeSky(blockPosition()) && getHealth() < getMaxHealth()
                && level.getBlockState(blockPosition().below()).is(BlockTags.DIRT)) {
            heal(1f);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || !canAttack(target))) {
            setTarget(null);
            target = null;
        }
        if (target == null) {
            if (++calmTicks > WildRules.TREANT_FORGET_TICKS) {
                BlockPos spot = home != null ? home : blockPosition();
                if (distanceToSqr(Vec3.atBottomCenterOf(spot)) < 2.5) {
                    settle();
                } else if (tickCount % 20 == 0) {
                    getNavigation().moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.8);
                }
            }
        } else {
            calmTicks = 0;
            tickRooting(level, target);
        }
    }

    /** The stamp: a short windup, then roots burst up under its target. */
    private void tickRooting(ServerLevel level, LivingEntity target) {
        int stamp = stampTicks();
        if (stamp > 0) {
            if (stamp >= WildRules.ROOT_WINDUP_TICKS) {
                entityData.set(STAMP, 0);
                if (rootTarget != null && rootTarget.isAlive()) {
                    root(level, rootTarget);
                }
                rootTarget = null;
            } else {
                entityData.set(STAMP, stamp + 1);
            }
            return;
        }
        if (WildRules.ready(nextRootAt, level.getGameTime()) && distanceTo(target) <= WildRules.ROOT_RANGE && hasLineOfSight(target)) {
            nextRootAt = level.getGameTime() + WildRules.ROOT_COOLDOWN_TICKS;
            rootTarget = target;
            entityData.set(STAMP, 1);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_HIT, SoundSource.HOSTILE, 1.5f, 0.5f);
        }
    }

    private void root(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(ModWild.ROOTED, WildRules.ROOTED_TICKS, 0, false, true, true));
        target.hurt(SpellDamage.source(level, Element.EARTH, this, this), WildRules.ROOT_DAMAGE);
        BlockParticleOption roots = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState());
        BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState());
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2 * i / 12;
            double x = target.getX() + Math.cos(angle) * 0.9;
            double z = target.getZ() + Math.sin(angle) * 0.9;
            level.sendParticles(roots, x, target.getY() + 0.3, z, 4, 0.05, 0.4, 0.05, 0.1);
        }
        level.sendParticles(dirt, target.getX(), target.getY() + 0.1, target.getZ(), 30, 0.6, 0.1, 0.6, 0.15);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.5f, 0.6f);
        level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.WOOD_PLACE, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        Element element = SpellDamage.elementOf(source);
        boolean fire = source.is(DamageTypeTags.IS_FIRE) || element != null && element.family() == Element.FIRE;
        boolean hurt = super.hurt(source, WildRules.treantDamage(amount, fire));
        if (hurt && isAlive()) {
            wake(source.getEntity() instanceof LivingEntity attacker ? attacker : null);
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            Vec3 away = target.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0e-4 ? getLookAngle() : away.normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(away.x * 0.9, 0.3, away.z * 0.9));
            target.hurtMarked = true;
        }
        return hit;
    }

    /** Its loot table gives the rest (Essence, sticks, now and then Heartwood); the logs are its own tree's. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(wood().log, 2 + getRandom().nextInt(3)));
        if (getRandom().nextBoolean()) {
            spawnAtLocation(new ItemStack(wood().sapling, 1 + getRandom().nextInt(2)));
        }
    }

    @Override
    public boolean isPushable() {
        return isAwake();
    }

    /** Asleep, it doesn't think or move at all. */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || !isAwake();
    }

    @Override
    public int getMaxHeadYRot() {
        return isAwake() ? 30 : 0;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isAwake() ? SoundEvents.WOOD_STEP : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        playSound(SoundEvents.WOOD_STEP, 0.8f, 0.6f);
    }

    @Override
    public float getVoicePitch() {
        return 0.5f + getRandom().nextFloat() * 0.1f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("awake", isAwake());
        tag.putString("wood", wood().id());
        if (home != null) {
            tag.put("home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AWAKE, tag.getBoolean("awake"));
        for (Wood wood : Wood.values()) {
            if (wood.id().equals(tag.getString("wood"))) {
                setWood(wood);
            }
        }
        home = NbtUtils.readBlockPos(tag, "home").orElse(null);
    }
}
