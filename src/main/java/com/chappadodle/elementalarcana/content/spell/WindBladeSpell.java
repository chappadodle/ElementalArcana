package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ConjureSpell;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.content.WindVortex;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Wind's basic spell, levels 1-10. Each press of the cast key conjures a crescent of wind in front
 * of you (up to the level's maximum); "launch one" / "launch all" slices them toward your crosshair,
 * a volley fanned out, knocking back what they cut (see Conjuring).
 *
 * <pre>
 * Lv1 Wind Blade     one crescent            Lv6  Keen Wind     +25% dmg; full charge = Airborne
 * Lv2 Twin Crescents two, in a V             Lv7  Slipstream    a speed burst when you fire
 * Lv3 Honed Edge     pierce 1; faster charge Lv8  Gale Fan      five crescents
 * Lv4 Cross Cut      three, fanned           Lv9  Vacuum Edge   double damage to Airborne targets
 * Lv5 branch:        Boomerang | Tempest Edge   Lv10 capstone: Storm Scythe | Thousand Cuts
 * </pre>
 * Every hit Swirls (spreads the target's element to creatures nearby).
 */
public class WindBladeSpell extends Spell implements ProjectileSpell, ConjureSpell {
    public static final String BOOMERANG = "boomerang";
    public static final String TEMPEST = "tempest";
    public static final String SCYTHE = "scythe";
    public static final String THOUSAND_CUTS = "thousand_cuts";


    // Looks (SpellProjectile#variant): the air gets sharper and brighter as it levels, and each
    // branch has its own look. Drawn by client/visual/WindSlashRenderer, not a block model
    // (see docs/superpowers/specs/2026-09-30-wind-blade-vfx-design.md).
    public static final int LOOK_GUST = 0;
    public static final int LOOK_BREEZE = 1;
    public static final int LOOK_GALE = 2;
    public static final int LOOK_TEMPEST = 3;
    public static final int LOOK_BOOMERANG = 4;
    public static final int LOOK_TEMPEST_EDGE = 5;
    public static final int LOOK_THOUSAND_CUTS = 6;
    public static final int LOOK_SCYTHE = 7;
    /** Added to the look at Lv 8+: the edge shines brighter. */
    public static final int BRIGHT = 8;
    private static final int EXTRA_BLADE_COST = 3;
    private static final int RANGE_TICKS = 20;
    private static final int BOOMERANG_TURN_TICKS = 10;

    private static final String TAG_LEVEL = "ea_wind_level";
    private static final String TAG_BRANCH_5 = "ea_wind_branch5";
    private static final String TAG_BRANCH_10 = "ea_wind_branch10";
    private static final String TAG_SCYTHE = "ea_wind_scythe";
    private static final String TAG_CHILD = "ea_wind_child";
    private static final String TAG_RETURNING = "ea_wind_returning";

    public WindBladeSpell() {
        super(ModSchools.WIND, 10, 40);
    }

    // ---- levels ----

    @Override
    public int maxLevel() {
        return 10;
    }

    @Override
    public List<String> branchOptions(int level) {
        return switch (level) {
            case 5 -> List.of(BOOMERANG, TEMPEST);
            case 10 -> List.of(SCYTHE, THOUSAND_CUTS);
            default -> List.of();
        };
    }

    private static int bladeCount(int level) {
        return level >= 8 ? 5 : level >= 4 ? 3 : level >= 2 ? 2 : 1;
    }

    private static int chargeTicks(int level) {
        return level >= 3 ? 15 : 20;
    }

    // ---- conjuring ----

    /** Never called: Wind Blade is conjured (see ConjureSpell). */
    @Override
    public CastResult cast(CastContext context) {
        return CastResult.fail(Component.translatable("message.elementalarcana.no_spell"));
    }

    @Override
    public int maxConjured(int spellLevel) {
        return bladeCount(spellLevel);
    }

    /** 10 for the first blade of a set, 3 for each more (a full set costs what the old volley did). */
    @Override
    public int conjureCost(int spellLevel, int alreadyHeld) {
        return alreadyHeld == 0 ? manaCost() : EXTRA_BLADE_COST;
    }

    @Override
    public SpellProjectile conjure(CastContext context, int seed) {
        int level = context.spellLevel();
        SpellProjectile blade = SpellProjectile.summonHeld(context, this, 0, 1, chargeTicks(level));
        CompoundTag tag = blade.getPersistentData();
        tag.putInt(TAG_LEVEL, level);
        tag.putString(TAG_BRANCH_5, orEmpty(context.branch(5)));
        tag.putString(TAG_BRANCH_10, orEmpty(context.branch(10)));
        blade.setVariant(lookFor(level, context.branch(5), context.branch(10)) | (level >= 8 ? BRIGHT : 0));
        playAt(context.caster(), SoundEvents.BREEZE_INHALE, 0.8f, 1.3f);
        return blade;
    }

    /**
     * A volley fans out horizontally around the aim, each blade toward its own side (by its place in
     * the formation); a single blade flies straight at the crosshair.
     */
    @Override
    public void launch(ServerPlayer caster, List<SpellProjectile> blades, Vec3 aim) {
        for (SpellProjectile blade : blades) {
            int count = blade.formationCount();
            float spread = blades.size() <= 1 || count <= 1 ? 0f
                    : (blade.formationSlot() - (count - 1) / 2f) * (count >= 5 ? 10f : 12f);
            Vec3 toAim = aim.subtract(blade.position()).yRot(spread * Mth.DEG_TO_RAD);
            blade.release(blade.position().add(toAim));
        }
        playAt(caster, SoundEvents.BREEZE_SHOOT, 0.9f, 1.2f);
        if (blades.get(0).getPersistentData().getInt(TAG_LEVEL) >= 7) {
            // Slipstream: a burst of speed as the wind leaves your hands.
            caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
        }
    }

    @Override
    public boolean canFuse(int spellLevel, Map<Integer, String> branches) {
        return SCYTHE.equals(branches.get(10));
    }

    /** Storm Scythe: the fan of blades merges into one giant crescent. */
    @Override
    public SpellProjectile fuse(ServerPlayer caster, List<SpellProjectile> blades) {
        ServerLevel level = caster.serverLevel();
        SpellProjectile scythe = blades.get(blades.size() / 2);
        for (SpellProjectile other : blades) {
            if (other != scythe) {
                level.sendParticles(ModContent.WIND_STREAK.get(), other.getX(), other.getY(), other.getZ(), 6, 0.1, 0.1, 0.1, 0.1);
                other.discard();
            }
        }
        scythe.setFormation(0, 1);
        scythe.setVisualScale(2.5f);
        scythe.setVariant(LOOK_SCYTHE | BRIGHT);
        scythe.getPersistentData().putBoolean(TAG_SCYTHE, true);
        playAt(caster, SoundEvents.BREEZE_WHIRL, 1.2f, 0.7f);
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, scythe.getX(), scythe.getY(), scythe.getZ(), 1, 0, 0, 0, 0);
        return scythe;
    }

    /** Fully grown: the wind catches with a rush. */
    @Override
    public void onFullyGrown(SpellProjectile blade) {
        playAt(blade, SoundEvents.BREEZE_CHARGE, 0.6f, 1.4f);
    }

    @Override
    public void fizzle(SpellProjectile blade) {
        impact(blade);
        blade.discard();
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Override
    public ParticleOptions trailParticle() {
        return ModContent.WIND_STREAK.get();
    }

    /** The look without the Lv 8+ brightness. */
    public static int look(int variant) {
        return variant & (BRIGHT - 1);
    }

    /** The look for a caster's blades: Thousand Cuts, then the Lv 5 branch, then how keen the wind is. */
    private static int lookFor(int level, @Nullable String branch5, @Nullable String branch10) {
        if (THOUSAND_CUTS.equals(branch10)) {
            return LOOK_THOUSAND_CUTS;
        }
        if (BOOMERANG.equals(branch5)) {
            return LOOK_BOOMERANG;
        }
        if (TEMPEST.equals(branch5)) {
            return LOOK_TEMPEST_EDGE;
        }
        return level >= 8 ? LOOK_TEMPEST : level >= 5 ? LOOK_GALE : level >= 3 ? LOOK_BREEZE : LOOK_GUST;
    }

    /** With a dynamic lights mod: air doesn't glow, except a faint Lv 8+ edge and the Storm Scythe's lightning. */
    @Override
    public int luminance(SpellProjectile blade) {
        if (look(blade.variant()) == LOOK_SCYTHE) {
            return 9;
        }
        return (blade.variant() & BRIGHT) != 0 ? 4 : 0;
    }

    @Override
    public int chargeTicks() {
        return chargeTicks(1);
    }

    @Override
    public float releaseSpeed(float charge) {
        return 1.6f + 1.2f * charge;
    }

    @Override
    public int lifetimeTicks() {
        // Long enough for a boomerang to come back; normal blades end themselves after RANGE_TICKS.
        return RANGE_TICKS * 2 + 10;
    }

    /** One crescent in front of you at chest height; more spread out side by side. */
    @Override
    public Vec3 holdOffset(int slot, int count) {
        if (count <= 1) {
            return new Vec3(0, -0.3, 1.2);
        }
        double x = -0.8 + 1.6 * slot / (count - 1);
        return new Vec3(x, -0.3, 1.15 - 0.2 * Math.abs(x));
    }

    // ---- visuals (client only, see WindBladeEffects) ----

    @Override
    public void heldParticles(SpellProjectile blade, float charge) {
        WindBladeEffects.held(blade, charge);
    }

    @Override
    public void grownParticles(SpellProjectile blade) {
        WindBladeEffects.grown(blade);
    }

    @Override
    public void releaseParticles(SpellProjectile blade) {
        WindBladeEffects.released(blade);
    }

    @Override
    public void flightParticles(SpellProjectile blade) {
        WindBladeEffects.flight(blade);
    }

    // ---- flight (server) ----

    @Override
    public void onRelease(SpellProjectile blade) {
        CompoundTag tag = blade.getPersistentData();
        int level = tag.getInt(TAG_LEVEL);
        if (tag.getBoolean(TAG_SCYTHE)) {
            blade.setPierce(Integer.MAX_VALUE);
            blade.setDeltaMovement(blade.getDeltaMovement().scale(1.2));
        } else if (BOOMERANG.equals(tag.getString(TAG_BRANCH_5))) {
            blade.setPierce(3);
        } else if (level >= 3) {
            blade.setPierce(1);
        }
    }

    @Override
    public void onTick(SpellProjectile blade) {
        CompoundTag tag = blade.getPersistentData();
        boolean boomerang = BOOMERANG.equals(tag.getString(TAG_BRANCH_5)) && !tag.getBoolean(TAG_SCYTHE) && !tag.getBoolean(TAG_CHILD);
        int flight = blade.ticksInFlight();
        if (!boomerang) {
            if (flight > RANGE_TICKS) {
                blade.discard();
            }
            return;
        }
        Entity owner = blade.getOwner();
        if (owner == null) {
            blade.discard();
            return;
        }
        if (flight == BOOMERANG_TURN_TICKS) {
            tag.putBoolean(TAG_RETURNING, true);
            blade.resetPierced();
            blade.setPierce(3);
        }
        if (tag.getBoolean(TAG_RETURNING)) {
            Vec3 home = owner.getEyePosition().subtract(0, 0.4, 0);
            if (blade.position().distanceTo(home) < 1.5) {
                blade.discard();
                return;
            }
            double speed = Math.max(1.4, blade.getDeltaMovement().length());
            Vec3 wanted = home.subtract(blade.position()).normalize().scale(speed);
            blade.setDeltaMovement(blade.getDeltaMovement().lerp(wanted, 0.35));
            blade.hasImpulse = true;
        }
    }

    // ---- hits (server) ----

    private static float damage(SpellProjectile blade, CompoundTag tag, LivingEntity target) {
        int level = tag.getInt(TAG_LEVEL);
        float charge = blade.charge(0f);
        if (level >= 3) {
            charge = Math.max(charge, 0.3f);
        }
        float damage = 2f + 4f * charge;
        if (level >= 6) {
            damage *= 1.25f;
        }
        if (tag.getBoolean(TAG_SCYTHE)) {
            damage *= bladeCount(level) * 0.8f;
        }
        if (tag.getBoolean(TAG_CHILD)) {
            damage *= 0.5f;
        }
        if (level >= 9 && target.hasEffect(ModContent.AIRBORNE)) {
            damage *= 2f;
        }
        return damage * blade.power();
    }

    @Override
    public void onHitEntity(SpellProjectile blade, EntityHitResult hit) {
        if (!(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        CompoundTag tag = blade.getPersistentData();
        int level = tag.getInt(TAG_LEVEL);
        float charge = blade.charge(0f);
        Entity owner = blade.getOwner();

        SpellDamage.hurtMultiHit(target, SpellDamage.source(blade.level(), Element.WIND, blade, owner), damage(blade, tag, target));
        Vec3 direction = blade.getDeltaMovement().normalize();
        double knockback = (0.3 + 0.9 * charge) * (tag.getBoolean(TAG_SCYTHE) ? 2 : 1);
        target.knockback(knockback, -direction.x, -direction.z);
        target.hurtMarked = true;
        if (level >= 6 && charge >= 1f) {
            ElementalReactions.launchAirborne(target, 0.55);
        }
        ElementalReactions.swirl(target, owner, blade.power());

        if (TEMPEST.equals(tag.getString(TAG_BRANCH_5)) && charge >= 1f && !tag.getBoolean(TAG_CHILD)) {
            WindVortex.spawn((ServerLevel) blade.level(), target.position(), 4.0, 0.35, 20, owner);
        }
        if (THOUSAND_CUTS.equals(tag.getString(TAG_BRANCH_10)) && !tag.getBoolean(TAG_CHILD)) {
            splitTowardOthers(blade, target);
        }
        impact(blade);
    }

    @Override
    public void onHitBlock(SpellProjectile blade, BlockHitResult hit) {
        CompoundTag tag = blade.getPersistentData();
        if (TEMPEST.equals(tag.getString(TAG_BRANCH_5)) && blade.charge(0f) >= 1f && !tag.getBoolean(TAG_CHILD)) {
            WindVortex.spawn((ServerLevel) blade.level(), hit.getLocation(), 4.0, 0.35, 20, blade.getOwner());
        }
        impact(blade);
    }

    /** Thousand Cuts: the blade splits into two smaller blades that seek the nearest other enemies. */
    private void splitTowardOthers(SpellProjectile blade, LivingEntity hitTarget) {
        Entity owner = blade.getOwner();
        if (owner == null) {
            return;
        }
        List<LivingEntity> others = blade.level().getEntitiesOfClass(LivingEntity.class, hitTarget.getBoundingBox().inflate(8),
                e -> e != hitTarget && e != owner && !(e instanceof Player) && e.isAlive());
        others.sort(Comparator.comparingDouble(e -> e.distanceToSqr(hitTarget)));
        for (LivingEntity next : others.subList(0, Math.min(2, others.size()))) {
            Vec3 from = hitTarget.getBoundingBox().getCenter();
            Vec3 velocity = next.getBoundingBox().getCenter().subtract(from).normalize().scale(1.8);
            SpellProjectile child = SpellProjectile.shootFrom(owner, this, from, velocity, blade.power());
            child.setVisualScale(0.6f);
            child.setVariant(LOOK_THOUSAND_CUTS | (blade.variant() & BRIGHT));
            child.ignoreEntity(hitTarget);
            CompoundTag childTag = child.getPersistentData();
            childTag.putInt(TAG_LEVEL, blade.getPersistentData().getInt(TAG_LEVEL));
            childTag.putBoolean(TAG_CHILD, true);
        }
    }

    private static void impact(SpellProjectile blade) {
        ServerLevel level = (ServerLevel) blade.level();
        float size = blade.visualScale();
        level.sendParticles(ParticleTypes.GUST, blade.getX(), blade.getY(), blade.getZ(), 1, 0, 0, 0, 0);
        level.sendParticles(ModContent.WIND_STREAK.get(), blade.getX(), blade.getY(), blade.getZ(), Math.round(8 * size), 0.3 * size, 0.3 * size, 0.3 * size, 0.2);
        level.playSound(null, blade.getX(), blade.getY(), blade.getZ(), SoundEvents.WIND_CHARGE_BURST.value(), SoundSource.PLAYERS,
                Math.min(1.5f, 0.5f * size), 1.4f / (float) Math.sqrt(size));
    }

    private static void playAt(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

}
