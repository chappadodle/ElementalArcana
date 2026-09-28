package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ice's basic spell, levels 1-10. Hold the cast key: frost gathers into icicles beside you that
 * grow sharper as they charge; release and they fly at your crosshair.
 *
 * <pre>
 * Lv1 Icicle        one icicle            Lv6  Sharpened     +25% damage, longer freeze
 * Lv2 Twin Icicles  two icicles           Lv7  Frostseeker   icicles curve toward the target
 * Lv3 Quick Frost   faster charge, taps   Lv8  Halo          five icicles, fired in a ripple
 *                   hit harder            Lv9  Deep Freeze   3+ hits from one volley = Frozen
 * Lv4 Triad         three icicles         Lv10 capstone:     Glacial Lance | Endless Winter
 * Lv5 branch:       Piercing Cold | Shatterburst
 * </pre>
 */
public class IcicleSpell extends Spell implements ProjectileSpell {
    public static final String PIERCING = "piercing";
    public static final String SHATTERBURST = "shatterburst";
    public static final String LANCE = "lance";
    public static final String WINTER = "winter";

    private static final int MAX_LEVEL = 10;
    private static final int FREEZE_RADIUS = 2;
    private static final int LANCE_EXTRA_HOLD = 20;
    private static final ResourceLocation MODEL = ElementalArcana.id("spell/icicle");

    // Per-icicle data stored on the projectile, so hits resolve with the caster's progress at cast time.
    private static final String TAG_LEVEL = "ea_icicle_level";
    private static final String TAG_BRANCH_5 = "ea_icicle_branch5";
    private static final String TAG_BRANCH_10 = "ea_icicle_branch10";
    private static final String TAG_VOLLEY = "ea_icicle_volley";
    private static final String TAG_HOMING = "ea_icicle_homing";
    private static final String TAG_LANCE = "ea_icicle_lance";
    // Stored on targets to count hits from one volley (Deep Freeze).
    private static final String TAG_TARGET_VOLLEY = "ea_icicle_hit_volley";
    private static final String TAG_TARGET_HITS = "ea_icicle_hit_count";

    public IcicleSpell() {
        super(ModSchools.ICE, 12, 12);
    }

    // ---- levels ----

    @Override
    public int maxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(PIERCING, SHATTERBURST);
            case 10 -> List.of(LANCE, WINTER);
            default -> List.of();
        };
    }

    private static int icicleCount(int level) {
        return level >= 8 ? 5 : level >= 4 ? 3 : level >= 2 ? 2 : 1;
    }

    private static int chargeTicks(int level) {
        return level >= 3 ? 15 : 20;
    }

    @Override
    public int manaCost(int spellLevel) {
        return 12 + 4 * (icicleCount(spellLevel) - 1);
    }

    // ---- casting ----

    @Override
    public CastResult cast(CastContext context) {
        int level = context.spellLevel();
        int count = icicleCount(level);
        int volley = context.caster().getRandom().nextInt();
        List<SpellProjectile> icicles = new ArrayList<>();
        for (int slot = 0; slot < count; slot++) {
            SpellProjectile icicle = SpellProjectile.summonHeld(context, this, slot, count, chargeTicks(level));
            CompoundTag tag = icicle.getPersistentData();
            tag.putInt(TAG_LEVEL, level);
            tag.putString(TAG_BRANCH_5, orEmpty(context.branch(5)));
            tag.putString(TAG_BRANCH_10, orEmpty(context.branch(10)));
            tag.putInt(TAG_VOLLEY, volley);
            icicles.add(icicle);
        }
        context.holdUntilRelease(new Hold(context.caster(), icicles, chargeTicks(level), context.hasBranch(10, LANCE)));
        playAt(icicles.get(0), SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 1.3f);
        return CastResult.SUCCESS;
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Override
    public ParticleOptions trailParticle() {
        return ModContent.FROST_SPARKLE.get();
    }

    @Override
    public ResourceLocation model() {
        return MODEL;
    }

    @Override
    public int chargeTicks() {
        return chargeTicks(1);
    }

    @Override
    public float releaseSpeed(float charge) {
        return 1.4f + 1.4f * charge;
    }

    @Override
    public int lifetimeTicks() {
        return 60;
    }

    /** One icicle over the right shoulder; more spread in an arc over your head, right to left. */
    @Override
    public Vec3 holdOffset(int slot, int count) {
        if (count <= 1) {
            return new Vec3(0.55, 0.15, 0.9);
        }
        double angle = Math.PI * slot / (count - 1);
        return new Vec3(0.8 * Math.cos(angle), 0.1 + 0.6 * Math.sin(angle), 0.75);
    }

    // ---- visuals (client) ----

    /** Frost gathers inward while charging (fewer sparkles each when several icicles form); once full, a steady glint. */
    @Override
    public void heldParticles(SpellProjectile icicle, float charge) {
        boolean many = icicle.formationCount() > 2;
        if (charge < 1f) {
            if (many && icicle.tickCount % 2 != 0) {
                return;
            }
            int count = many ? 1 : 1 + Math.round(charge * 3);
            for (int i = 0; i < count; i++) {
                Vec3 from = randomOnSphere(icicle, 0.8 + icicle.getRandom().nextDouble() * 0.5);
                // Sparkles slow by friction 0.86/tick, so total travel is about 7x this speed: aim to land on the icicle.
                Vec3 inward = icicle.position().subtract(from).scale(0.15);
                icicle.level().addParticle(ModContent.FROST_SPARKLE.get(), from.x, from.y, from.z, inward.x, inward.y, inward.z);
            }
            if (icicle.tickCount % 4 == 0) {
                icicle.spawnParticleAround(ModContent.FROST_MIST.get(), 0.15, new Vec3(0, 0.005, 0));
            }
        } else if (icicle.tickCount % (many ? 4 : 2) == 0) {
            icicle.spawnParticleAround(ModContent.FROST_SPARKLE.get(), 0.25 * icicle.visualScale(), new Vec3(0, 0.01, 0));
        }
    }

    /** A shimmering trail of glints, a wisp of cold mist, and the odd snowflake. */
    @Override
    public void flightParticles(SpellProjectile icicle) {
        Vec3 back = icicle.getDeltaMovement().scale(-0.05);
        int sparkles = icicle.visualScale() > 1.5f ? 5 : 2;
        for (int i = 0; i < sparkles; i++) {
            icicle.spawnParticleAround(ModContent.FROST_SPARKLE.get(), 0.08 * icicle.visualScale(), back);
        }
        if (icicle.tickCount % 2 == 0) {
            icicle.spawnParticleAround(ModContent.FROST_MIST.get(), 0.05, Vec3.ZERO);
        }
        if (icicle.tickCount % 3 == 0) {
            icicle.spawnParticleAround(ParticleTypes.SNOWFLAKE, 0.1, Vec3.ZERO);
        }
    }

    private static Vec3 randomOnSphere(SpellProjectile icicle, double radius) {
        double theta = icicle.getRandom().nextDouble() * Mth.TWO_PI;
        double y = icicle.getRandom().nextDouble() * 2 - 1;
        double ring = Math.sqrt(1 - y * y);
        return icicle.position().add(Math.cos(theta) * ring * radius, y * radius, Math.sin(theta) * ring * radius);
    }

    // ---- flight (server) ----

    /** Branch effects that apply from the moment each icicle is thrown. */
    @Override
    public void onRelease(SpellProjectile icicle) {
        CompoundTag tag = icicle.getPersistentData();
        int level = tag.getInt(TAG_LEVEL);
        if (tag.getBoolean(TAG_LANCE)) {
            icicle.setPierce(Integer.MAX_VALUE);
            icicle.setDeltaMovement(icicle.getDeltaMovement().scale(1.2));
        } else if (PIERCING.equals(tag.getString(TAG_BRANCH_5))) {
            icicle.setPierce(2);
            icicle.setDeltaMovement(icicle.getDeltaMovement().scale(1.3));
        }
        if (level >= 7 && icicle.getOwner() instanceof ServerPlayer owner) {
            LivingEntity target = homingTarget(owner);
            if (target != null) {
                tag.putInt(TAG_HOMING, target.getId());
            }
        }
        playAt(icicle, SoundEvents.TRIDENT_THROW.value(), icicle.formationCount() > 1 ? 0.5f : 0.8f, 1.5f);
    }

    /** The living thing closest to the caster's crosshair, within a narrow cone and 40 blocks. */
    @Nullable
    private static LivingEntity homingTarget(ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        LivingEntity best = null;
        double bestAngle = Math.cos(Math.toRadians(20));
        AABB area = caster.getBoundingBox().expandTowards(look.scale(40)).inflate(4);
        for (LivingEntity candidate : caster.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != caster && e.isAlive() && !e.isSpectator())) {
            Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
            double dot = to.normalize().dot(look);
            if (dot > bestAngle && to.length() <= 40 && caster.hasLineOfSight(candidate)) {
                bestAngle = dot;
                best = candidate;
            }
        }
        return best;
    }

    @Override
    public void onTick(SpellProjectile icicle) {
        // Projectiles fly straight through water surfaces, so freezing is checked in flight.
        if (icicle.isInWater()) {
            Freezing.freezeWater((ServerLevel) icicle.level(), icicle.blockPosition(), FREEZE_RADIUS);
            shatter(icicle);
            icicle.discard();
            return;
        }
        CompoundTag tag = icicle.getPersistentData();
        if (tag.contains(TAG_HOMING) && icicle.level().getEntity(tag.getInt(TAG_HOMING)) instanceof LivingEntity target && target.isAlive()) {
            // Frostseeker: turn a little toward the target each tick, keeping speed.
            Vec3 velocity = icicle.getDeltaMovement();
            Vec3 wanted = target.getBoundingBox().getCenter().subtract(icicle.position()).normalize().scale(velocity.length());
            icicle.setDeltaMovement(velocity.lerp(wanted, 0.18).normalize().scale(velocity.length()));
            icicle.hasImpulse = true;
        }
    }

    // ---- hits (server) ----

    private static float damage(SpellProjectile icicle, CompoundTag tag) {
        int level = tag.getInt(TAG_LEVEL);
        float charge = icicle.charge(0f);
        if (level >= 3) {
            // Quick Frost: even a tap-throw carries some bite.
            charge = Math.max(charge, 0.3f);
        }
        float damage = 3f + 5f * charge;
        if (level >= 6) {
            damage *= 1.25f;
        }
        if (tag.getBoolean(TAG_LANCE)) {
            damage *= icicleCount(level) * 0.8f;
        }
        return damage * icicle.power();
    }

    @Override
    public void onHitEntity(SpellProjectile icicle, EntityHitResult hit) {
        Entity target = hit.getEntity();
        CompoundTag tag = icicle.getPersistentData();
        int level = tag.getInt(TAG_LEVEL);
        float charge = icicle.charge(0f);
        SpellDamage.hurtMultiHit(target, SpellDamage.source(icicle.level(), Element.ICE, icicle, icicle.getOwner()), damage(icicle, tag));
        if (target instanceof LivingEntity living) {
            ElementalReactions.iceHit(living);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(40 + 40 * charge), 1));
            if (level >= 9) {
                countVolleyHit(living, tag.getInt(TAG_VOLLEY));
            }
        }
        if (target.canFreeze() && charge >= 1f) {
            freezeOver(target, level >= 6 ? 120 : 80);
        }
        if (SHATTERBURST.equals(tag.getString(TAG_BRANCH_5)) && !tag.getBoolean(TAG_LANCE)) {
            shatterburst(icicle, target);
        }
        if (WINTER.equals(tag.getString(TAG_BRANCH_10))) {
            if (!target.isAlive() && icicle.getOwner() instanceof ServerPlayer owner) {
                // Endless Winter: a kill takes half off the remaining cooldown.
                MagicAttachments.get(owner).reduceCooldown(id(), owner.level().getGameTime(), 0.5f);
                MagicAttachments.sync(owner);
            }
            if (charge >= 1f) {
                frostPatch(icicle, target.position());
            }
        }
        shatter(icicle);
    }

    @Override
    public void onHitBlock(SpellProjectile icicle, BlockHitResult hit) {
        CompoundTag tag = icicle.getPersistentData();
        Freezing.freezeWater((ServerLevel) icicle.level(), hit.getBlockPos(), FREEZE_RADIUS);
        if (SHATTERBURST.equals(tag.getString(TAG_BRANCH_5)) && !tag.getBoolean(TAG_LANCE)) {
            shatterburst(icicle, null);
        }
        if (WINTER.equals(tag.getString(TAG_BRANCH_10)) && icicle.charge(0f) >= 1f) {
            frostPatch(icicle, Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection())));
        }
        shatter(icicle);
    }

    /** Deep Freeze: the third hit on one target from the same volley freezes it solid for 2 seconds. */
    private static void countVolleyHit(LivingEntity target, int volley) {
        CompoundTag data = target.getPersistentData();
        int hits = data.getInt(TAG_TARGET_VOLLEY) == volley ? data.getInt(TAG_TARGET_HITS) + 1 : 1;
        data.putInt(TAG_TARGET_VOLLEY, volley);
        data.putInt(TAG_TARGET_HITS, hits);
        if (hits == 3) {
            target.addEffect(new MobEffectInstance(ModContent.FROZEN, 40));
            ServerLevel level = (ServerLevel) target.level();
            level.sendParticles(ModContent.ICE_SHARD.get(), target.getX(), target.getY(0.5), target.getZ(), 16, 0.3, 0.4, 0.3, 0.08);
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1f, 0.6f);
        }
    }

    /** Shatterburst: shards burst out and hit everything close by for half damage. */
    private static void shatterburst(SpellProjectile icicle, @Nullable Entity alreadyHit) {
        ServerLevel level = (ServerLevel) icicle.level();
        float damage = damage(icicle, icicle.getPersistentData()) * 0.5f;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, icicle.getBoundingBox().inflate(2.0),
                e -> e != alreadyHit && e != icicle.getOwner() && e.isAlive() && e.distanceTo(icicle) <= 2.0)) {
            SpellDamage.hurtMultiHit(nearby, SpellDamage.source(level, Element.ICE, icicle, icicle.getOwner()), damage);
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
        }
        level.sendParticles(ModContent.ICE_SHARD.get(), icicle.getX(), icicle.getY(), icicle.getZ(), 24, 0.15, 0.15, 0.15, 0.3);
        level.sendParticles(ModContent.FROST_MIST.get(), icicle.getX(), icicle.getY(), icicle.getZ(), 6, 0.6, 0.3, 0.6, 0.02);
    }

    /** Endless Winter: a lingering patch of frost that slows whatever walks through it. */
    private static void frostPatch(SpellProjectile icicle, Vec3 at) {
        AreaEffectCloud patch = new AreaEffectCloud(icicle.level(), at.x, at.y, at.z);
        if (icicle.getOwner() instanceof LivingEntity owner) {
            patch.setOwner(owner);
        }
        patch.setRadius(2.5f);
        patch.setRadiusPerTick(-0.01f);
        patch.setDuration(100);
        patch.setWaitTime(0);
        patch.setParticle(ParticleTypes.SNOWFLAKE);
        patch.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        icicle.level().addFreshEntity(patch);
    }

    /** Full-charge hit: the target frosts over (vanilla freeze overlay + shivering) with a crust of ice. */
    private static void freezeOver(Entity target, int extraTicks) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + extraTicks));
        ServerLevel level = (ServerLevel) target.level();
        double width = target.getBbWidth() * 0.6;
        double height = target.getBbHeight() * 0.5;
        level.sendParticles(ModContent.FROST_SPARKLE.get(), target.getX(), target.getY(0.5), target.getZ(), 18, width, height, width, 0.02);
        level.sendParticles(ModContent.FROST_MIST.get(), target.getX(), target.getY(0.3), target.getZ(), 5, width * 0.6, height * 0.5, width * 0.6, 0.01);
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 12, width, height, width, 0.01);
    }

    /** Shatter: shards spray and rain down, a puff of cold mist, glints; bigger and deeper at full charge. */
    private static void shatter(SpellProjectile icicle) {
        ServerLevel level = (ServerLevel) icicle.level();
        float charge = icicle.charge(0f);
        float size = icicle.visualScale();
        double x = icicle.getX();
        double y = icicle.getY();
        double z = icicle.getZ();
        level.sendParticles(ModContent.ICE_SHARD.get(), x, y, z, Math.round((8 + 12 * charge) * size), 0.1 * size, 0.1 * size, 0.1 * size, 0.12 + 0.1 * charge);
        level.sendParticles(ModContent.FROST_MIST.get(), x, y, z, Math.round((2 + 3 * charge) * size), 0.15 * size, 0.15 * size, 0.15 * size, 0.02);
        level.sendParticles(ModContent.FROST_SPARKLE.get(), x, y, z, Math.round((6 + 10 * charge) * size), 0.2 * size, 0.2 * size, 0.2 * size, 0.15);
        level.playSound(null, x, y, z, ModContent.ICICLE_IMPACT.get(), SoundSource.PLAYERS,
                Math.min(2f, (0.7f + 0.4f * charge) * size), (1.15f - 0.25f * charge) / (float) Math.sqrt(size) + (level.getRandom().nextFloat() - 0.5f) * 0.1f);
    }

    private static void playAt(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    // ---- the hold ----

    private static final class Hold implements SpellHold {
        private final ServerPlayer caster;
        private final List<SpellProjectile> icicles;
        private final int chargeTicks;
        private final boolean canForgeLance;
        private boolean lanceForged;

        Hold(ServerPlayer caster, List<SpellProjectile> icicles, int chargeTicks, boolean canForgeLance) {
            this.caster = caster;
            this.icicles = icicles;
            this.chargeTicks = chargeTicks;
            this.canForgeLance = canForgeLance && icicles.size() > 1;
        }

        @Override
        public boolean tick(int heldTicks) {
            icicles.removeIf(icicle -> !icicle.isAlive());
            if (icicles.isEmpty()) {
                return false;
            }
            if (heldTicks == chargeTicks) {
                playAt(icicles.get(0), SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
                for (SpellProjectile icicle : icicles) {
                    ((ServerLevel) icicle.level()).sendParticles(ModContent.FROST_SPARKLE.get(),
                            icicle.getX(), icicle.getY(), icicle.getZ(), 14, 0.05, 0.05, 0.05, 0.12);
                }
            }
            if (canForgeLance && !lanceForged && heldTicks == chargeTicks + LANCE_EXTRA_HOLD) {
                forgeLance();
            }
            return true;
        }

        /** Glacial Lance: the whole formation fuses into one huge icicle in front of you. */
        private void forgeLance() {
            lanceForged = true;
            ServerLevel level = caster.serverLevel();
            SpellProjectile lance = icicles.get(0);
            for (SpellProjectile other : icicles.subList(1, icicles.size())) {
                level.sendParticles(ModContent.FROST_SPARKLE.get(), other.getX(), other.getY(), other.getZ(), 10, 0.1, 0.1, 0.1, 0.05);
                level.sendParticles(ModContent.FROST_MIST.get(), other.getX(), other.getY(), other.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
                other.discard();
            }
            icicles.subList(1, icicles.size()).clear();
            lance.setFormation(0, 1);
            lance.setVisualScale(2.2f);
            lance.getPersistentData().putBoolean(TAG_LANCE, true);
            playAt(lance, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.6f);
            playAt(lance, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.8f);
            level.sendParticles(ModContent.FROST_SPARKLE.get(), lance.getX(), lance.getY(), lance.getZ(), 30, 0.3, 0.3, 0.3, 0.15);
        }

        @Override
        public void release(int heldTicks) {
            Vec3 aim = SpellProjectile.crosshairTarget(caster);
            boolean ripple = icicles.size() >= 5;
            for (int i = 0; i < icicles.size(); i++) {
                // Halo: fire in a quick ripple rather than all at once.
                icicles.get(i).release(aim, ripple ? i * 2 : 0);
            }
        }

        @Override
        public void cancel() {
            for (SpellProjectile icicle : icicles) {
                if (icicle.isAlive()) {
                    shatter(icicle);
                    icicle.discard();
                }
            }
        }
    }
}
