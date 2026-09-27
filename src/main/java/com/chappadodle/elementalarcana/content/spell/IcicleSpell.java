package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ice's basic spell. Hold the cast key: frost gathers beside you into an icicle that grows
 * sharper for a second. Release it and it flies at your
 * crosshair. A quick tap throws a weak one. A full charge freezes what it hits.
 */
public class IcicleSpell extends Spell implements ProjectileSpell {
    private static final int CHARGE_TICKS = 20;
    private static final int FREEZE_RADIUS = 2;
    private static final ResourceLocation MODEL = ElementalArcana.id("spell/icicle");

    public IcicleSpell() {
        super(ModSchools.ICE, 12, 12);
    }

    @Override
    public CastResult cast(CastContext context) {
        SpellProjectile icicle = SpellProjectile.summonHeld(context, this);
        context.holdUntilRelease(new Hold(context.caster(), icicle));
        playAt(icicle, SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 1.3f);
        return CastResult.SUCCESS;
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
        return CHARGE_TICKS;
    }

    @Override
    public float releaseSpeed(float charge) {
        return 1.4f + 1.4f * charge;
    }

    @Override
    public int lifetimeTicks() {
        return 60;
    }

    // ---- visuals (client) ----

    /** Frost gathers inward from all around while charging; once full, it glints steadily. */
    @Override
    public void heldParticles(SpellProjectile icicle, float charge) {
        if (charge < 1f) {
            int count = 1 + Math.round(charge * 3);
            for (int i = 0; i < count; i++) {
                Vec3 from = randomOnSphere(icicle, 0.8 + icicle.getRandom().nextDouble() * 0.5);
                // Sparkles slow by friction 0.86/tick, so total travel is about 7x this speed: aim to land on the icicle.
                Vec3 inward = icicle.position().subtract(from).scale(0.15);
                icicle.level().addParticle(ModContent.FROST_SPARKLE.get(), from.x, from.y, from.z, inward.x, inward.y, inward.z);
            }
            if (icicle.tickCount % 4 == 0) {
                icicle.spawnParticleAround(ModContent.FROST_MIST.get(), 0.15, new Vec3(0, 0.005, 0));
            }
        } else if (icicle.tickCount % 2 == 0) {
            icicle.spawnParticleAround(ModContent.FROST_SPARKLE.get(), 0.25, new Vec3(0, 0.01, 0));
        }
    }

    /** A shimmering trail of glints, a wisp of cold mist, and the odd snowflake. */
    @Override
    public void flightParticles(SpellProjectile icicle) {
        Vec3 back = icicle.getDeltaMovement().scale(-0.05);
        for (int i = 0; i < 2; i++) {
            icicle.spawnParticleAround(ModContent.FROST_SPARKLE.get(), 0.08, back);
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

    // ---- gameplay (server) ----

    // Projectiles fly straight through water surfaces, so freezing is checked in flight.
    @Override
    public void onTick(SpellProjectile projectile) {
        if (projectile.isInWater()) {
            Freezing.freezeWater((ServerLevel) projectile.level(), projectile.blockPosition(), FREEZE_RADIUS);
            shatter(projectile);
            projectile.discard();
        }
    }

    @Override
    public void onHitEntity(SpellProjectile projectile, EntityHitResult hit) {
        Entity target = hit.getEntity();
        float charge = projectile.charge(0f);
        target.hurt(projectile.damageSources().indirectMagic(projectile, projectile.getOwner()), (3f + 5f * charge) * projectile.power());
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(40 + 40 * charge), 1));
        }
        if (target.canFreeze() && charge >= 1f) {
            freezeOver(target);
        }
        shatter(projectile);
    }

    @Override
    public void onHitBlock(SpellProjectile projectile, BlockHitResult hit) {
        Freezing.freezeWater((ServerLevel) projectile.level(), hit.getBlockPos(), FREEZE_RADIUS);
        shatter(projectile);
    }

    /** Full-charge hit: the target frosts over (vanilla freeze overlay + shivering) with a crust of ice. */
    private static void freezeOver(Entity target) {
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 80));
        ServerLevel level = (ServerLevel) target.level();
        double width = target.getBbWidth() * 0.6;
        double height = target.getBbHeight() * 0.5;
        level.sendParticles(ModContent.FROST_SPARKLE.get(), target.getX(), target.getY(0.5), target.getZ(), 18, width, height, width, 0.02);
        level.sendParticles(ModContent.FROST_MIST.get(), target.getX(), target.getY(0.3), target.getZ(), 5, width * 0.6, height * 0.5, width * 0.6, 0.01);
        level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY(0.5), target.getZ(), 12, width, height, width, 0.01);
    }

    /** Shatter: shards spray and rain down, a puff of cold mist, glints; bigger and deeper at full charge. */
    private static void shatter(SpellProjectile projectile) {
        ServerLevel level = (ServerLevel) projectile.level();
        float charge = projectile.charge(0f);
        double x = projectile.getX();
        double y = projectile.getY();
        double z = projectile.getZ();
        level.sendParticles(ModContent.ICE_SHARD.get(), x, y, z, Math.round(8 + 12 * charge), 0.1, 0.1, 0.1, 0.12 + 0.1 * charge);
        level.sendParticles(ModContent.FROST_MIST.get(), x, y, z, Math.round(2 + 3 * charge), 0.15, 0.15, 0.15, 0.02);
        level.sendParticles(ModContent.FROST_SPARKLE.get(), x, y, z, Math.round(6 + 10 * charge), 0.2, 0.2, 0.2, 0.15);
        level.playSound(null, x, y, z, ModContent.ICICLE_IMPACT.get(), SoundSource.PLAYERS,
                0.7f + 0.4f * charge, 1.15f - 0.25f * charge + (level.getRandom().nextFloat() - 0.5f) * 0.1f);
    }

    private static void playAt(Entity at, SoundEvent sound, float volume, float pitch) {
        at.level().playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    private record Hold(ServerPlayer caster, SpellProjectile icicle) implements SpellHold {
        @Override
        public boolean tick(int heldTicks) {
            if (heldTicks == CHARGE_TICKS && icicle.isAlive()) {
                playAt(icicle, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 1.6f);
                ((ServerLevel) icicle.level()).sendParticles(ModContent.FROST_SPARKLE.get(),
                        icicle.getX(), icicle.getY(), icicle.getZ(), 14, 0.05, 0.05, 0.05, 0.12);
            }
            return icicle.isAlive();
        }

        @Override
        public void release(int heldTicks) {
            icicle.release(SpellProjectile.crosshairTarget(caster));
            playAt(icicle, SoundEvents.TRIDENT_THROW.value(), 0.8f, 1.5f);
        }

        @Override
        public void cancel() {
            if (icicle.isAlive()) {
                shatter(icicle);
                icicle.discard();
            }
        }
    }
}
