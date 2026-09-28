package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * One projectile entity for every {@link ProjectileSpell}: it stores which spell fired it
 * (synced, so clients can draw that spell's model and trail) and hands hits back to the spell.
 *
 * <p>It can also be <em>held</em>: floating beside its caster and charging until
 * {@link #release} throws it. A held projectile positions itself from its owner on both
 * sides every tick, so it stays glued to the caster instead of trailing behind over the network.
 */
public class SpellProjectile extends ThrowableProjectile {
    private static final EntityDataAccessor<String> SPELL_ID = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> HELD = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CHARGE_TICKS = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLOT_COUNT = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.FLOAT);
    private static final double AIM_RANGE = 64.0;

    private float power = 1f;
    private int releasedAt;
    // Server-side: piercing, and a release scheduled a few ticks ahead (for rippling volleys).
    private int pierceLeft;
    private int bouncesLeft;
    private final Set<Integer> piercedIds = new HashSet<>();
    private int releaseCountdown = -1;
    @Nullable
    private Vec3 pendingTarget;

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    /** Fires straight away along the caster's look direction. */
    public static <S extends Spell & ProjectileSpell> SpellProjectile launch(CastContext context, S spell) {
        SpellProjectile projectile = create(context, spell);
        Vec3 eye = context.eyePosition();
        projectile.setPos(eye.x, eye.y - 0.1, eye.z);
        projectile.entityData.set(CHARGE, 1f);
        ServerPlayer caster = context.caster();
        projectile.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0f, spell.projectileSpeed(), 0f);
        context.level().addFreshEntity(projectile);
        return projectile;
    }

    /** Summons one projectile floating beside the caster, charging until {@link #release}. */
    public static <S extends Spell & ProjectileSpell> SpellProjectile summonHeld(CastContext context, S spell) {
        return summonHeld(context, spell, 0, 1, spell.chargeTicks());
    }

    /**
     * Summons a held projectile in formation slot {@code slot} of {@code count} (see
     * {@link ProjectileSpell#holdOffset}), taking {@code chargeTicks} to fully charge.
     */
    public static <S extends Spell & ProjectileSpell> SpellProjectile summonHeld(CastContext context, S spell, int slot, int count, int chargeTicks) {
        SpellProjectile projectile = create(context, spell);
        projectile.entityData.set(HELD, true);
        projectile.entityData.set(SLOT, slot);
        projectile.entityData.set(SLOT_COUNT, count);
        projectile.entityData.set(CHARGE_TICKS, chargeTicks);
        projectile.setPos(holdPosition(context.caster(), spell, slot, count, 1f));
        context.level().addFreshEntity(projectile);
        return projectile;
    }

    /**
     * Fires a projectile of {@code spell} from any point with a given velocity, outside of a cast
     * (e.g. a projectile splitting into smaller ones on impact).
     */
    public static <S extends Spell & ProjectileSpell> SpellProjectile shootFrom(Entity owner, S spell, Vec3 position, Vec3 velocity, float power) {
        SpellProjectile projectile = new SpellProjectile(ModContent.SPELL_PROJECTILE.get(), owner.level());
        projectile.setOwner(owner);
        projectile.entityData.set(SPELL_ID, spell.id().toString());
        projectile.entityData.set(CHARGE, 1f);
        projectile.power = power;
        projectile.setPos(position);
        projectile.setDeltaMovement(velocity);
        owner.level().addFreshEntity(projectile);
        return projectile;
    }

    private static SpellProjectile create(CastContext context, Spell spell) {
        SpellProjectile projectile = new SpellProjectile(ModContent.SPELL_PROJECTILE.get(), context.level());
        projectile.setOwner(context.caster());
        projectile.entityData.set(SPELL_ID, spell.id().toString());
        projectile.power = context.power();
        return projectile;
    }

    /** Throws a held projectile at {@code target} after {@code delayTicks} (it keeps floating until then). */
    public void release(Vec3 target, int delayTicks) {
        if (delayTicks <= 0) {
            release(target);
        } else {
            pendingTarget = target;
            releaseCountdown = delayTicks;
        }
    }

    /** Throws a held projectile at {@code target}, locking in its current charge. */
    public void release(Vec3 target) {
        ProjectileSpell spell = spell();
        if (!isHeld() || spell == null) {
            return;
        }
        float charge = charge(0f);
        entityData.set(CHARGE, charge);
        entityData.set(HELD, false);
        releasedAt = tickCount;
        Vec3 direction = target.subtract(position());
        shoot(direction.x, direction.y, direction.z, spell.releaseSpeed(charge), 0f);
        hasImpulse = true;
        spell.onRelease(this);
    }

    /**
     * What the caster is aiming at: the first entity or block under their crosshair, up to 64
     * blocks away. Held projectiles float off to the side, so they aim at this point rather than
     * flying parallel to the caster's view.
     */
    public static Vec3 crosshairTarget(ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        Vec3 end = eye.add(look.scale(AIM_RANGE));
        BlockHitResult block = caster.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        Vec3 blockHit = block.getLocation();
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(caster, eye, blockHit,
                caster.getBoundingBox().expandTowards(look.scale(AIM_RANGE)).inflate(1.0),
                target -> !target.isSpectator() && target.isPickable(), eye.distanceToSqr(blockHit));
        return entity != null ? entity.getEntity().getBoundingBox().getCenter() : blockHit;
    }

    /** Where this held projectile floats for {@code owner} at the given partial tick. */
    public Vec3 holdPosition(Entity owner, ProjectileSpell spell, float partialTick) {
        return holdPosition(owner, spell, entityData.get(SLOT), entityData.get(SLOT_COUNT), partialTick);
    }

    /** Where a held projectile in formation slot {@code slot} of {@code count} floats for {@code owner}. */
    public static Vec3 holdPosition(Entity owner, ProjectileSpell spell, int slot, int count, float partialTick) {
        Vec3 forward = owner.getViewVector(partialTick);
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        if (right.lengthSqr() < 1.0e-4) {
            float yaw = owner.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
            right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        }
        right = right.normalize();
        Vec3 up = right.cross(forward);
        Vec3 offset = spell.holdOffset(slot, count);
        return owner.getEyePosition(partialTick)
                .add(right.scale(offset.x))
                .add(up.scale(offset.y))
                .add(forward.scale(offset.z));
    }

    public boolean isHeld() {
        return entityData.get(HELD);
    }

    /** 0..1: grows while held, then fixed at the value it was released with. */
    public float charge(float partialTick) {
        if (!isHeld()) {
            return entityData.get(CHARGE);
        }
        int chargeTicks = entityData.get(CHARGE_TICKS);
        return chargeTicks <= 0 ? 1f : Math.min(1f, (tickCount + partialTick) / chargeTicks);
    }

    /** How many projectiles are held together in this one's formation. */
    public int formationCount() {
        return entityData.get(SLOT_COUNT);
    }

    /** Moves a held projectile to another formation slot. */
    public void setFormation(int slot, int count) {
        entityData.set(SLOT, slot);
        entityData.set(SLOT_COUNT, count);
    }

    /** Size multiplier for the projectile's model (synced). */
    public float visualScale() {
        return entityData.get(SCALE);
    }

    public void setVisualScale(float scale) {
        entityData.set(SCALE, scale);
    }

    /** Lets the projectile pass through this many more entities (each is hit only once). */
    public void setPierce(int targets) {
        pierceLeft = targets;
    }

    /** Downward pull per tick (synced, so clients predict the arc). 0 = flies straight. */
    public void setGravity(float gravity) {
        entityData.set(GRAVITY, gravity);
    }

    /** Bounces off this many blocks before a block hit counts as an impact. */
    public void setBounces(int bounces) {
        bouncesLeft = bounces;
    }

    /** Makes the projectile pass through {@code entity} without hitting it. */
    public void ignoreEntity(Entity entity) {
        piercedIds.add(entity.getId());
    }

    /** Forgets which entities it already passed through, so it can hit them again (e.g. on the way back). */
    public void resetPierced() {
        piercedIds.clear();
    }

    /** Ticks since it was thrown (or since it was spawned, if it was never held). */
    public int ticksInFlight() {
        return tickCount - releasedAt;
    }

    public float power() {
        return power;
    }

    @Nullable
    public ProjectileSpell spell() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(SPELL_ID));
        return id != null && SpellRegistries.SPELLS.get(id) instanceof ProjectileSpell spell ? spell : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPELL_ID, "");
        builder.define(HELD, false);
        builder.define(CHARGE, 1f);
        builder.define(CHARGE_TICKS, 0);
        builder.define(SLOT, 0);
        builder.define(SLOT_COUNT, 1);
        builder.define(SCALE, 1f);
        builder.define(GRAVITY, 0f);
    }

    @Override
    protected double getDefaultGravity() {
        return entityData.get(GRAVITY);
    }

    @Override
    public void tick() {
        ProjectileSpell spell = spell();
        if (isHeld()) {
            tickHeld(spell);
            return;
        }
        super.tick();
        if (level().isClientSide()) {
            if (spell != null) {
                spell.flightParticles(this);
            }
        } else if (spell == null || tickCount - releasedAt > spell.lifetimeTicks()) {
            discard();
        } else if (!isRemoved()) {
            spell.onTick(this);
        }
    }

    private void tickHeld(@Nullable ProjectileSpell spell) {
        Entity owner = getOwner();
        if (spell == null || owner == null || !owner.isAlive()) {
            if (!level().isClientSide()) {
                discard();
            }
            return;
        }
        setPos(holdPosition(owner, spell, 1f));
        setDeltaMovement(Vec3.ZERO);
        if (!level().isClientSide() && releaseCountdown > 0 && --releaseCountdown == 0 && pendingTarget != null) {
            release(pendingTarget);
            return;
        }
        if (level().isClientSide()) {
            spell.heldParticles(this, charge(0f));
        }
    }

    /** Client-side helper for spell visuals: one particle at a random point within {@code spread}. */
    public void spawnParticleAround(ParticleOptions particle, double spread, Vec3 velocity) {
        level().addParticle(particle,
                getX() + (random.nextDouble() - 0.5) * 2 * spread,
                getY() + (random.nextDouble() - 0.5) * 2 * spread,
                getZ() + (random.nextDouble() - 0.5) * 2 * spread,
                velocity.x, velocity.y, velocity.z);
    }

    // While held, both sides position the projectile themselves; ignore the server's position updates.
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (!isHeld()) {
            super.lerpTo(x, y, z, yRot, xRot, steps);
        }
    }

    /** Reflects off the block face that was hit, losing some speed. */
    private void bounce(BlockHitResult hit) {
        bouncesLeft--;
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 velocity = getDeltaMovement();
        Vec3 reflected = velocity.subtract(normal.scale(2 * velocity.dot(normal))).scale(0.55);
        setDeltaMovement(reflected);
        setPos(hit.getLocation().add(normal.scale(0.05)));
        hasImpulse = true;
    }

    // Vanilla lets a projectile hit its own shooter once it has flown clear of them (arrows shot
    // straight up, returning boomerangs...). A spell never hits its own caster.
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && target != getOwner() && !piercedIds.contains(target.getId());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        ProjectileSpell spell = spell();
        if (spell != null && !level().isClientSide()) {
            spell.onHitEntity(this, result);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        ProjectileSpell spell = spell();
        if (spell != null && !level().isClientSide()) {
            spell.onHitBlock(this, result);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        if (result instanceof BlockHitResult blockHit && bouncesLeft > 0 && !level().isClientSide()) {
            bounce(blockHit);
            return;
        }
        super.onHit(result);
        if (level().isClientSide()) {
            return;
        }
        if (result instanceof EntityHitResult entityHit && pierceLeft > 0) {
            pierceLeft--;
            piercedIds.add(entityHit.getEntity().getId());
        } else {
            discard();
        }
    }

    // A held projectile never saves as held: after a reload it has no caster, so it just expires.
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Spell", entityData.get(SPELL_ID));
        tag.putFloat("Power", power);
        tag.putFloat("Charge", entityData.get(CHARGE));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SPELL_ID, tag.getString("Spell"));
        power = tag.contains("Power") ? tag.getFloat("Power") : 1f;
        entityData.set(CHARGE, tag.contains("Charge") ? tag.getFloat("Charge") : 1f);
    }
}
