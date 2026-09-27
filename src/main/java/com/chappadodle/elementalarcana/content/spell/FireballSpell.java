package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class FireballSpell extends Spell implements ProjectileSpell {

    public FireballSpell() {
        super(ModSchools.FIRE, 20, 20);
    }

    @Override
    public CastResult cast(CastContext context) {
        SpellProjectile.launch(context, this);
        return CastResult.SUCCESS;
    }

    @Override
    public ParticleOptions trailParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    public float projectileSpeed() {
        return 1.4f;
    }

    @Override
    public void onHitEntity(SpellProjectile projectile, EntityHitResult hit) {
        Entity target = hit.getEntity();
        target.hurt(projectile.damageSources().indirectMagic(projectile, projectile.getOwner()), 6f * projectile.power());
        target.igniteForTicks(100);
        burst(projectile);
    }

    @Override
    public void onHitBlock(SpellProjectile projectile, BlockHitResult hit) {
        ServerLevel level = (ServerLevel) projectile.level();
        BlockPos firePos = hit.getBlockPos().relative(hit.getDirection());
        if (level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)
                && level.isEmptyBlock(firePos)
                && BaseFireBlock.canBePlacedAt(level, firePos, hit.getDirection())) {
            level.setBlockAndUpdate(firePos, BaseFireBlock.getState(level, firePos));
        }
        burst(projectile);
    }

    private static void burst(SpellProjectile projectile) {
        ServerLevel level = (ServerLevel) projectile.level();
        level.sendParticles(ParticleTypes.FLAME, projectile.getX(), projectile.getY(), projectile.getZ(), 25, 0.3, 0.3, 0.3, 0.06);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, projectile.getX(), projectile.getY(), projectile.getZ(), 8, 0.2, 0.2, 0.2, 0.02);
        level.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(), SoundEvents.GENERIC_BURN, SoundSource.PLAYERS, 0.8f, 1.2f);
    }
}
