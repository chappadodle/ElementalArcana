package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.Glow;
import com.chappadodle.elementalarcana.api.ProjectileSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.api.SpellProjectile;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Crystal's first spell: a fast bolt of violet crystal. Whatever it strikes, it bursts into three
 * shards that fly on, fanned out (off a wall they fly back out from it), each hitting for a little
 * under half as much. Like Earth, a crystal hit Crystallizes anything burning, wet or frozen.
 * Crystal wisps throw it too.
 */
public class PrismBoltSpell extends Spell implements ProjectileSpell {
    /** A shard of a burst bolt (SpellProjectile#variant). */
    public static final int SHARD = 1;
    private static final float DAMAGE = 6f;
    private static final float SHARD_DAMAGE = 2.5f;
    private static final float SPREAD = (float) Math.toRadians(25);
    private static final DustParticleOptions TRAIL = new DustParticleOptions(new Vector3f(0.82f, 0.55f, 1f), 0.9f);

    public PrismBoltSpell() {
        super(ModSchools.CRYSTAL, 22, 40);
    }

    @Override
    public CastResult cast(CastContext context) {
        SpellProjectile.launch(context, this);
        return CastResult.SUCCESS;
    }

    @Override
    public ParticleOptions trailParticle() {
        return TRAIL;
    }

    @Override
    public float projectileSpeed() {
        return 2.0f;
    }

    @Override
    public int lifetimeTicks() {
        return 40;
    }

    @Override
    public Glow glow(int variant) {
        return new Glow(0xD08CFF, variant == SHARD ? 0.45f : 0.9f, 0.7f);
    }

    @Override
    public int luminance(SpellProjectile projectile) {
        return projectile.variant() == SHARD ? 4 : 9;
    }

    @Override
    public void onHitEntity(SpellProjectile projectile, EntityHitResult hit) {
        if (!(projectile.level() instanceof ServerLevel level) || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Entity owner = projectile.getOwner();
        boolean shard = projectile.variant() == SHARD;
        Vec3 at = projectile.impactPoint(hit);
        SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.CRYSTAL, projectile, owner),
                (shard ? SHARD_DAMAGE : DAMAGE) * projectile.power());
        if (!shard) {
            ElementalReactions.earthHit(target, owner, 200);
            burst(level, projectile, at, projectile.getDeltaMovement(), target);
        }
        shatter(level, at, shard);
    }

    @Override
    public void onHitBlock(SpellProjectile projectile, BlockHitResult hit) {
        if (!(projectile.level() instanceof ServerLevel level)) {
            return;
        }
        boolean shard = projectile.variant() == SHARD;
        if (!shard) {
            // Off a wall the shards fly back out: the bolt's direction, mirrored in the face it struck.
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 direction = projectile.getDeltaMovement();
            Vec3 reflected = direction.subtract(normal.scale(2 * direction.dot(normal)));
            burst(level, projectile, hit.getLocation().add(normal.scale(0.2)), reflected, null);
        }
        shatter(level, hit.getLocation(), shard);
    }

    /** Three shards from {@code at}, fanned around {@code direction}, sparing what the bolt just struck. */
    private void burst(ServerLevel level, SpellProjectile bolt, Vec3 at, Vec3 direction, @Nullable Entity spare) {
        Entity owner = bolt.getOwner();
        if (owner == null || direction.lengthSqr() < 1.0e-6) {
            return;
        }
        Vec3 forward = direction.normalize();
        for (int i = -1; i <= 1; i++) {
            SpellProjectile shard = SpellProjectile.shootFrom(owner, this, at, forward.yRot(SPREAD * i).scale(1.3), bolt.power());
            shard.setVariant(SHARD);
            shard.setVisualScale(0.6f);
            if (spare != null) {
                shard.ignoreEntity(spare);
            }
        }
    }

    private static void shatter(ServerLevel level, Vec3 at, boolean shard) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()),
                at.x, at.y, at.z, shard ? 6 : 16, 0.15, 0.15, 0.15, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, shard ? 1 : 5, 0.1, 0.1, 0.1, 0.06);
        level.playSound(null, at.x, at.y, at.z, shard ? SoundEvents.AMETHYST_BLOCK_HIT : SoundEvents.AMETHYST_CLUSTER_BREAK,
                SoundSource.PLAYERS, shard ? 0.5f : 1f, shard ? 1.6f : 1.1f);
    }
}
