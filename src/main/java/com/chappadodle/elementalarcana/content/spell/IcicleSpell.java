package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ConjureSpell;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Glow;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.IceShatterOptions;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

import java.util.List;
import java.util.Map;

/**
 * Ice's basic spell, levels 1-10. Each press of the cast key conjures an icicle beside you (up to
 * the level's maximum); they grow sharper while held, and "launch one" / "launch all" throws them
 * at your crosshair (see Conjuring). Hold the cast key with a full set for Glacial Lance.
 *
 * <pre>
 * Lv1 Icicle        one icicle            Lv6  Sharpened     +25% damage, longer freeze
 * Lv2 Twin Icicles  two icicles           Lv7  Frostseeker   icicles curve toward the target
 * Lv3 Quick Frost   faster charge, taps   Lv8  Halo          up to five icicles
 *                   hit harder            Lv9  Deep Freeze   3+ hits from one volley = Frozen
 * Lv4 Triad         three icicles         Lv10 capstone:     Glacial Lance | Endless Winter
 * Lv5 branch:       Piercing Cold | Shatterburst
 * </pre>
 */
public class IcicleSpell extends Spell implements ProjectileSpell, ConjureSpell {
    public static final String PIERCING = "piercing";
    public static final String SHATTERBURST = "shatterburst";
    public static final String LANCE = "lance";
    public static final String WINTER = "winter";

    private static final int MAX_LEVEL = 10;
    private static final int FREEZE_RADIUS = 2;
    private static final int EXTRA_ICICLE_COST = 4;
    private static final ResourceLocation MODEL = ElementalArcana.id("spell/icicle");

    // Looks (SpellProjectile#variant): the ice runs colder as it levels, and each branch has its own
    // look (see docs/superpowers/specs/2026-09-30-icicle-vfx-design.md).
    public static final int LOOK_FROST_1 = 0;
    public static final int LOOK_FROST_2 = 1;
    public static final int LOOK_FROST_3 = 2;
    public static final int LOOK_FROST_4 = 3;
    public static final int LOOK_PIERCING = 4;
    public static final int LOOK_SHATTER = 5;
    public static final int LOOK_WINTER = 6;
    public static final int LOOK_LANCE = 7;
    /** Added to the look at Lv 8+: the ice glows brighter (see glow and luminance). */
    public static final int BRIGHT = 8;
    private static final List<ResourceLocation> LOOK_MODELS = List.of(MODEL,
            ElementalArcana.id("spell/icicle_frost2"), ElementalArcana.id("spell/icicle_frost3"),
            ElementalArcana.id("spell/icicle_frost4"), ElementalArcana.id("spell/icicle_needle"),
            ElementalArcana.id("spell/icicle_crystal"), ElementalArcana.id("spell/icicle_winter"),
            ElementalArcana.id("spell/icicle_lance"));
    private static final List<Glow> LOOK_GLOWS = List.of(
            new Glow(0xBFE6FF, 0.8f, 0.25f),
            new Glow(0x8FD3FF, 0.9f, 0.35f),
            new Glow(0x5AA8FF, 1.0f, 0.45f),
            new Glow(0xA8F4FF, 1.2f, 0.7f),
            new Glow(0xC8FAFF, 0.8f, 0.45f),
            new Glow(0x6FE0FF, 1.0f, 0.6f),
            new Glow(0x9FB8D8, 1.1f, 0.35f),
            new Glow(0x9FE8FF, 1.8f, 0.9f));

    // Per-icicle data stored on the projectile, so hits resolve with the caster's progress at cast time.
    private static final String TAG_LEVEL = "ea_icicle_level";
    private static final String TAG_BRANCH_5 = "ea_icicle_branch5";
    private static final String TAG_BRANCH_10 = "ea_icicle_branch10";
    private static final String TAG_VOLLEY = "ea_icicle_volley";
    private static final String TAG_HOMING = "ea_icicle_homing";
    private static final String TAG_LANCE = "ea_icicle_lance";
    private static final String TAG_SHRAPNEL = "ea_icicle_shrapnel";
    private static final String TAG_SHRAPNEL_DAMAGE = "ea_icicle_shrapnel_damage";
    private static final int SHRAPNEL_COUNT = 8;
    private static final float SHRAPNEL_DAMAGE = 0.3f;
    // Stored on targets to count hits from one volley (Deep Freeze).
    private static final String TAG_TARGET_VOLLEY = "ea_icicle_hit_volley";
    private static final String TAG_TARGET_HITS = "ea_icicle_hit_count";

    public IcicleSpell() {
        super(ModSchools.ICE, 12, 50);
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

    // ---- conjuring ----

    /** Never called: Icicle is conjured (see ConjureSpell). */
    @Override
    public CastResult cast(CastContext context) {
        return CastResult.fail(Component.translatable("message.elementalarcana.no_spell"));
    }

    @Override
    public int maxConjured(int spellLevel) {
        return icicleCount(spellLevel);
    }

    /** 12 for the first icicle of a set, 4 for each more (a full set costs what the old volley did). */
    @Override
    public int conjureCost(int spellLevel, int alreadyHeld) {
        return alreadyHeld == 0 ? manaCost() : EXTRA_ICICLE_COST;
    }

    @Override
    public SpellProjectile conjure(CastContext context, int seed) {
        int level = context.spellLevel();
        SpellProjectile icicle = SpellProjectile.summonHeld(context, this, 0, 1, chargeTicks(level));
        CompoundTag tag = icicle.getPersistentData();
        tag.putInt(TAG_LEVEL, level);
        tag.putString(TAG_BRANCH_5, orEmpty(context.branch(5)));
        tag.putString(TAG_BRANCH_10, orEmpty(context.branch(10)));
        // One "volley" is one conjured set (Deep Freeze counts hits per set).
        tag.putInt(TAG_VOLLEY, seed);
        icicle.setVariant(lookFor(level, context.branch(5), context.branch(10)) | (level >= 8 ? BRIGHT : 0));
        playAt(icicle, SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 1.3f);
        return icicle;
    }

    @Override
    public boolean canFuse(int spellLevel, Map<Integer, String> branches) {
        return LANCE.equals(branches.get(10));
    }

    /** Glacial Lance: the whole set fuses into one huge icicle in front of you. */
    @Override
    public SpellProjectile fuse(ServerPlayer caster, List<SpellProjectile> icicles) {
        ServerLevel level = caster.serverLevel();
        SpellProjectile lance = icicles.get(0);
        for (SpellProjectile other : icicles.subList(1, icicles.size())) {
            level.sendParticles(ModContent.FROST_SPARKLE.get(), other.getX(), other.getY(), other.getZ(), 10, 0.1, 0.1, 0.1, 0.05);
            level.sendParticles(ModContent.FROST_MIST.get(), other.getX(), other.getY(), other.getZ(), 2, 0.1, 0.1, 0.1, 0.01);
            other.discard();
        }
        lance.setFormation(0, 1);
        lance.setVisualScale(2.2f);
        lance.setVariant(LOOK_LANCE | BRIGHT);
        lance.getPersistentData().putBoolean(TAG_LANCE, true);
        playAt(lance, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.6f);
        playAt(lance, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.8f);
        level.sendParticles(ModContent.FROST_SPARKLE.get(), lance.getX(), lance.getY(), lance.getZ(), 30, 0.3, 0.3, 0.3, 0.15);
        return lance;
    }

    /** Fully grown: a bright chime (its flash and glints are client-side, see IcicleEffects#grown). */
    @Override
    public void onFullyGrown(SpellProjectile icicle) {
        playAt(icicle, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.6f);
    }

    @Override
    public void fizzle(SpellProjectile icicle) {
        shatter(icicle, icicle.position());
        icicle.discard();
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

    /** The look without the Lv 8+ brightness. */
    public static int look(int variant) {
        return variant & (BRIGHT - 1);
    }

    @Override
    public ResourceLocation model(int variant) {
        return LOOK_MODELS.get(look(variant));
    }

    @Override
    public List<ResourceLocation> models() {
        return LOOK_MODELS;
    }

    /** A pale cold halo; at Lv 8+ a clearly brighter, wider one. */
    @Override
    public Glow glow(int variant) {
        Glow glow = LOOK_GLOWS.get(look(variant));
        return (variant & BRIGHT) == 0 ? glow : new Glow(glow.color(), glow.size() * 1.3f, Math.max(0.8f, glow.intensity() * 1.6f));
    }

    /** The look for a caster's icicles: Endless Winter, then the Lv 5 branch, then how cold it runs. */
    private static int lookFor(int level, @Nullable String branch5, @Nullable String branch10) {
        if (WINTER.equals(branch10)) {
            return LOOK_WINTER;
        }
        if (PIERCING.equals(branch5)) {
            return LOOK_PIERCING;
        }
        if (SHATTERBURST.equals(branch5)) {
            return LOOK_SHATTER;
        }
        return level >= 8 ? LOOK_FROST_4 : level >= 5 ? LOOK_FROST_3 : level >= 3 ? LOOK_FROST_2 : LOOK_FROST_1;
    }

    /**
     * With a dynamic lights mod: icicles light up the dark as they fly, a little dimmer than fire,
     * brighter as the ice runs colder and brighter again at Lv 8+. Held, they brighten as they grow;
     * Shatterburst shrapnel just glints.
     */
    @Override
    public int luminance(SpellProjectile icicle) {
        if (icicle.visualScale() < 0.5f) {
            return 4;
        }
        int light = switch (look(icicle.variant())) {
            case LOOK_FROST_1 -> 6;
            case LOOK_FROST_2 -> 7;
            case LOOK_LANCE -> 12;
            case LOOK_WINTER -> 9;
            default -> 8;
        };
        if ((icicle.variant() & BRIGHT) != 0) {
            light += 2;
        }
        light = Math.min(15, light);
        return icicle.isHeld() ? Math.round(light * (0.5f + 0.5f * icicle.charge(0f))) : light;
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

    // ---- visuals (client only, see IcicleEffects) ----

    @Override
    public void heldParticles(SpellProjectile icicle, float charge) {
        IcicleEffects.held(icicle, charge);
    }

    @Override
    public void grownParticles(SpellProjectile icicle) {
        IcicleEffects.grown(icicle);
    }

    @Override
    public void releaseParticles(SpellProjectile icicle) {
        IcicleEffects.released(icicle);
    }

    @Override
    public void flightParticles(SpellProjectile icicle) {
        IcicleEffects.flight(icicle);
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
            shatter(icicle, icicle.position());
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
        Vec3 at = icicle.impactPoint(hit);
        if (tag.getBoolean(TAG_SHRAPNEL)) {
            // A Shatterburst shard: a small cut and a chill.
            SpellDamage.hurtMultiHit(target, SpellDamage.source(icicle.level(), Element.ICE, icicle, icicle.getOwner()), tag.getFloat(TAG_SHRAPNEL_DAMAGE));
            if (target instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0));
            }
            shatter(icicle, at);
            return;
        }
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
            // The shrapnel carries on past the target, and fans out.
            shatterburst(icicle, at, target, icicle.getDeltaMovement());
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
        shatter(icicle, at);
    }

    @Override
    public void onHitBlock(SpellProjectile icicle, BlockHitResult hit) {
        CompoundTag tag = icicle.getPersistentData();
        Vec3 at = hit.getLocation();
        if (tag.getBoolean(TAG_SHRAPNEL)) {
            shatter(icicle, at);
            return;
        }
        Freezing.freezeWater((ServerLevel) icicle.level(), hit.getBlockPos(), FREEZE_RADIUS);
        if (SHATTERBURST.equals(tag.getString(TAG_BRANCH_5)) && !tag.getBoolean(TAG_LANCE)) {
            // The shrapnel bursts back off the surface it hit.
            shatterburst(icicle, at, null, Vec3.atLowerCornerOf(hit.getDirection().getNormal()));
        }
        if (WINTER.equals(tag.getString(TAG_BRANCH_10)) && icicle.charge(0f) >= 1f) {
            frostPatch(icicle, Vec3.atBottomCenterOf(hit.getBlockPos().relative(hit.getDirection())));
        }
        shatter(icicle, at);
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

    /**
     * Shatterburst: the icicle bursts into shrapnel, real shards that fly out from where it hit
     * (onward past a creature it hit, or back off a surface), arc down, and each cut whatever they
     * strike for part of the damage. Skips the creature the icicle itself hit.
     */
    private void shatterburst(SpellProjectile icicle, Vec3 at, @Nullable Entity alreadyHit, Vec3 away) {
        Entity owner = icicle.getOwner();
        if (owner == null) {
            return;
        }
        RandomSource random = icicle.getRandom();
        Vec3 heading = away.lengthSqr() < 1.0e-6 ? new Vec3(0, 1, 0) : away.normalize();
        float damage = damage(icicle, icicle.getPersistentData()) * SHRAPNEL_DAMAGE;
        for (int i = 0; i < SHRAPNEL_COUNT; i++) {
            Vec3 spread = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 direction = heading.scale(0.8).add(spread.scale(0.9)).normalize();
            if (direction.dot(heading) < 0.1) {
                direction = direction.subtract(heading.scale(direction.dot(heading) - 0.1)).normalize();
            }
            double speed = 0.7 + random.nextDouble() * 0.35;
            SpellProjectile shard = SpellProjectile.shootFrom(owner, this, at.add(direction.scale(0.2)), direction.scale(speed), icicle.power());
            shard.setVisualScale(0.3f);
            shard.setVariant(icicle.variant());
            shard.setGravity(0.04f);
            if (alreadyHit != null) {
                shard.ignoreEntity(alreadyHit);
            }
            CompoundTag tag = shard.getPersistentData();
            tag.putBoolean(TAG_SHRAPNEL, true);
            tag.putFloat(TAG_SHRAPNEL_DAMAGE, damage);
        }
        ServerLevel level = (ServerLevel) icicle.level();
        level.sendParticles(ModContent.FROST_MIST.get(), at.x, at.y, at.z, 6, 0.4, 0.3, 0.4, 0.02);
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
    private static void shatter(SpellProjectile icicle, Vec3 at) {
        ServerLevel level = (ServerLevel) icicle.level();
        float charge = icicle.charge(0f);
        float size = icicle.visualScale();
        double x = at.x;
        double y = at.y;
        double z = at.z;
        // The whole shatter goes out as one particle; each client plays it out (IcicleEffects#shatter).
        IceShatterOptions shatter = new IceShatterOptions(icicle.variant(), size, charge);
        double reach = size > 1.5f ? 48 : 32;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < reach * reach) {
                level.sendParticles(player, shatter, true, x, y, z, 1, 0, 0, 0, 0);
            }
        }
        level.playSound(null, x, y, z, ModContent.ICICLE_IMPACT.get(), SoundSource.PLAYERS,
                Math.min(2f, (0.7f + 0.4f * charge) * size), (1.15f - 0.25f * charge) / (float) Math.sqrt(size) + (level.getRandom().nextFloat() - 0.5f) * 0.1f);
    }

    private static void playAt(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

}
