package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.AttunementRules;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An elemental wisp (see the wisps spec): a small floating orb of one element, drawn to mana. It is
 * always Attuned to its element (an Adept or, rarely, more), so it casts that element's mob spells
 * from a distance, circling its target. It leaves the dormant and its kin alone, fights other mages
 * on sight, and fights back against anyone who hurts it. A shrine's guardians stay near the shrine.
 */
public class WispEntity extends Monster implements FlyingAnimal, ElementalOrb {
    /** How far a shrine's guardian strays from it. */
    public static final int GUARD_RADIUS = 8;

    private final Element element;
    @Nullable
    private BlockPos home;

    public WispEntity(EntityType<? extends WispEntity> type, Level level, Element element) {
        super(type, level);
        this.element = element;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.xpReward = 5;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12)
                .add(Attributes.FLYING_SPEED, 0.7)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24);
    }

    /** Whether it guards a shrine (a guardian can't be bound as a familiar). */
    public boolean isGuardian() {
        return home != null;
    }

    @Override
    public Element element() {
        return element;
    }

    /** The shrine this wisp guards, if any. */
    @Nullable
    public BlockPos home() {
        return home;
    }

    /** Makes this wisp a guardian of the shrine at {@code shrine}: it keeps near it. */
    public void guard(BlockPos shrine) {
        home = shrine.immutable();
        restrictTo(home, GUARD_RADIUS);
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
        goalSelector.addGoal(2, new WispFightGoal(this));
        goalSelector.addGoal(5, new WispWanderGoal(this));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isRival));
    }

    /** Mages who don't hold this wisp's element family. The dormant are no threat, and kin are kin. */
    private boolean isRival(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return false;
        }
        MagicData data = MagicAttachments.get(player);
        return data.isAwakened() && !data.holdsFamily(element);
    }

    /** Every wisp is Attuned to its element: an Adept, or on the usual roll a Magus or Archmage. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        BlockPos worldSpawn = level.getLevel().getSharedSpawnPos();
        double distance = Math.hypot(getX() - worldSpawn.getX(), getZ() - worldSpawn.getZ());
        AttunementRank rolled = AttunementRules.rollRank(distance, getRandom()::nextDouble);
        Attunement.attune(this, element, rolled == null ? AttunementRank.ADEPT : rolled);
        updateSize();
        return result;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 20 == 1) {
            updateSize();
        }
    }

    /** Greater wisps are bigger (a Magus 1.35 times, an Archmage 1.75 times). */
    private void updateSize() {
        CreatureMagic magic = Attunement.get(this);
        double scale = magic == null ? 1 : switch (magic.rank()) {
            case ADEPT -> 1;
            case MAGUS -> 1.35;
            case ARCHMAGE -> 1.75;
        };
        AttributeInstance size = getAttribute(Attributes.SCALE);
        if (size != null && size.getBaseValue() != scale) {
            size.setBaseValue(scale);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide() && tickCount % 3 == 0) {
            level().addParticle(trailParticle(), getRandomX(0.5), getY() + getBbHeight() * (0.2 + random.nextDouble() * 0.6),
                    getRandomZ(0.5), 0, -0.01, 0);
        }
    }

    private ParticleOptions trailParticle() {
        return switch (element) {
            case FIRE -> ParticleTypes.SMALL_FLAME;
            case WATER -> ParticleTypes.FALLING_WATER;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case WIND -> ModContent.WIND_STREAK.get();
            case EARTH -> new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.ROOTED_DIRT.defaultBlockState());
            case CRYSTAL -> new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.AMETHYST_BLOCK.defaultBlockState());
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case RADIANCE -> ParticleTypes.END_ROD;
        };
    }

    private ParticleOptions burstParticle() {
        return switch (element) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.SPLASH;
            case ICE -> ModContent.FROST_SPARKLE.get();
            case WIND -> ParticleTypes.CLOUD;
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            case CRYSTAL -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState());
            case LIGHTNING -> ParticleTypes.ELECTRIC_SPARK;
            case RADIANCE -> ParticleTypes.END_ROD;
        };
    }

    // ---- an orb of magic: it floats, and it pops when it dies ----

    @Override
    public boolean isFlying() {
        return !onGround();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    /** Water and rain hurt Fire wisps, as they hurt blazes. */
    @Override
    public boolean isSensitiveToWater() {
        return element == Element.FIRE;
    }

    @Override
    public boolean canFreeze() {
        return element != Element.ICE && super.canFreeze();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source.is(DamageTypes.IN_WALL) || source.is(DamageTypeTags.IS_FALL) || super.isInvulnerableTo(source);
    }

    @Override
    protected void tickDeath() {
        if (++deathTime >= 3 && !level().isClientSide() && !isRemoved()) {
            level().broadcastEntityEvent(this, EntityEvent.POOF);
            remove(RemovalReason.KILLED);
        }
    }

    /** On the client, the death event: it pops in a burst of its element instead of the usual smoke. */
    @Override
    public void handleEntityEvent(byte id) {
        if (id != EntityEvent.POOF) {
            super.handleEntityEvent(id);
            return;
        }
        for (int i = 0; i < 18; i++) {
            level().addParticle(burstParticle(), getRandomX(0.6), getRandomY(), getRandomZ(0.6),
                    random.nextGaussian() * 0.08, random.nextGaussian() * 0.08 + 0.04, random.nextGaussian() * 0.08);
        }
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
        return 1.4f + random.nextFloat() * 0.4f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 100;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.put("home", NbtUtils.writeBlockPos(home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "home").ifPresent(this::guard);
    }
}
