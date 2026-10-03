package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.PrismBoltRules;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSpells;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Prism Bolt's Crystal Spire (Lv 10): a cluster of violet crystal grown out of the ground where a
 * bolt first struck. For six seconds it throws a shard at the nearest creature its caster may hurt
 * within 10 blocks, every half second, then shatters. One per caster: a new cast's spire shatters
 * the old one (a Twin Prisms cast grows only one). Drawn by client/CrystalSpireRenderer; never saved.
 */
public class CrystalSpire extends Entity {
    private static final Map<UUID, UUID> BY_CASTER = new HashMap<>();
    /** How high above its foot it throws from. */
    private static final double TIP = 1.5;

    @Nullable
    private UUID owner;
    private float power = 1f;
    private int spellLevel = 10;
    @Nullable
    private String fork;
    private int cast;

    public CrystalSpire(EntityType<? extends CrystalSpire> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
    }

    /** Grows {@code owner}'s spire at {@code at} (on the ground below it, if it's close). */
    public static void grow(ServerLevel level, LivingEntity owner, Vec3 at, float power, int spellLevel, @Nullable String fork, int cast) {
        UUID previous = BY_CASTER.get(owner.getUUID());
        if (previous != null && level.getEntity(previous) instanceof CrystalSpire old) {
            if (old.cast == cast) {
                return;
            }
            old.shatter(level);
        }
        CrystalSpire spire = ModContent.CRYSTAL_SPIRE.get().create(level);
        if (spire == null) {
            return;
        }
        BlockHitResult ground = level.clip(new ClipContext(at.add(0, 0.5, 0), at.subtract(0, 3, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 foot = ground.getType() == HitResult.Type.BLOCK ? ground.getLocation() : at;
        spire.owner = owner.getUUID();
        spire.power = power;
        spire.spellLevel = spellLevel;
        spire.fork = fork;
        spire.cast = cast;
        spire.moveTo(foot.x, foot.y, foot.z, owner.getRandom().nextFloat() * 360f, 0);
        level.addFreshEntity(spire);
        BY_CASTER.put(owner.getUUID(), spire.getUUID());
        level.playSound(null, foot.x, foot.y, foot.z, SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 1.2f, 0.8f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                foot.x, foot.y + 0.2, foot.z, 20, 0.4, 0.2, 0.4, 0.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (random.nextInt(6) == 0) {
                level().addParticle(ParticleTypes.END_ROD, getX() + (random.nextDouble() - 0.5) * 0.8,
                        getY() + random.nextDouble() * TIP, getZ() + (random.nextDouble() - 0.5) * 0.8, 0, 0.01, 0);
            }
            return;
        }
        ServerLevel level = (ServerLevel) level();
        LivingEntity caster = owner != null && level.getEntity(owner) instanceof LivingEntity living ? living : null;
        if (caster == null || !caster.isAlive() || tickCount > PrismBoltRules.SPIRE_TICKS) {
            shatter(level);
            return;
        }
        if (tickCount >= 10 && tickCount % PrismBoltRules.SPIRE_INTERVAL == 0) {
            Vec3 tip = position().add(0, TIP, 0);
            LivingEntity target = nearest(level, caster, tip);
            if (target != null) {
                ModSpells.PRISM_BOLT.get().throwFromSpire(caster, tip, target, power, spellLevel, fork, cast);
                level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f + random.nextFloat() * 0.3f);
            }
        }
    }

    /** The nearest creature its caster may hurt within reach, in sight of its tip. */
    @Nullable
    private static LivingEntity nearest(ServerLevel level, LivingEntity caster, Vec3 tip) {
        double range = PrismBoltRules.SPIRE_RANGE;
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(tip, tip).inflate(range),
                        e -> SpellTargets.canAffect(caster, e) && e.getBoundingBox().getCenter().distanceToSqr(tip) <= range * range)
                .stream()
                .filter(e -> level.clip(new ClipContext(tip, e.getBoundingBox().getCenter(), ClipContext.Block.COLLIDER,
                        ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS)
                .min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(tip)))
                .orElse(null);
    }

    private void shatter(ServerLevel level) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                getX(), getY() + 0.8, getZ(), 30, 0.4, 0.6, 0.4, 0.15);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 0.9f);
        if (owner != null && getUUID().equals(BY_CASTER.get(owner))) {
            BY_CASTER.remove(owner);
        }
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
