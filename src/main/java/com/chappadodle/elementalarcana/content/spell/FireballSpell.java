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
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.FireBlastOptions;
import com.chappadodle.elementalarcana.content.FireEvents;
import com.chappadodle.elementalarcana.content.FireField;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Fire's basic spell, levels 1-10. Each press of the cast key conjures a fireball that grows in
 * your palm (up to the level's maximum); "launch one" / "launch all" throws them (see Conjuring).
 * It explodes where it lands, igniting everything in the blast (no block damage).
 * Hitting frozen or frosted enemies Melts them, and wet ones Vaporize, for extra damage.
 *
 * <pre>
 * Lv1 Fireball      1.5-block burst        Lv6  Scorch        +25% dmg, +25% more vs burning
 * Lv2 Wildfire      2.5-block burst        Lv7  Combustion    burning kills explode and chain
 * Lv3 Quick Kindle  faster charge, bolts   Lv8  Triple Flames three fireballs
 * Lv4 Twin Flames   two fireballs          Lv9  Heat Haze     blasts leave burning ground
 * Lv5 branch:       Cluster Bomb | Meteor  Lv10 capstone:     Sunfire | Phoenix
 * </pre>
 */
public class FireballSpell extends Spell implements ProjectileSpell, ConjureSpell {
    public static final String CLUSTER = "cluster";
    public static final String METEOR = "meteor";
    public static final String SUNFIRE = "sunfire";
    public static final String PHOENIX = "phoenix";

    private static final ResourceLocation MODEL = ElementalArcana.id("spell/fireball");

    // Looks (SpellProjectile#variant): the fireball burns hotter as it levels, and each branch has
    // its own look (see docs/superpowers/specs/2026-09-29-fireball-vfx-design.md).
    public static final int LOOK_HEAT_1 = 0;
    public static final int LOOK_HEAT_2 = 1;
    public static final int LOOK_HEAT_3 = 2;
    public static final int LOOK_HEAT_4 = 3;
    public static final int LOOK_CLUSTER = 4;
    public static final int LOOK_METEOR = 5;
    public static final int LOOK_SUN = 6;
    public static final int LOOK_PHOENIX = 7;
    private static final List<ResourceLocation> LOOK_MODELS = List.of(MODEL,
            ElementalArcana.id("spell/fireball_heat2"), ElementalArcana.id("spell/fireball_heat3"),
            ElementalArcana.id("spell/fireball_heat4"), ElementalArcana.id("spell/fireball_cluster"),
            ElementalArcana.id("spell/fireball_meteor"), ElementalArcana.id("spell/fireball_sun"),
            ElementalArcana.id("spell/fireball_phoenix"));
    private static final List<Glow> LOOK_GLOWS = List.of(
            new Glow(0xFF5A14, 1.1f, 0.55f),
            new Glow(0xFF8C28, 1.25f, 0.7f),
            new Glow(0xFFB446, 1.4f, 0.85f),
            new Glow(0xFFD782, 1.6f, 1.0f),
            new Glow(0xFF781E, 0.9f, 0.45f),
            new Glow(0xFF6414, 1.2f, 0.6f),
            new Glow(0xFFEB96, 2.0f, 1.3f),
            new Glow(0xFF463C, 1.4f, 0.9f));
    private static final int EXTRA_FIREBALL_COST = 5;
    private static final float METEOR_GRAVITY = 0.05f;

    private static final String TAG_LEVEL = "ea_fire_level";
    private static final String TAG_BRANCH_5 = "ea_fire_branch5";
    private static final String TAG_BRANCH_10 = "ea_fire_branch10";
    private static final String TAG_SUN = "ea_fire_sun";
    private static final String TAG_BOMBLET = "ea_fire_bomblet";
    private static final String TAG_FUSE = "ea_fire_fuse";
    private static final String TAG_HOMING = "ea_fire_homing";
    private static final String TAG_REFLOWN = "ea_fire_reflown";

    public FireballSpell() {
        super(ModSchools.FIRE, 15, 60);
    }

    // ---- levels ----

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(CLUSTER, METEOR);
            case 10 -> List.of(SUNFIRE, PHOENIX);
            default -> List.of();
        };
    }

    private static int fireballCount(int level) {
        return level >= 8 ? 3 : level >= 4 ? 2 : 1;
    }

    private static int chargeTicks(int level) {
        return level >= 3 ? 15 : 20;
    }

    // ---- conjuring ----

    /** Never called: Fireball is conjured (see ConjureSpell). */
    @Override
    public CastResult cast(CastContext context) {
        return CastResult.fail(Component.translatable("message.elementalarcana.no_spell"));
    }

    @Override
    public int maxConjured(int spellLevel) {
        return fireballCount(spellLevel);
    }

    /** 15 for the first fireball of a set, 5 for each more (a full set costs what the old volley did). */
    @Override
    public int conjureCost(int spellLevel, int alreadyHeld) {
        return alreadyHeld == 0 ? manaCost() : EXTRA_FIREBALL_COST;
    }

    @Override
    public SpellProjectile conjure(CastContext context, int seed) {
        int level = context.spellLevel();
        SpellProjectile fireball = SpellProjectile.summonHeld(context, this, 0, 1, chargeTicks(level));
        CompoundTag tag = fireball.getPersistentData();
        tag.putInt(TAG_LEVEL, level);
        tag.putString(TAG_BRANCH_5, orEmpty(context.branch(5)));
        tag.putString(TAG_BRANCH_10, orEmpty(context.branch(10)));
        fireball.setVariant(lookFor(level, context.branch(5), context.branch(10)));
        playAt(context.caster(), SoundEvents.FLINTANDSTEEL_USE, 1f, 0.9f);
        return fireball;
    }

    /** Meteor: each fireball is lobbed in a high arc instead of thrown straight. */
    @Override
    public void launch(ServerPlayer caster, List<SpellProjectile> fireballs, Vec3 aim) {
        for (SpellProjectile fireball : fireballs) {
            fireball.release(aim);
            CompoundTag tag = fireball.getPersistentData();
            if (METEOR.equals(tag.getString(TAG_BRANCH_5)) && !tag.getBoolean(TAG_SUN)) {
                launchInArc(fireball, aim);
            }
        }
        playAt(caster, SoundEvents.BLAZE_SHOOT, 1f, 1.1f);
    }

    @Override
    public boolean canFuse(int spellLevel, Map<Integer, String> branches) {
        return SUNFIRE.equals(branches.get(10));
    }

    /** Sunfire: the fireballs fuse into a miniature sun floating above your head. */
    @Override
    public SpellProjectile fuse(ServerPlayer caster, List<SpellProjectile> fireballs) {
        ServerLevel level = caster.serverLevel();
        SpellProjectile sun = fireballs.get(0);
        for (SpellProjectile other : fireballs.subList(1, fireballs.size())) {
            level.sendParticles(ParticleTypes.FLAME, other.getX(), other.getY(), other.getZ(), 12, 0.1, 0.1, 0.1, 0.05);
            other.discard();
        }
        sun.setFormation(2, 3);
        sun.setVisualScale(2.8f);
        sun.setVariant(LOOK_SUN);
        sun.getPersistentData().putBoolean(TAG_SUN, true);
        playAt(caster, SoundEvents.BLAZE_AMBIENT, 1.5f, 0.6f);
        playAt(caster, SoundEvents.FIRECHARGE_USE, 1.5f, 0.6f);
        level.sendParticles(ModContent.EMBER.get(), sun.getX(), sun.getY(), sun.getZ(), 30, 0.4, 0.4, 0.4, 0.1);
        return sun;
    }

    /** Fully grown: a crackle (the flare and sparks are client-side, see FireballEffects#grown). */
    @Override
    public void onFullyGrown(SpellProjectile fireball) {
        playAt(fireball, SoundEvents.FIRECHARGE_USE, 0.6f, 1.3f);
    }

    @Override
    public void fizzle(SpellProjectile fireball) {
        ((ServerLevel) fireball.level()).sendParticles(ParticleTypes.SMOKE, fireball.getX(), fireball.getY(), fireball.getZ(), 8, 0.1, 0.1, 0.1, 0.02);
        fireball.discard();
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Override
    public ParticleOptions trailParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    public ResourceLocation model() {
        return MODEL;
    }

    @Override
    public ResourceLocation model(int variant) {
        return LOOK_MODELS.get(Math.floorMod(variant, LOOK_MODELS.size()));
    }

    @Override
    public List<ResourceLocation> models() {
        return LOOK_MODELS;
    }

    @Override
    public Glow glow(int variant) {
        return LOOK_GLOWS.get(Math.floorMod(variant, LOOK_GLOWS.size()));
    }

    /** The look for a caster's fireballs: Phoenix, then the Lv 5 branch, then how hot it burns. */
    private static int lookFor(int level, @Nullable String branch5, @Nullable String branch10) {
        if (PHOENIX.equals(branch10)) {
            return LOOK_PHOENIX;
        }
        if (CLUSTER.equals(branch5)) {
            return LOOK_CLUSTER;
        }
        if (METEOR.equals(branch5)) {
            return LOOK_METEOR;
        }
        return level >= 8 ? LOOK_HEAT_4 : level >= 5 ? LOOK_HEAT_3 : level >= 3 ? LOOK_HEAT_2 : LOOK_HEAT_1;
    }

    @Override
    public int chargeTicks() {
        return chargeTicks(1);
    }

    /** A quick tap throws a small, fast bolt; a full charge is a big, heavier fireball. */
    @Override
    public float releaseSpeed(float charge) {
        return 2.2f - 1.0f * charge;
    }

    @Override
    public int lifetimeTicks() {
        return 80;
    }

    /** In your right palm; the second in your left; a third floats above, between them. */
    @Override
    public Vec3 holdOffset(int slot, int count) {
        return switch (slot) {
            case 1 -> new Vec3(-0.5, -0.25, 0.75);
            case 2 -> new Vec3(0, 0.4, 0.9);
            default -> new Vec3(count > 1 ? 0.5 : 0.45, -0.25, count > 1 ? 0.75 : 0.8);
        };
    }

    // ---- visuals (client, see FireballEffects) ----

    @Override
    public void heldParticles(SpellProjectile fireball, float charge) {
        FireballEffects.held(fireball, charge);
    }

    @Override
    public void grownParticles(SpellProjectile fireball) {
        FireballEffects.grown(fireball);
    }

    @Override
    public void releaseParticles(SpellProjectile fireball) {
        FireballEffects.released(fireball);
    }

    @Override
    public void flightParticles(SpellProjectile fireball) {
        FireballEffects.flight(fireball);
    }

    // ---- flight (server) ----

    @Override
    public void onRelease(SpellProjectile fireball) {
        CompoundTag tag = fireball.getPersistentData();
        if (PHOENIX.equals(tag.getString(TAG_BRANCH_10)) && fireball.getOwner() instanceof ServerPlayer owner) {
            fireball.setPierce(1);
            LivingEntity target = nearestToCrosshair(owner);
            if (target != null) {
                tag.putInt(TAG_HOMING, target.getId());
            }
        }
    }

    @Override
    public void onTick(SpellProjectile fireball) {
        if (fireball.isInWater()) {
            // Fire doesn't survive water: it fizzles out in a puff of steam.
            ServerLevel level = (ServerLevel) fireball.level();
            level.sendParticles(ParticleTypes.CLOUD, fireball.getX(), fireball.getY(), fireball.getZ(), 8, 0.2, 0.2, 0.2, 0.03);
            playAt(fireball, SoundEvents.FIRE_EXTINGUISH, 0.8f, 1.2f);
            fireball.discard();
            return;
        }
        CompoundTag tag = fireball.getPersistentData();
        if (tag.getBoolean(TAG_BOMBLET) && fireball.ticksInFlight() >= tag.getInt(TAG_FUSE)) {
            explode(fireball, null, fireball.position());
            fireball.discard();
            return;
        }
        if (tag.contains(TAG_HOMING) && fireball.level().getEntity(tag.getInt(TAG_HOMING)) instanceof LivingEntity target && target.isAlive()) {
            Vec3 velocity = fireball.getDeltaMovement();
            double speed = Math.max(1.0, velocity.length());
            Vec3 wanted = target.getBoundingBox().getCenter().subtract(fireball.position()).normalize().scale(speed);
            fireball.setDeltaMovement(velocity.lerp(wanted, 0.2).normalize().scale(speed));
            fireball.hasImpulse = true;
        }
    }

    // ---- impact (server) ----

    @Override
    public void onHitEntity(SpellProjectile fireball, EntityHitResult hit) {
        CompoundTag tag = fireball.getPersistentData();
        Entity target = hit.getEntity();
        explode(fireball, target, fireball.impactPoint(hit));
        if (isMain(tag) && CLUSTER.equals(tag.getString(TAG_BRANCH_5))) {
            scatterBomblets(fireball);
        }
        if (PHOENIX.equals(tag.getString(TAG_BRANCH_10)) && !tag.getBoolean(TAG_BOMBLET)) {
            LivingEntity next = !target.isAlive() && !tag.getBoolean(TAG_REFLOWN) && fireball.getOwner() instanceof ServerPlayer owner
                    ? nearestTo(target, owner) : null;
            if (next != null) {
                // Phoenix: it made a kill, so it wheels around toward one more target.
                tag.putBoolean(TAG_REFLOWN, true);
                tag.putInt(TAG_HOMING, next.getId());
                fireball.ignoreEntity(target);
            } else {
                fireball.discard();
            }
        }
    }

    @Override
    public void onHitBlock(SpellProjectile fireball, BlockHitResult hit) {
        CompoundTag tag = fireball.getPersistentData();
        explode(fireball, null, hit.getLocation());
        if (isMain(tag)) {
            if (CLUSTER.equals(tag.getString(TAG_BRANCH_5))) {
                scatterBomblets(fireball);
            }
            ServerLevel level = (ServerLevel) fireball.level();
            BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
            if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && level.isEmptyBlock(firePos)
                    && BaseFireBlock.canBePlacedAt(level, firePos, hit.getDirection())) {
                level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
            }
        }
    }

    /** The fireball the player threw (not a Cluster Bomb bomblet). */
    private static boolean isMain(CompoundTag tag) {
        return !tag.getBoolean(TAG_BOMBLET);
    }

    /** The blast: damages, ignites and shoves everything within its radius; Melts frozen targets. */
    private static void explode(SpellProjectile fireball, @Nullable Entity directHit, Vec3 at) {
        ServerLevel level = (ServerLevel) fireball.level();
        CompoundTag tag = fireball.getPersistentData();
        int spellLevel = tag.getInt(TAG_LEVEL);
        boolean sun = tag.getBoolean(TAG_SUN);
        boolean bomblet = tag.getBoolean(TAG_BOMBLET);
        boolean meteor = METEOR.equals(tag.getString(TAG_BRANCH_5)) && !sun;
        Entity owner = fireball.getOwner();

        float charge = fireball.charge(0f);
        if (spellLevel >= 3) {
            charge = Math.max(charge, 0.3f);
        }
        double radius = sun ? 5.0 : meteor ? 4.0 : bomblet ? 1.8 : spellLevel >= 2 ? 2.5 : 1.5;
        float damage = 3f + 5f * charge;
        if (spellLevel >= 6) {
            damage *= 1.25f;
        }
        if (sun) {
            damage *= fireballCount(spellLevel) * 0.9f;
        }
        if (bomblet) {
            damage *= 0.5f;
        }
        damage *= fireball.power();
        int burnTicks = spellLevel >= 2 ? 120 : 80;
        double knockback = sun || meteor ? 1.2 : 0.4;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != owner && e.isAlive() && (e == directHit || SpellTargets.canAffect(owner, e)
                        && e.getBoundingBox().getCenter().distanceTo(at) <= radius + e.getBbWidth() / 2))) {
            // The creature it hit takes the full blast; falloff is for the splash around it.
            double falloff = target == directHit ? 1.0
                    : 1.0 - 0.5 * Math.min(1.0, target.getBoundingBox().getCenter().distanceTo(at) / radius);
            float hit = (float) (damage * falloff);
            if (spellLevel >= 6 && target.isOnFire()) {
                hit *= 1.25f;
            }
            hit *= ElementalReactions.fireHit(target);
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.FIRE, fireball, owner), hit);
            target.igniteForTicks(burnTicks);
            if (spellLevel >= 7) {
                FireEvents.markForCombustion(target);
            }
            Vec3 away = target.position().subtract(at).normalize();
            target.knockback(knockback, -away.x, -away.z);
            target.hurtMarked = true;
        }

        if (meteor && !bomblet) {
            FireField.spawn(level, at, 3.0, 100, owner);
        } else if (spellLevel >= 9 && !bomblet) {
            FireField.spawn(level, at, 2.0, 60, owner);
        }

        float size = (float) (radius / 2.0);
        // The whole explosion goes out as one particle; each client plays it out (FireballEffects#blast).
        level.sendParticles(new FireBlastOptions(fireball.variant(), (float) radius, bomblet), at.x, at.y, at.z, 1, 0, 0, 0, 0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS,
                Math.min(2.5f, 0.5f * size), 1.5f / (float) Math.sqrt(Math.max(1f, size)));
    }

    /** Cluster Bomb: four bomblets bounce away from the blast and go off one after another. */
    private void scatterBomblets(SpellProjectile fireball) {
        Entity owner = fireball.getOwner();
        if (owner == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 2 * i + fireball.getRandom().nextDouble() * 0.6;
            Vec3 velocity = new Vec3(Math.cos(angle) * 0.35, 0.45 + fireball.getRandom().nextDouble() * 0.15, Math.sin(angle) * 0.35);
            SpellProjectile bomblet = SpellProjectile.shootFrom(owner, this, fireball.position().add(0, 0.3, 0), velocity, fireball.power());
            bomblet.setVisualScale(0.45f);
            bomblet.setVariant(LOOK_CLUSTER);
            bomblet.setGravity(0.06f);
            bomblet.setBounces(2);
            CompoundTag tag = bomblet.getPersistentData();
            tag.putInt(TAG_LEVEL, fireball.getPersistentData().getInt(TAG_LEVEL));
            tag.putBoolean(TAG_BOMBLET, true);
            tag.putInt(TAG_FUSE, 18 + i * 6);
        }
    }

    /** The living thing nearest the caster's crosshair (narrow cone, 40 blocks). */
    @Nullable
    private static LivingEntity nearestToCrosshair(ServerPlayer caster) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle();
        return caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().expandTowards(look.scale(40)).inflate(4),
                        e -> e != caster && e.isAlive() && !(e instanceof Player) && caster.hasLineOfSight(e)
                                && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > Math.cos(Math.toRadians(20)))
                .stream()
                .max(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look)))
                .orElse(null);
    }

    /** The nearest other enemy to {@code from} within 12 blocks. */
    @Nullable
    private static LivingEntity nearestTo(Entity from, Entity owner) {
        return from.level().getEntitiesOfClass(LivingEntity.class, from.getBoundingBox().inflate(12),
                        e -> e != from && e != owner && e.isAlive() && !(e instanceof Player))
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(from)))
                .orElse(null);
    }

    private static void playAt(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Meteor: lob it in a high arc that comes down on the target. */
    private static void launchInArc(SpellProjectile fireball, Vec3 target) {
        Vec3 delta = target.subtract(fireball.position());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double ticks = Math.max(12, horizontal / 0.9);
        double vertical = (delta.y + 0.5 * METEOR_GRAVITY * ticks * ticks) / ticks;
        fireball.setGravity(METEOR_GRAVITY);
        fireball.setVisualScale(1.3f);
        fireball.setDeltaMovement(delta.x / ticks, vertical, delta.z / ticks);
        fireball.hasImpulse = true;
    }

    /**
     * Mob magic (Attuned creatures): throws a plain Lv 1 fireball at {@code target}, or, with
     * {@code meteor}, lobs a Meteor that comes down on it. {@code power} scales it like a player's
     * spell power.
     */
    public SpellProjectile shootForMob(Mob caster, Vec3 target, float power, boolean meteor) {
        Vec3 eye = caster.getEyePosition();
        Vec3 from = eye.add(target.subtract(eye).normalize().scale(0.6));
        SpellProjectile fireball = SpellProjectile.shootFrom(caster, this, from, target.subtract(from).normalize().scale(releaseSpeed(1f)), power);
        if (meteor) {
            fireball.setVariant(LOOK_METEOR);
            fireball.getPersistentData().putString(TAG_BRANCH_5, METEOR);
            launchInArc(fireball, target);
        }
        return fireball;
    }

}
