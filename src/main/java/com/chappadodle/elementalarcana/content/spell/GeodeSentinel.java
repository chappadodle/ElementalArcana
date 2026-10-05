package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.CrystalShards;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

/**
 * Crystal's Geode Sentinel (docs/superpowers/specs/2026-10-05-geode-sentinel-design.md): a cluster of
 * violet crystal grown where its caster looked. For 12 seconds every hostile creature within 10
 * blocks that its caster may hurt turns on it (once a second; bosses don't). Its caster and their
 * allies can't hurt it, nothing moves it, and whatever does hit it takes a third of the blow back
 * (thorns, from the sentinel, so the attacker stays turned on it). When it breaks or its time is up
 * it bursts, hurting and throwing back the foes round it, and leaves a Crystallize shield shard. One
 * per caster. Drawn by client/GeodeSentinelRenderer; never saved.
 */
public class GeodeSentinel extends LivingEntity {
    public static final int LIFETIME_TICKS = 240;
    private static final Map<UUID, UUID> BY_OWNER = new HashMap<>();
    private static final double BASE_HEALTH = 40;
    private static final double HEALTH_PER_POWER = 20;
    private static final double TAUNT_RADIUS = 10;
    private static final double BURST_RADIUS = 4;
    private static final float BURST_DAMAGE = 6f;
    private static final float THORNS = 1f / 3f;
    private static final int SHARD_TICKS = 200;
    private static final int COLOR = Element.CRYSTAL.color();

    @Nullable
    private UUID owner;
    private float power = 1f;
    private boolean shattered;

    public GeodeSentinel(EntityType<? extends GeodeSentinel> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    /** Grows {@code owner}'s sentinel on the ground under {@code at}, shattering the one they had. */
    public static void grow(ServerLevel level, LivingEntity owner, Vec3 at, float power) {
        UUID previous = BY_OWNER.get(owner.getUUID());
        if (previous != null && level.getEntity(previous) instanceof GeodeSentinel old) {
            old.shatter();
        }
        GeodeSentinel sentinel = ModContent.GEODE_SENTINEL.get().create(level);
        if (sentinel == null) {
            return;
        }
        BlockHitResult ground = level.clip(new ClipContext(at.add(0, 0.5, 0), at.subtract(0, 4, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 foot = ground.getType() == HitResult.Type.BLOCK ? ground.getLocation() : at;
        sentinel.owner = owner.getUUID();
        sentinel.power = power;
        AttributeInstance health = sentinel.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(BASE_HEALTH + HEALTH_PER_POWER * power);
        }
        sentinel.setHealth(sentinel.getMaxHealth());
        sentinel.moveTo(foot.x, foot.y, foot.z, owner.getRandom().nextFloat() * 360f, 0);
        level.addFreshEntity(sentinel);
        BY_OWNER.put(owner.getUUID(), sentinel.getUUID());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                foot.x, foot.y + 0.2, foot.z, 30, 0.6, 0.1, 0.6, 0.15);
        play(level, foot, SoundEvents.AMETHYST_BLOCK_PLACE, 1.2f, 0.6f);
        play(level, foot, SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 0.8f);
    }

    @Nullable
    private LivingEntity owner(ServerLevel level) {
        return owner != null && level.getEntity(owner) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel level) || shattered) {
            return;
        }
        LivingEntity caster = owner(level);
        if (caster == null || tickCount >= LIFETIME_TICKS) {
            shatter();
            return;
        }
        if (tickCount % 20 == 1) {
            taunt(level, caster);
        }
        if (tickCount % 5 == 0) {
            level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), COLOR, 0x7A3FC0, 0.18f, 16),
                    getX(), getY(0.6), getZ(), 2, 0.4, 0.6, 0.4, 0.01);
        }
    }

    /** Every hostile creature in reach (not a boss) turns on the sentinel; a ring pulses out over the ground. */
    private void taunt(ServerLevel level, LivingEntity caster) {
        List<Mob> foes = level.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(TAUNT_RADIUS),
                mob -> mob instanceof Enemy && mob.isAlive() && mob.distanceTo(this) <= TAUNT_RADIUS
                        && !mob.getType().is(Tags.EntityTypes.BOSSES) && SpellTargets.canAffect(caster, mob));
        for (Mob mob : foes) {
            if (mob.getTarget() != this) {
                mob.setTarget(this);
                level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), COLOR, 0x7A3FC0, 0.25f, 12),
                        mob.getX(), mob.getY(1.1), mob.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
            }
        }
        for (int i = 0; i < 28; i++) {
            double angle = Math.PI * 2 * i / 28;
            // Count 0: the offsets are the particle's velocity, outward.
            level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), COLOR, 0x7A3FC0, 0.3f, 14),
                    getX() + Math.cos(angle) * 1.6, getY() + 0.15, getZ() + Math.sin(angle) * 1.6, 0,
                    Math.cos(angle) * 0.25, 0, Math.sin(angle) * 0.25, 1);
        }
        play(level, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7f, 1.2f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel level) || shattered || isInvulnerableTo(source)
                || source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        LivingEntity caster = owner(level);
        Entity attacker = source.getEntity();
        if (caster != null && attacker instanceof LivingEntity living && (living == caster || !SpellTargets.canAffect(caster, living))) {
            // Its caster and their allies can't hurt it.
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && caster != null && attacker instanceof LivingEntity living && living != this && living.isAlive()) {
            // Thorns from the sentinel itself, so what it hurts stays turned on it (not on its caster).
            living.hurt(damageSources().thorns(this), Math.max(1f, amount * THORNS));
            Vec3 from = getBoundingBox().getCenter();
            Vec3 to = living.getBoundingBox().getCenter();
            for (int i = 1; i <= 5; i++) {
                Vec3 at = from.lerp(to, i / 6.0);
                level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), COLOR, 0xFFFFFF, 0.2f, 8), at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
            play(level, position(), SoundEvents.AMETHYST_CLUSTER_HIT, 1f, 1.1f);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        shatter();
    }

    /** Bursts: the foes close by are hurt and thrown back, a shield shard is left, and it's gone. */
    void shatter() {
        if (shattered || !(level() instanceof ServerLevel level)) {
            return;
        }
        shattered = true;
        LivingEntity caster = owner(level);
        if (caster != null) {
            for (LivingEntity foe : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(BURST_RADIUS),
                    foe -> foe != this && foe.isAlive() && foe.distanceTo(this) <= BURST_RADIUS && SpellTargets.canAffect(caster, foe))) {
                SpellDamage.hurtMultiHit(foe, SpellDamage.source(level, Element.CRYSTAL, this, caster), BURST_DAMAGE * power);
                Vec3 away = foe.position().subtract(position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
                foe.setDeltaMovement(foe.getDeltaMovement().add(away.scale(0.6)).add(0, 0.3, 0));
                foe.hurtMarked = true;
            }
            CrystalShards.spawn(level, position(), COLOR, SHARD_TICKS);
        }
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                getX(), getY(0.5), getZ(), 60, 0.5, 0.8, 0.5, 0.3);
        level.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), COLOR, 0xFFFFFF, 0.4f, 16), getX(), getY(0.5), getZ(),
                24, 0.6, 0.8, 0.6, 0.12);
        play(level, position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 1.3f, 0.7f);
        play(level, position(), SoundEvents.GLASS_BREAK, 0.8f, 1.3f);
        if (owner != null) {
            BY_OWNER.remove(owner, getUUID());
        }
        discard();
    }

    private static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    // It stands where it grew: nothing pushes it, it pushes nothing, and it shrugs off potions.

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
