package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.CreatureLevels;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import com.chappadodle.elementalarcana.content.creature.WispSpawner;
import com.chappadodle.elementalarcana.content.mob.MobCasting;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * A Sovereign (see the sanctums spec): a colossal floating mask over a core of its element, the
 * keeper of one of the seals. An Archmage of its element at level 50 or more, it flies (no
 * gravity), hovers about 8 blocks from its target and 3 above, circling slowly, and never strays
 * more than 16 blocks from its seal. It casts its element's spells quickly and its own two
 * (SovereignSpells). At half health it calls two wisps and burns brighter (its second phase). With
 * no one to fight for 20 seconds it goes back to its seal and heals. When it falls, its seal is
 * restored and it leaves its Heart.
 */
public class SovereignEntity extends Monster {
    private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(SovereignEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(SovereignEntity.class, EntityDataSerializers.BOOLEAN);
    private static final double HOVER_DISTANCE = 8;
    private static final double HOVER_HEIGHT = 3.5;
    private static final double SPEED = 0.06;
    private static final double CASTING_SPEED = 0.015;
    /** The stone its Bulwark's shell is drawn with. */
    private static final BlockParticleOption RUBBLE = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE.defaultBlockState());

    private record Pending(long at, Runnable action) {
    }

    private final Element element;
    private final List<Pending> pending = new ArrayList<>();
    private final Set<UUID> bulwark = new HashSet<>();
    @Nullable
    private BlockPos seal;
    /** Where it first appeared: its home when it has no seal (from an egg or a command). */
    @Nullable
    private Vec3 anchor;
    private int calmTicks;

    public SovereignEntity(EntityType<? extends SovereignEntity> type, Level level, Element element) {
        super(type, level);
        this.element = element;
        this.xpReward = 250;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 320)
                .add(Attributes.ARMOR, 10)
                .add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 8);
    }

    /** Its element (a Sovereign's never changes; the Hollow's is its current form). */
    public Element element() {
        return element;
    }

    /** Leashes it to its sanctum's seal (before it's added to the world). */
    public void bindTo(BlockPos seal) {
        this.seal = seal.immutable();
    }

    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    /** Whether it's in its second phase (synced, for its burning eyes). */
    public boolean isEnraged() {
        return entityData.get(ENRAGED);
    }

    protected void setEnraged(boolean enraged) {
        entityData.set(ENRAGED, enraged);
    }

    /** How far it may stray from home, and whether it is held to it (a Sovereign by its seal). */
    protected double leash() {
        return SovereignRules.LEASH;
    }

    protected boolean leashed() {
        return seal != null;
    }

    /** Its level before its rank's bonus, where the zone's is {@code zoneLevel}. */
    protected int baseLevel(int zoneLevel) {
        return SovereignRules.baseLevel(zoneLevel);
    }

    /** What it leaves when it falls: its Heart. */
    protected ItemStack trophy() {
        return SovereignHeartItem.of(element());
    }

    /** Runs {@code action} in {@code ticks} ticks, unless it falls first (its spells' later pulses). */
    public void later(int ticks, Runnable action) {
        pending.add(new Pending(level().getGameTime() + ticks, action));
    }

    /** A guardian of its Bulwark: while any of them lives, it takes a quarter of the damage. */
    public void guardBy(LivingEntity guardian) {
        bulwark.add(guardian.getUUID());
    }

    public boolean hasBulwark() {
        if (!(level() instanceof ServerLevel level)) {
            return false;
        }
        bulwark.removeIf(id -> !(level.getEntity(id) instanceof LivingEntity guardian) || !guardian.isAlive());
        return !bulwark.isEmpty();
    }

    /** The boss bar's title: "Vulkhar, Sovereign of Flame". */
    public Component bossTitle() {
        return Component.translatable("bossbar.elementalarcana.sovereign." + element().name().toLowerCase(Locale.ROOT));
    }

    public BossEvent.BossBarColor barColor() {
        return switch (element()) {
            case FIRE, RADIANCE -> BossEvent.BossBarColor.RED;
            case WATER, ICE -> BossEvent.BossBarColor.BLUE;
            case WIND, LIGHTNING -> BossEvent.BossBarColor.WHITE;
            case EARTH, CRYSTAL -> BossEvent.BossBarColor.GREEN;
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, false);
        builder.define(ENRAGED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new SovereignCastGoal(this));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::inReach));
    }

    /** Anyone in its sanctum (or near it, with no sanctum): kin or not, every mage is tested. */
    private boolean inReach(LivingEntity target) {
        return !leashed() || target.position().distanceTo(home()) <= leash() + 8;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        Attunement.attune(this, element(), AttunementRank.ARCHMAGE);
        CreatureLevels.levelOf(this);
        CreatureLevels.setBaseLevel(this, baseLevel(getData(MagicAttachments.CREATURE_LEVEL)));
        setHealth(getMaxHealth());
        setPersistenceRequired();
        return result;
    }

    private Vec3 home() {
        if (seal != null) {
            return Vec3.atBottomCenterOf(seal).add(0, 1, 0);
        }
        if (anchor == null) {
            anchor = position();
        }
        return anchor;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && tickCount % 2 == 0) {
            boolean casting = goalSelector.getAvailableGoals().stream()
                    .anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof SovereignCastGoal);
            if (casting != isCasting()) {
                entityData.set(CASTING, casting);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) level();
        runPending();
        LivingEntity target = getTarget();
        if (target != null && (!target.isAlive() || target instanceof Player player && (player.isCreative() || player.isSpectator()))) {
            setTarget(null);
            target = null;
        }
        if (target == null) {
            if (++calmTicks == SovereignRules.CALM_TICKS) {
                calm();
            }
        } else {
            calmTicks = 0;
        }
        steer(target);
        if (target != null) {
            face(target);
        }
        if (leashed() && position().distanceTo(home()) > leash() + 8) {
            // Thrown or dragged out of its sanctum: it returns at once.
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 40, 0.5, 1, 0.5, 0.1);
            Vec3 back = home().add(0, 3, 0);
            teleportTo(back.x, back.y, back.z);
        }
        if (tickCount % 10 == 0 && hasBulwark()) {
            level.sendParticles(RUBBLE, getX(), getY(0.5), getZ(), 6, 0.8, 1.2, 0.8, 0.02);
        }
    }

    /** Glides toward where it wants to be: circling its target, or over its seal. */
    private void steer(@Nullable LivingEntity target) {
        Vec3 home = home();
        Vec3 goal;
        if (target != null) {
            double angle = tickCount * 0.012 + getId();
            goal = target.position().add(Math.cos(angle) * HOVER_DISTANCE, HOVER_HEIGHT, Math.sin(angle) * HOVER_DISTANCE);
        } else {
            goal = home.add(0, 3, 0);
        }
        if (leashed()) {
            Vec3 flat = new Vec3(goal.x - home.x, 0, goal.z - home.z);
            if (flat.length() > leash()) {
                flat = flat.normalize().scale(leash());
            }
            goal = new Vec3(home.x + flat.x, Mth.clamp(goal.y, home.y + 1.5, home.y + 10), home.z + flat.z);
        }
        Vec3 toGoal = goal.subtract(position());
        double distance = toGoal.length();
        if (distance < 0.05) {
            return;
        }
        double speed = Math.min(isCasting() ? CASTING_SPEED : SPEED, distance * 0.05);
        // A slow bob, so it never hangs dead still.
        double bob = Math.sin(tickCount * 0.08) * 0.004;
        setDeltaMovement(getDeltaMovement().scale(0.85).add(toGoal.scale(speed / distance)).add(0, bob, 0));
    }

    private void face(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        double dy = target.getEyeY() - getEyeY();
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
        setYRot(Mth.approachDegrees(getYRot(), yaw, 6f));
        setXRot(Mth.approachDegrees(getXRot(), pitch, 4f));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private void runPending() {
        if (pending.isEmpty()) {
            return;
        }
        long now = level().getGameTime();
        List<Runnable> due = new ArrayList<>();
        for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
            Pending next = it.next();
            if (now >= next.at()) {
                it.remove();
                due.add(next.action());
            }
        }
        due.forEach(Runnable::run);
    }

    /** No one to fight: back to full health and its first phase. */
    protected void calm() {
        setHealth(getMaxHealth());
        entityData.set(ENRAGED, false);
        pending.clear();
        bulwark.clear();
    }

    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance()) {
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.91));
        }
        calculateEntityAnimation(false);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.DROWN) || source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }
        if (hasBulwark() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= SovereignRules.BULWARK_DAMAGE;
            if (level() instanceof ServerLevel level) {
                level.sendParticles(RUBBLE, getX(), getY(0.5), getZ(), 12, 0.6, 1, 0.6, 0.08);
                level.playSound(null, getX(), getY(), getZ(), SoundEvents.STONE_HIT, SoundSource.HOSTILE, 1.2f, 0.6f);
            }
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && !isEnraged() && SovereignRules.secondPhase(getHealth(), getMaxHealth()) && level() instanceof ServerLevel level) {
            enrage(level);
        }
        return hurt;
    }

    /** Half health: it cries out, calls two wisps of its element and burns brighter. */
    private void enrage(ServerLevel level) {
        entityData.set(ENRAGED, true);
        for (int i = 0; i < 2; i++) {
            BlockPos at = BlockPos.containing(getX() + getRandom().nextInt(7) - 3, getY(), getZ() + getRandom().nextInt(7) - 3);
            WispEntity wisp = WispSpawner.spawnAt(level, element(), at, MobSpawnType.MOB_SUMMONED);
            if (wisp != null && getTarget() != null) {
                wisp.setTarget(getTarget());
            }
        }
        level.sendParticles(MobCasting.handsParticle(element()), getX(), getY(0.6), getZ(), 80, 1.2, 1.4, 1.2, 0.15);
        level.sendParticles(ParticleTypes.FLASH, getX(), getY(0.6), getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 1.2f);
        Component message = Component.translatable("message.elementalarcana.sovereign.enraged." + element().name().toLowerCase(Locale.ROOT))
                .withStyle(ChatFormatting.RED);
        level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48)).forEach(player -> player.sendSystemMessage(message));
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(MobCasting.handsParticle(element()), getX(), getY(0.6), getZ(), 150, 1.2, 1.6, 1.2, 0.3);
            level.sendParticles(ParticleTypes.FLASH, getX(), getY(0.6), getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(0.6), getZ(), 1, 0, 0, 0, 0);
            if (seal != null && level.getBlockEntity(seal) instanceof SanctumSealBlockEntity block) {
                block.restore();
            }
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ItemStack trophy = trophy();
        ItemEntity dropped = trophy.isEmpty() ? null : spawnAtLocation(trophy);
        if (dropped != null) {
            dropped.setUnlimitedLifetime();
            dropped.setGlowingTag(true);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (element()) {
            case FIRE, RADIANCE -> SoundEvents.BLAZE_AMBIENT;
            case WATER, ICE -> SoundEvents.ELDER_GUARDIAN_AMBIENT;
            case WIND, LIGHTNING -> SoundEvents.BREEZE_IDLE_AIR;
            case EARTH, CRYSTAL -> SoundEvents.WARDEN_AMBIENT;
        };
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return switch (element()) {
            case FIRE, RADIANCE -> SoundEvents.BLAZE_HURT;
            case WATER, ICE -> SoundEvents.ELDER_GUARDIAN_HURT;
            case WIND, LIGHTNING -> SoundEvents.BREEZE_HURT;
            case EARTH, CRYSTAL -> SoundEvents.IRON_GOLEM_HURT;
        };
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 2f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (seal != null) {
            tag.put("seal", NbtUtils.writeBlockPos(seal));
        }
        if (anchor != null) {
            tag.putDouble("anchor_x", anchor.x);
            tag.putDouble("anchor_y", anchor.y);
            tag.putDouble("anchor_z", anchor.z);
        }
        tag.putBoolean("enraged", isEnraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        seal = NbtUtils.readBlockPos(tag, "seal").orElse(null);
        anchor = tag.contains("anchor_x") ? new Vec3(tag.getDouble("anchor_x"), tag.getDouble("anchor_y"), tag.getDouble("anchor_z")) : null;
        entityData.set(ENRAGED, tag.getBoolean("enraged"));
    }

    /** Everyone within {@code radius} it may hurt (see SpellTargets). */
    public List<LivingEntity> foesWithin(double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(radius, radius / 2 + 2, radius),
                e -> e.isAlive() && e != this && e.distanceTo(this) <= radius
                        && SpellTargets.canAffect(this, e));
    }
}
