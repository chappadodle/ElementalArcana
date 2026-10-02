package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ConjureSpell;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Earth's basic spell, levels 1-10 (docs/superpowers/specs/2026-10-01-earth-design.md). Each press
 * of the cast key conjures a heavy boulder; "launch one" / "launch all" throws them (see
 * Conjuring). They arc, hit hard and shove far, and Crystallize anything burning, wet or frozen.
 *
 * <pre>
 * Lv1 Boulder        thrown rock        Lv6  Hardened       +25% dmg, longer Crystallize
 * Lv2 Heavy Stone    more dmg, bounces  Lv7  Rolling Stone  rolls on after its bounce
 * Lv3 Quick Hands    faster conjure     Lv8  Triad          three boulders
 * Lv4 Twin Boulders  two boulders       Lv9  Tectonic       two hits in a volley stun
 * Lv5 branch:        Landslide | Bedrock   Lv10 capstone:   Mountainfall | Avalanche
 * </pre>
 */
public class BoulderSpell extends Spell implements ProjectileSpell, ConjureSpell {
    public static final String LANDSLIDE = "landslide";
    public static final String BEDROCK = "bedrock";
    public static final String MOUNTAINFALL = "mountainfall";
    public static final String AVALANCHE = "avalanche";

    private static final ResourceLocation MODEL = ElementalArcana.id("spell/boulder");
    private static final ResourceLocation BEDROCK_MODEL = ElementalArcana.id("spell/boulder_bedrock");
    public static final int LOOK_BOULDER = 0;
    public static final int LOOK_BEDROCK = 1;
    private static final List<ResourceLocation> LOOK_MODELS = List.of(MODEL, BEDROCK_MODEL);

    private static final int EXTRA_BOULDER_COST = 6;
    private static final float HELD_SCALE = 0.55f;
    private static final float GRAVITY = 0.05f;
    // A little upward lift so a throw at the crosshair arcs over, not into, the ground.
    private static final double LIFT = 0.1;

    private static final String TAG_LEVEL = "ea_earth_level";
    private static final String TAG_BRANCH_5 = "ea_earth_branch5";
    private static final String TAG_BRANCH_10 = "ea_earth_branch10";
    private static final String TAG_VOLLEY = "ea_earth_volley";
    private static final String TAG_FUSED = "ea_earth_fused";
    private static final String TAG_ROCK = "ea_earth_rock";
    private static final String TAG_ROLLER = "ea_earth_roller";
    private static final String TAG_HITS = "ea_earth_hits";
    private static final String TAG_HITS_VOLLEY = "ea_earth_hits_volley";

    public BoulderSpell() {
        super(ModSchools.EARTH, 18, 70);
    }

    // ---- levels ----

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(LANDSLIDE, BEDROCK);
            case 10 -> List.of(MOUNTAINFALL, AVALANCHE);
            default -> List.of();
        };
    }

    private static int boulderCount(int level) {
        return level >= 8 ? 3 : level >= 4 ? 2 : 1;
    }

    private static int chargeTicks(int level) {
        return level >= 3 ? 15 : 22;
    }

    // ---- conjuring ----

    /** Never called: Boulder is conjured (see ConjureSpell). */
    @Override
    public CastResult cast(CastContext context) {
        return CastResult.fail(Component.translatable("message.elementalarcana.no_spell"));
    }

    @Override
    public int maxConjured(int spellLevel) {
        return boulderCount(spellLevel);
    }

    /** 18 for the first boulder of a set, 6 for each more. */
    @Override
    public int conjureCost(int spellLevel, int alreadyHeld) {
        return alreadyHeld == 0 ? manaCost() : EXTRA_BOULDER_COST;
    }

    @Override
    public SpellProjectile conjure(CastContext context, int seed) {
        int level = context.spellLevel();
        SpellProjectile boulder = SpellProjectile.summonHeld(context, this, 0, 1, chargeTicks(level));
        CompoundTag tag = boulder.getPersistentData();
        tag.putInt(TAG_LEVEL, level);
        tag.putString(TAG_BRANCH_5, orEmpty(context.branch(5)));
        tag.putString(TAG_BRANCH_10, orEmpty(context.branch(10)));
        tag.putInt(TAG_VOLLEY, seed);
        if (BEDROCK.equals(context.branch(5))) {
            boulder.setVariant(LOOK_BEDROCK);
        }
        // Held small and low so it doesn't fill the view; full size once thrown (see onRelease).
        boulder.setVisualScale(HELD_SCALE);
        return boulder;
    }

    @Override
    public void launch(ServerPlayer caster, List<SpellProjectile> boulders, Vec3 aim) {
        for (SpellProjectile boulder : boulders) {
            boulder.release(aim);
            if (boulder.getPersistentData().getBoolean(TAG_FUSED)) {
                // Mountainfall: the huge boulder is lobbed high and comes down on the target.
                lobOnto(boulder, aim);
            }
        }
    }

    @Override
    public boolean canFuse(int spellLevel, Map<Integer, String> branches) {
        return MOUNTAINFALL.equals(branches.get(10));
    }

    /** Mountainfall: the held boulders fuse into one huge boulder. */
    @Override
    public SpellProjectile fuse(ServerPlayer caster, List<SpellProjectile> boulders) {
        ServerLevel level = caster.serverLevel();
        SpellProjectile mountain = boulders.get(0);
        for (SpellProjectile other : boulders.subList(1, boulders.size())) {
            level.sendParticles(rubble(), other.getX(), other.getY(), other.getZ(), 14, 0.2, 0.2, 0.2, 0.05);
            other.discard();
        }
        mountain.setFormation(2, 3);
        mountain.setVisualScale(3f);
        mountain.getPersistentData().putBoolean(TAG_FUSED, true);
        mountain.getPersistentData().putInt(TAG_HITS, boulders.size());
        play(caster, SoundEvents.ANVIL_LAND, 1.2f, 0.5f);
        play(caster, SoundEvents.STONE_BREAK, 1.5f, 0.5f);
        return mountain;
    }

    @Override
    public void fizzle(SpellProjectile boulder) {
        ((ServerLevel) boulder.level()).sendParticles(rubble(), boulder.getX(), boulder.getY(), boulder.getZ(), 10, 0.2, 0.2, 0.2, 0.03);
        boulder.discard();
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    // ---- ProjectileSpell: how it looks and flies ----

    @Override
    public ParticleOptions trailParticle() {
        return rubble();
    }

    private static ParticleOptions rubble() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
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
    public int chargeTicks() {
        return chargeTicks(1);
    }

    @Override
    public float releaseSpeed(float charge) {
        return 1.7f - 0.4f * charge;
    }

    @Override
    public int lifetimeTicks() {
        return 100;
    }

    @Override
    public Vec3 holdOffset(int slot, int count) {
        return switch (slot) {
            case 1 -> new Vec3(-0.6, -0.35, 1.1);
            case 2 -> new Vec3(0, 0.45, 1.2);
            default -> new Vec3(count > 1 ? 0.6 : 0.55, -0.35, 1.1);
        };
    }

    @Override
    public void flightParticles(SpellProjectile boulder) {
        if (boulder.tickCount % 2 == 0) {
            boulder.spawnParticleAround(rubble(), 0.2 * boulder.visualScale(), Vec3.ZERO);
        }
    }

    // ---- flight (server) ----

    /** A thrown boulder arcs, and from Lv 2 bounces once. */
    @Override
    public void onRelease(SpellProjectile boulder) {
        CompoundTag tag = boulder.getPersistentData();
        if (!tag.getBoolean(TAG_FUSED)) {
            boulder.setVisualScale(BEDROCK.equals(tag.getString(TAG_BRANCH_5)) ? 1.4f : 1f);
        }
        boulder.setGravity(GRAVITY);
        boulder.setBounces(tag.getInt(TAG_LEVEL) >= 2 ? 1 : 0);
        Vec3 velocity = boulder.getDeltaMovement();
        double slow = BEDROCK.equals(tag.getString(TAG_BRANCH_5)) ? 0.75 : 1.0;
        boulder.setDeltaMovement(velocity.x * slow, velocity.y * slow + LIFT, velocity.z * slow);
        boulder.hasImpulse = true;
    }

    /** Mountainfall: a high lob that comes down on {@code target}. */
    private static void lobOnto(SpellProjectile boulder, Vec3 target) {
        Vec3 delta = target.subtract(boulder.position());
        double horizontal = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double ticks = Math.max(14, horizontal / 0.8);
        double vertical = (delta.y + 0.5 * GRAVITY * ticks * ticks) / ticks;
        boulder.setBounces(0);
        boulder.setGravity(GRAVITY);
        boulder.setDeltaMovement(delta.x / ticks, vertical, delta.z / ticks);
        boulder.hasImpulse = true;
    }

    // ---- impact (server) ----

    @Override
    public void onHitEntity(SpellProjectile boulder, EntityHitResult hit) {
        Entity target = hit.getEntity();
        crush(boulder, target, boulder.impactPoint(hit));
        afterImpact(boulder, boulder.impactPoint(hit), null);
    }

    @Override
    public void onHitBlock(SpellProjectile boulder, BlockHitResult hit) {
        crush(boulder, null, hit.getLocation());
        afterImpact(boulder, hit.getLocation(), hit.getDirection());
    }

    /** Landslide, Avalanche and Rolling Stone, for the boulder the player threw. */
    private void afterImpact(SpellProjectile boulder, Vec3 at, @Nullable Direction face) {
        CompoundTag tag = boulder.getPersistentData();
        Entity owner = boulder.getOwner();
        if (owner == null || tag.getBoolean(TAG_ROCK) || tag.getBoolean(TAG_ROLLER)) {
            return;
        }
        int level = tag.getInt(TAG_LEVEL);
        if (LANDSLIDE.equals(tag.getString(TAG_BRANCH_5))) {
            scatterRocks(boulder, at);
        }
        if (AVALANCHE.equals(tag.getString(TAG_BRANCH_10)) && level >= 10) {
            Rockfall.start((ServerLevel) boulder.level(), at, owner, boulder.power(), level);
        }
        if (level >= 7 && face == Direction.UP) {
            roll(boulder, at);
        }
    }

    /** Landslide: the boulder shatters into five rocks that spread out and keep hitting. */
    private void scatterRocks(SpellProjectile boulder, Vec3 at) {
        Entity owner = boulder.getOwner();
        for (int i = 0; i < 5; i++) {
            double angle = Math.PI * 2 / 5 * i + boulder.getRandom().nextDouble() * 0.5;
            Vec3 velocity = new Vec3(Math.cos(angle) * 0.4, 0.25 + boulder.getRandom().nextDouble() * 0.1, Math.sin(angle) * 0.4);
            SpellProjectile rock = SpellProjectile.shootFrom(owner, this, at.add(0, 0.3, 0), velocity, boulder.power());
            rock.setVisualScale(0.5f);
            rock.setGravity(0.06f);
            rock.setBounces(1);
            rock.setPierce(1);
            CompoundTag tag = rock.getPersistentData();
            tag.putInt(TAG_LEVEL, boulder.getPersistentData().getInt(TAG_LEVEL));
            tag.putBoolean(TAG_ROCK, true);
        }
    }

    /** Rolling Stone: after landing it rolls on along the ground, crushing what's in the way. */
    private void roll(SpellProjectile boulder, Vec3 at) {
        Vec3 heading = boulder.getDeltaMovement().multiply(1, 0, 1);
        if (heading.lengthSqr() < 1.0e-4) {
            return;
        }
        SpellProjectile roller = SpellProjectile.shootFrom(boulder.getOwner(), this, at.add(0, 0.4, 0), heading.normalize().scale(0.45), boulder.power());
        roller.setVisualScale(0.8f);
        roller.setGravity(0.06f);
        roller.setPierce(4);
        CompoundTag tag = roller.getPersistentData();
        tag.putInt(TAG_LEVEL, boulder.getPersistentData().getInt(TAG_LEVEL));
        tag.putBoolean(TAG_ROLLER, true);
    }

    /** A rock falling out of the sky (Avalanche). */
    static void dropRock(Entity owner, Vec3 from, float power, int level) {
        BoulderSpell spell = (BoulderSpell) com.chappadodle.elementalarcana.content.ModSpells.BOULDER.get();
        SpellProjectile rock = SpellProjectile.shootFrom(owner, spell, from, new Vec3(0, -0.9, 0), power);
        rock.setVisualScale(0.7f);
        rock.setGravity(0.04f);
        CompoundTag tag = rock.getPersistentData();
        tag.putInt(TAG_LEVEL, level);
        tag.putBoolean(TAG_ROCK, true);
    }

    /** The crush: damages, shoves and stuns everything in the blast, and Crystallizes burning, wet or frozen targets. */
    private static void crush(SpellProjectile boulder, @Nullable Entity directHit, Vec3 at) {
        ServerLevel level = (ServerLevel) boulder.level();
        CompoundTag tag = boulder.getPersistentData();
        int spellLevel = tag.getInt(TAG_LEVEL);
        boolean bedrock = BEDROCK.equals(tag.getString(TAG_BRANCH_5));
        boolean fused = tag.getBoolean(TAG_FUSED);
        boolean rock = tag.getBoolean(TAG_ROCK);
        boolean roller = tag.getBoolean(TAG_ROLLER);
        Entity owner = boulder.getOwner();

        float charge = boulder.charge(0f);
        if (spellLevel >= 3) {
            charge = Math.max(charge, 0.3f);
        }
        double radius = fused ? 5.0 : bedrock ? 2.2 : rock || roller ? 1.2 : spellLevel >= 2 ? 1.8 : 1.4;
        float damage = 4f + 5f * charge;
        if (spellLevel >= 2) {
            damage *= 1.2f;
        }
        if (spellLevel >= 6) {
            damage *= 1.25f;
        }
        if (bedrock) {
            damage *= 1.6f;
        }
        if (fused) {
            damage *= tag.getInt(TAG_HITS) * 0.9f;
        }
        if (rock) {
            damage *= 0.5f;
        } else if (roller) {
            damage *= 0.6f;
        }
        damage *= boulder.power();
        double knockback = bedrock || fused ? 1.6 : rock || roller ? 0.5 : 1.0;
        int shardTicks = spellLevel >= 6 ? 300 : 200;

        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                e -> e != owner && e.isAlive() && (e == directHit || SpellTargets.canAffect(owner, e)
                        && e.getBoundingBox().getCenter().distanceTo(at) <= radius + e.getBbWidth() / 2))) {
            double falloff = target == directHit ? 1.0
                    : 1.0 - 0.5 * Math.min(1.0, target.getBoundingBox().getCenter().distanceTo(at) / radius);
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.EARTH, boulder, owner), (float) (damage * falloff));
            ElementalReactions.earthHit(target, owner, shardTicks);
            Vec3 away = target.position().subtract(at).normalize();
            target.knockback(knockback, -away.x, -away.z);
            target.hurtMarked = true;
            if (bedrock && !rock) {
                stun(target, 20);
            }
            if (spellLevel >= 9 && !rock && !roller) {
                tectonic(target, tag.getInt(TAG_VOLLEY), level.getGameTime());
            }
        }

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), at.x, at.y + 0.2, at.z,
                rock ? 12 : 30 + (int) (radius * 8), radius * 0.4, 0.3, radius * 0.4, 0.15);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y + 0.2, at.z, rock ? 2 : 6, radius * 0.3, 0.2, radius * 0.3, 0.02);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.STONE_BREAK, SoundSource.PLAYERS, rock ? 0.8f : 1.4f, rock ? 1.0f : 0.6f);
        if (!rock) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f);
        }
    }

    /** Tectonic: the second hit from one volley stuns (1.5 s). */
    private static void tectonic(LivingEntity target, int volley, long now) {
        CompoundTag data = target.getPersistentData();
        if (data.getInt(TAG_HITS_VOLLEY) != volley || now - data.getLong(TAG_HITS_VOLLEY + "_at") > 60) {
            data.putInt(TAG_HITS_VOLLEY, volley);
            data.putInt(TAG_HITS, 0);
        }
        data.putLong(TAG_HITS_VOLLEY + "_at", now);
        int hits = data.getInt(TAG_HITS) + 1;
        data.putInt(TAG_HITS, hits);
        if (hits == 2) {
            stun(target, 30);
        }
    }

    /** Roots a creature in place for {@code ticks}. */
    static void stun(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.JUMP, ticks, 128, false, false, false));
    }

    private static void play(Entity at, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Mob magic (Attuned creatures): throws a plain Lv 1 boulder at {@code target}. */
    public SpellProjectile shootForMob(Mob caster, Vec3 target, float power) {
        Vec3 eye = caster.getEyePosition();
        Vec3 from = eye.add(target.subtract(eye).normalize().scale(0.6));
        Vec3 velocity = target.subtract(from).normalize().scale(releaseSpeed(1f));
        SpellProjectile boulder = SpellProjectile.shootFrom(caster, this, from, velocity.add(0, LIFT, 0), power);
        boulder.setGravity(GRAVITY);
        boulder.getPersistentData().putInt(TAG_LEVEL, 1);
        return boulder;
    }
}
