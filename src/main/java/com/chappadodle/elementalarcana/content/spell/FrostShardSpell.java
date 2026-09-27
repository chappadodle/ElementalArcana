package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class FrostShardSpell extends Spell implements ProjectileSpell {
    private static final int FREEZE_RADIUS = 2;

    public FrostShardSpell() {
        super(ModSchools.ICE, 15, 15);
    }

    @Override
    public CastResult cast(CastContext context) {
        SpellProjectile.launch(context, this);
        return CastResult.SUCCESS;
    }

    @Override
    public ParticleOptions trailParticle() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    public float projectileSpeed() {
        return 2.2f;
    }

    @Override
    public int lifetimeTicks() {
        return 40;
    }

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
        target.hurt(projectile.damageSources().indirectMagic(projectile, projectile.getOwner()), 4f * projectile.power());
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        }
        if (target.canFreeze()) {
            // Past getTicksRequiredToFreeze the vanilla frost overlay and shivering kick in.
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + 100));
        }
        shatter(projectile);
    }

    @Override
    public void onHitBlock(SpellProjectile projectile, BlockHitResult hit) {
        Freezing.freezeWater((ServerLevel) projectile.level(), hit.getBlockPos(), FREEZE_RADIUS);
        shatter(projectile);
    }

    private static void shatter(SpellProjectile projectile) {
        ServerLevel level = (ServerLevel) projectile.level();
        level.sendParticles(ParticleTypes.SNOWFLAKE, projectile.getX(), projectile.getY(), projectile.getZ(), 20, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.ITEM_SNOWBALL, projectile.getX(), projectile.getY(), projectile.getZ(), 10, 0.2, 0.2, 0.2, 0.1);
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.6f, 1.6f);
    }
}
