package com.chappadodle.elementalarcana.api;

import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One projectile entity for every {@link ProjectileSpell}: it stores which spell fired it
 * (synced, so clients can draw that spell's trail) and hands hits back to the spell.
 */
public class SpellProjectile extends ThrowableProjectile {
    private static final EntityDataAccessor<String> SPELL_ID = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.STRING);

    private float power = 1f;

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    public static <S extends Spell & ProjectileSpell> SpellProjectile launch(CastContext context, S spell) {
        ServerPlayer caster = context.caster();
        SpellProjectile projectile = new SpellProjectile(ModContent.SPELL_PROJECTILE.get(), context.level());
        projectile.setOwner(caster);
        Vec3 eye = context.eyePosition();
        projectile.setPos(eye.x, eye.y - 0.1, eye.z);
        projectile.entityData.set(SPELL_ID, spell.id().toString());
        projectile.power = context.power();
        projectile.shootFromRotation(caster, caster.getXRot(), caster.getYRot(), 0f, spell.projectileSpeed(), 0f);
        context.level().addFreshEntity(projectile);
        return projectile;
    }

    public float power() {
        return power;
    }

    @Nullable
    public ProjectileSpell spell() {
        ResourceLocation id = ResourceLocation.tryParse(entityData.get(SPELL_ID));
        return id != null && SpellRegistries.SPELLS.get(id) instanceof ProjectileSpell spell ? spell : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SPELL_ID, "");
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        ProjectileSpell spell = spell();
        if (level().isClientSide()) {
            if (spell != null) {
                for (int i = 0; i < 3; i++) {
                    level().addParticle(spell.trailParticle(),
                            getX() + (random.nextDouble() - 0.5) * 0.2,
                            getY() + (random.nextDouble() - 0.5) * 0.2,
                            getZ() + (random.nextDouble() - 0.5) * 0.2,
                            0, 0, 0);
                }
            }
        } else if (spell == null || tickCount > spell.lifetimeTicks()) {
            discard();
        } else if (!isRemoved()) {
            spell.onTick(this);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        ProjectileSpell spell = spell();
        if (spell != null && !level().isClientSide()) {
            spell.onHitEntity(this, result);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        ProjectileSpell spell = spell();
        if (spell != null && !level().isClientSide()) {
            spell.onHitBlock(this, result);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide()) {
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Spell", entityData.get(SPELL_ID));
        tag.putFloat("Power", power);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SPELL_ID, tag.getString("Spell"));
        power = tag.contains("Power") ? tag.getFloat("Power") : 1f;
    }
}
