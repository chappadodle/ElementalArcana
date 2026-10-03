package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.api.StormeyeRules;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A Stormeye tornado (see StormeyeSpell and the Stormeye spec): an entity, so it moves smoothly for
 * everyone, with its size and element synced; never saved. Each tick on the server it drags
 * whatever its caster may hurt toward its eye (Eye of Calm: holds it at its wall), lifts it and
 * carries it round (knockback resistance holds against it), hurts it every half second, and does
 * what its level adds (StormeyeRules has the numbers; Stormeyes, the damage it doubles). Drawn by
 * client/StormeyeRenderer; here, on the client, it throws the ground's pieces and wind curls round.
 */
public class StormeyeEntity extends Entity {
    private static final EntityDataAccessor<Integer> AURA = SynchedEntityData.defineId(StormeyeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> REACH = SynchedEntityData.defineId(StormeyeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(StormeyeEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(StormeyeEntity.class, EntityDataSerializers.INT);
    private static final int NO_AURA = -1;
    private static final int WIND_TINT = 0x9ADCD8;

    @Nullable
    private UUID owner;
    private int spellLevel = 1;
    @Nullable
    private String branch5;
    @Nullable
    private String branch10;
    private float power = 1f;
    private int age;
    /** Twin Storms: the middle the two circle, and where round it this one is. */
    @Nullable
    private Vec3 orbitCenter;
    private double orbitAngle;
    private final Set<Integer> held = new HashSet<>();
    private final Set<Integer> deflected = new HashSet<>();

    public StormeyeEntity(EntityType<? extends StormeyeEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Raises {@code caster}'s tornado (two for Twin Storms) at {@code at}, or around them for Eye of Calm. */
    public static void raise(ServerLevel level, LivingEntity caster, Vec3 at, int spellLevel,
                             @Nullable String branch5, @Nullable String branch10, float power) {
        if (StormeyeRules.EYE_OF_CALM.equals(branch10)) {
            at = caster.position();
        }
        if (StormeyeRules.TWIN.equals(branch5)) {
            for (int i = 0; i < 2; i++) {
                StormeyeEntity storm = create(level, caster, at, spellLevel, branch5, branch10, power);
                if (storm != null) {
                    storm.orbitCenter = at;
                    storm.orbitAngle = Math.PI * i;
                    storm.moveTo(storm.orbitPoint(level));
                    level.addFreshEntity(storm);
                }
            }
        } else {
            StormeyeEntity storm = create(level, caster, at, spellLevel, branch5, branch10, power);
            if (storm != null) {
                level.addFreshEntity(storm);
            }
        }
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.2f, 0.6f);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_INHALE, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    @Nullable
    private static StormeyeEntity create(ServerLevel level, LivingEntity caster, Vec3 at, int spellLevel,
                                         @Nullable String branch5, @Nullable String branch10, float power) {
        StormeyeEntity storm = ModContent.STORMEYE.get().create(level);
        if (storm == null) {
            return null;
        }
        storm.owner = caster.getUUID();
        storm.spellLevel = spellLevel;
        storm.branch5 = branch5;
        storm.branch10 = branch10;
        storm.power = power;
        storm.entityData.set(REACH, (float) StormeyeRules.reach(spellLevel, branch5, branch10));
        storm.entityData.set(HEIGHT, (float) StormeyeRules.height(branch10));
        storm.entityData.set(LIFETIME, StormeyeRules.lifetime(spellLevel));
        storm.moveTo(at.x, at.y, at.z, 0, 0);
        return storm;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(AURA, NO_AURA);
        builder.define(REACH, 5f);
        builder.define(HEIGHT, 7f);
        builder.define(LIFETIME, 80);
    }

    public float reach() {
        return entityData.get(REACH);
    }

    public float height() {
        return entityData.get(HEIGHT);
    }

    public int lifetime() {
        return entityData.get(LIFETIME);
    }

    public int age() {
        return age;
    }

    /** The element it took (Absorption), or null. */
    @Nullable
    public ElementalReactions.Aura aura() {
        int aura = entityData.get(AURA);
        return aura == NO_AURA ? null : ElementalReactions.Aura.values()[aura];
    }

    /** Its colour: its element's, or the wind's. */
    public int tint() {
        ElementalReactions.Aura aura = aura();
        return aura == null ? WIND_TINT : aura.color();
    }

    @Override
    public void tick() {
        super.tick();
        age++;
        if (level().isClientSide()) {
            whirlDebris();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        LivingEntity caster = owner == null ? null : level.getEntity(owner) instanceof LivingEntity living ? living : null;
        if (caster == null || !caster.isAlive() || caster.level() != level || age >= lifetime()) {
            end(level);
            return;
        }
        drift(level, caster);
        hold(level, caster);
        if (spellLevel >= 8) {
            scavenge(level, caster);
        }
        if (StormeyeRules.GREAT_TEMPEST.equals(branch10) || StormeyeRules.EYE_OF_CALM.equals(branch10)) {
            deflect(level, caster);
        }
        if (StormeyeRules.EYE_OF_CALM.equals(branch10) && age % (StormeyeRules.PULSE_TICKS * 2) == 0) {
            mend(level, caster);
        }
        if (age % 20 == 1) {
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_WHIRL, SoundSource.PLAYERS, 1.4f, 0.6f);
        }
    }

    /** Where it goes: with its caster (Eye of Calm), round the middle (Twin Storms), or after where they look (Wandering Storm). */
    private void drift(ServerLevel level, LivingEntity caster) {
        if (StormeyeRules.EYE_OF_CALM.equals(branch10)) {
            setPos(caster.position());
        } else if (orbitCenter != null) {
            orbitAngle += 0.08;
            setPos(orbitPoint(level));
        } else if (StormeyeRules.WANDERING.equals(branch5)) {
            Vec3 aim = StormeyeSpell.groundUnderAim(level, caster);
            if (aim != null) {
                Vec3 way = aim.subtract(position()).multiply(1, 0, 1);
                if (way.lengthSqr() > 0.04) {
                    Vec3 next = position().add(way.normalize().scale(Math.min(StormeyeRules.WANDER_SPEED, way.length())));
                    Vec3 ground = StormeyeSpell.groundBelow(level, next.add(0, 2, 0), 6);
                    setPos(ground != null ? ground : next);
                }
            }
        }
    }

    private Vec3 orbitPoint(ServerLevel level) {
        Vec3 point = orbitCenter.add(Math.cos(orbitAngle) * StormeyeRules.TWIN_ORBIT, 0, Math.sin(orbitAngle) * StormeyeRules.TWIN_ORBIT);
        Vec3 ground = StormeyeSpell.groundBelow(level, point.add(0, 2, 0), 6);
        return ground != null ? ground : point;
    }

    /** Drags in, lifts and carries round whatever it may hurt within its reach; hurts it, and what its level adds. */
    private void hold(ServerLevel level, LivingEntity caster) {
        Vec3 eye = position();
        double reach = reach();
        boolean calm = StormeyeRules.EYE_OF_CALM.equals(branch10);
        long now = level.getGameTime();
        held.clear();
        AABB box = new AABB(eye.x - reach - 1, eye.y - 1, eye.z - reach - 1, eye.x + reach + 1, eye.y + height(), eye.z + reach + 1);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, target -> SpellTargets.canAffect(caster, target))) {
            Vec3 flat = target.position().subtract(eye).multiply(1, 0, 1);
            double distance = flat.length();
            if (distance > reach + target.getBbWidth() / 2) {
                continue;
            }
            held.add(target.getId());
            if (spellLevel >= 9) {
                Stormeyes.holdInEye(target, now);
            }
            double grip = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            if (grip > 0) {
                Vec3 inward = distance < 1.0e-3 ? Vec3.ZERO : flat.scale(-1 / distance);
                Vec3 around = new Vec3(-inward.z, 0, inward.x);
                Vec3 push = calm
                        ? inward.scale(distance < reach * 0.8 ? -StormeyeRules.pull(spellLevel) : 0).add(around.scale(StormeyeRules.SPIN))
                        : inward.scale(StormeyeRules.pull(spellLevel) * Math.min(1, distance / 1.5)).add(around.scale(StormeyeRules.SPIN));
                double rise = target.getY() < eye.y + StormeyeRules.LIFT_HEIGHT ? 0.22 : 0.04;
                Vec3 motion = target.getDeltaMovement();
                target.setDeltaMovement(motion.x * (1 - grip) + push.x * grip, Math.max(motion.y, rise * grip), motion.z * (1 - grip) + push.z * grip);
                target.hurtMarked = true;
                target.resetFallDistance();
                if (age % 20 == 1) {
                    target.addEffect(new MobEffectInstance(ModContent.AIRBORNE, 30));
                }
            }
            if (age % StormeyeRules.HIT_TICKS == 0) {
                SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WIND, this, caster), StormeyeRules.damage(spellLevel) * power);
            }
            if (spellLevel >= 3 && aura() == null) {
                absorb(level, target);
            }
            ElementalReactions.Aura aura = aura();
            if (aura != null && age % StormeyeRules.PULSE_TICKS == 0) {
                ElementalReactions.inflict(target, aura);
            }
            if (spellLevel >= 7 && age % StormeyeRules.PULSE_TICKS == StormeyeRules.PULSE_TICKS / 2) {
                ElementalReactions.swirl(target, caster, power);
            }
        }
    }

    /** Absorption: the first creature it takes that carries an element gives it that element. */
    private void absorb(ServerLevel level, LivingEntity target) {
        ElementalReactions.Aura aura = ElementalReactions.auraOf(target);
        if (aura == null) {
            return;
        }
        entityData.set(AURA, aura.ordinal());
        ColorParticleOption color = ColorParticleOption.create(ModContent.SWIRL.get(), 0xFF000000 | aura.color());
        level.sendParticles(color, getX(), getY() + height() * 0.4, getZ(), 40, reach() * 0.4, height() * 0.3, reach() * 0.4, 0.2);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1f, 0.8f);
    }

    /** Scavenger Wind: dropped items and experience nearby fly to its caster. */
    private void scavenge(ServerLevel level, LivingEntity caster) {
        double reach = reach() * 1.5;
        AABB box = new AABB(getX() - reach, getY() - 2, getZ() - reach, getX() + reach, getY() + height(), getZ() + reach);
        for (Entity loot : level.getEntities((Entity) null, box, entity -> entity instanceof ItemEntity || entity instanceof ExperienceOrb)) {
            Vec3 way = caster.position().add(0, 0.6, 0).subtract(loot.position());
            if (way.lengthSqr() < 1.0) {
                continue;
            }
            loot.setDeltaMovement(way.normalize().scale(Math.min(0.9, way.length() * 0.15)).add(0, 0.05, 0));
            loot.hasImpulse = true;
        }
    }

    /**
     * Great Tempest: projectiles that fly into it are thrown back out. Eye of Calm: projectiles
     * flying at its caster are thrown aside. Each only once.
     */
    private void deflect(ServerLevel level, LivingEntity caster) {
        double reach = reach();
        AABB box = new AABB(getX() - reach, getY() - 1, getZ() - reach, getX() + reach, getY() + height(), getZ() + reach);
        boolean calm = StormeyeRules.EYE_OF_CALM.equals(branch10);
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box,
                projectile -> projectile.getOwner() != caster && !deflected.contains(projectile.getId()))) {
            Vec3 motion = projectile.getDeltaMovement();
            double speed = Math.max(0.6, motion.length());
            Vec3 out;
            if (calm) {
                Vec3 toCaster = caster.getBoundingBox().getCenter().subtract(projectile.position());
                if (motion.dot(toCaster) <= 0) {
                    continue;
                }
                out = new Vec3(-motion.z, 0, motion.x).normalize().scale(speed * 0.8).add(0, 0.2, 0);
            } else {
                Vec3 flat = projectile.position().subtract(position()).multiply(1, 0, 1);
                out = (flat.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : flat.normalize()).scale(speed).add(0, 0.3, 0);
            }
            deflected.add(projectile.getId());
            projectile.setDeltaMovement(out);
            projectile.hasImpulse = true;
            projectile.hurtMarked = true;
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, projectile.getX(), projectile.getY(), projectile.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.BREEZE_DEFLECT, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    /** Eye of Calm: its caster and the players inside it mend a heart. */
    private void mend(ServerLevel level, LivingEntity caster) {
        double reach = reach();
        for (Player player : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(reach, height(), reach),
                player -> player.isAlive() && player.position().subtract(position()).multiply(1, 0, 1).length() <= reach)) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(2f);
                level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.1), player.getZ(), 1, 0.2, 0.1, 0.2, 0);
            }
        }
    }

    /** It blows itself out: what it held drops (Crushing Winds: is flung out, hard). */
    private void end(ServerLevel level) {
        if (spellLevel >= 6) {
            for (int id : held) {
                if (level.getEntity(id) instanceof LivingEntity target && target.isAlive()) {
                    double grip = 1 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
                    Vec3 flat = target.position().subtract(position()).multiply(1, 0, 1);
                    Vec3 out = flat.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : flat.normalize();
                    target.setDeltaMovement(out.scale(1.2 * grip).add(0, 0.6 * grip, 0));
                    target.hurtMarked = true;
                }
            }
        }
        level.sendParticles(ParticleTypes.GUST_EMITTER_LARGE, getX(), getY() + 0.5, getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 30, reach() * 0.4, 0.3, reach() * 0.4, 0.15);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
        discard();
    }

    /** The ground's own pieces and curls of wind, spinning up through it. */
    private void whirlDebris() {
        float reach = reach();
        float strength = Mth.clamp(age / 6f, 0f, 1f);
        for (int i = 0; i < 3; i++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double r = (0.3 + random.nextDouble() * 0.7) * reach * 0.6 * strength;
            double x = getX() + Math.cos(angle) * r;
            double z = getZ() + Math.sin(angle) * r;
            Vec3 around = new Vec3(-Math.sin(angle), 0, Math.cos(angle)).scale(0.35);
            BlockState ground = level().getBlockState(BlockPos.containing(x, getY() - 0.5, z));
            if (i == 0 && !ground.isAir() && ground.getRenderShape() != RenderShape.INVISIBLE) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, ground), x, getY() + 0.2, z, around.x, 0.45, around.z);
            }
            double y = getY() + random.nextDouble() * height() * 0.8;
            level().addParticle(ColorParticleOption.create(ModContent.SWIRL.get(), 0xFF000000 | tint()), x, y, z, around.x, 0.08, around.z);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double reach = reach();
        return new AABB(getX() - reach, getY(), getZ() - reach, getX() + reach, getY() + height(), getZ() + reach);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
