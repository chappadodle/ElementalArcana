package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.chappadodle.elementalarcana.content.WindVortex;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Wind's basic spell, levels 1-10. Hold the cast key: crescents of wind form in front of you;
 * release and they slice toward your crosshair, fanned out, knocking back what they cut.
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
public class WindBladeSpell extends Spell implements ProjectileSpell {
    public static final String BOOMERANG = "boomerang";
    public static final String TEMPEST = "tempest";
    public static final String SCYTHE = "scythe";
    public static final String THOUSAND_CUTS = "thousand_cuts";

    private static final ResourceLocation MODEL = ElementalArcana.id("spell/wind_blade");
    private static final int SCYTHE_EXTRA_HOLD = 20;
    private static final int RANGE_TICKS = 20;
    private static final int BOOMERANG_TURN_TICKS = 10;

    private static final String TAG_LEVEL = "ea_wind_level";
    private static final String TAG_BRANCH_5 = "ea_wind_branch5";
    private static final String TAG_BRANCH_10 = "ea_wind_branch10";
    private static final String TAG_SCYTHE = "ea_wind_scythe";
    private static final String TAG_CHILD = "ea_wind_child";
    private static final String TAG_RETURNING = "ea_wind_returning";

    public WindBladeSpell() {
        super(ModSchools.WIND, 10, 10);
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

    @Override
    public int manaCost(int spellLevel) {
        return 10 + 3 * (bladeCount(spellLevel) - 1);
    }

    // ---- casting ----

    @Override
    public CastResult cast(CastContext context) {
        int level = context.spellLevel();
        int count = bladeCount(level);
        List<SpellProjectile> blades = new ArrayList<>();
        for (int slot = 0; slot < count; slot++) {
            SpellProjectile blade = SpellProjectile.summonHeld(context, this, slot, count, chargeTicks(level));
            CompoundTag tag = blade.getPersistentData();
            tag.putInt(TAG_LEVEL, level);
            tag.putString(TAG_BRANCH_5, orEmpty(context.branch(5)));
            tag.putString(TAG_BRANCH_10, orEmpty(context.branch(10)));
            blades.add(blade);
        }
        context.holdUntilRelease(new Hold(context.caster(), blades, chargeTicks(level), level, context.hasBranch(10, SCYTHE)));
        playAt(context.caster(), SoundEvents.BREEZE_INHALE, 0.8f, 1.3f);
        return CastResult.SUCCESS;
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    @Override
    public ParticleOptions trailParticle() {
        return ModContent.WIND_STREAK.get();
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

    // ---- visuals (client) ----

    @Override
    public void heldParticles(SpellProjectile blade, float charge) {
        int every = blade.formationCount() > 2 ? 3 : 2;
        if (blade.tickCount % every == 0) {
            float angle = blade.tickCount * 0.7f;
            Vec3 swirl = new Vec3(-Mth.sin(angle), 0.02, Mth.cos(angle)).scale(0.08 + 0.1 * charge);
            blade.spawnParticleAround(ModContent.WIND_STREAK.get(), 0.35, swirl);
        }
    }

    @Override
    public void flightParticles(SpellProjectile blade) {
        Vec3 back = blade.getDeltaMovement().scale(-0.08);
        int count = blade.visualScale() > 1.5f ? 4 : 1 + (blade.tickCount % 2);
        for (int i = 0; i < count; i++) {
            blade.spawnParticleAround(ModContent.WIND_STREAK.get(), 0.25 * blade.visualScale(), back);
        }
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

        SpellDamage.hurtMultiHit(target, blade.damageSources().indirectMagic(blade, owner), damage(blade, tag, target));
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

    // ---- the hold ----

    private static final class Hold implements SpellHold {
        private final ServerPlayer caster;
        private final List<SpellProjectile> blades;
        private final int chargeTicks;
        private final int level;
        private final boolean canForgeScythe;
        private boolean scytheForged;

        Hold(ServerPlayer caster, List<SpellProjectile> blades, int chargeTicks, int level, boolean canForgeScythe) {
            this.caster = caster;
            this.blades = blades;
            this.chargeTicks = chargeTicks;
            this.level = level;
            this.canForgeScythe = canForgeScythe && blades.size() > 1;
        }

        @Override
        public boolean tick(int heldTicks) {
            blades.removeIf(blade -> !blade.isAlive());
            if (blades.isEmpty()) {
                return false;
            }
            if (heldTicks == chargeTicks) {
                playAt(caster, SoundEvents.BREEZE_CHARGE, 0.8f, 1.4f);
                for (SpellProjectile blade : blades) {
                    ((ServerLevel) blade.level()).sendParticles(ModContent.WIND_STREAK.get(), blade.getX(), blade.getY(), blade.getZ(), 8, 0.1, 0.1, 0.1, 0.15);
                }
            }
            if (canForgeScythe && !scytheForged && heldTicks == chargeTicks + SCYTHE_EXTRA_HOLD) {
                forgeScythe();
            }
            return true;
        }

        /** Storm Scythe: the fan of blades merges into one giant crescent. */
        private void forgeScythe() {
            scytheForged = true;
            ServerLevel level = caster.serverLevel();
            SpellProjectile scythe = blades.get(blades.size() / 2);
            for (SpellProjectile other : blades) {
                if (other != scythe) {
                    level.sendParticles(ModContent.WIND_STREAK.get(), other.getX(), other.getY(), other.getZ(), 6, 0.1, 0.1, 0.1, 0.1);
                    other.discard();
                }
            }
            blades.clear();
            blades.add(scythe);
            scythe.setFormation(0, 1);
            scythe.setVisualScale(2.5f);
            scythe.getPersistentData().putBoolean(TAG_SCYTHE, true);
            playAt(caster, SoundEvents.BREEZE_WHIRL, 1.2f, 0.7f);
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, scythe.getX(), scythe.getY(), scythe.getZ(), 1, 0, 0, 0, 0);
        }

        @Override
        public void release(int heldTicks) {
            Vec3 aim = SpellProjectile.crosshairTarget(caster);
            int count = blades.size();
            for (int i = 0; i < count; i++) {
                SpellProjectile blade = blades.get(i);
                // Fan the volley out horizontally around the aim direction.
                float spread = count <= 1 ? 0f : (i - (count - 1) / 2f) * (count >= 5 ? 10f : 12f);
                Vec3 toAim = aim.subtract(blade.position()).yRot(spread * Mth.DEG_TO_RAD);
                blade.release(blade.position().add(toAim));
            }
            playAt(caster, SoundEvents.BREEZE_SHOOT, 0.9f, 1.2f);
            if (level >= 7) {
                // Slipstream: a burst of speed as the wind leaves your hands.
                caster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1));
            }
        }

        @Override
        public void cancel() {
            for (SpellProjectile blade : blades) {
                if (blade.isAlive()) {
                    impact(blade);
                    blade.discard();
                }
            }
        }
    }
}
