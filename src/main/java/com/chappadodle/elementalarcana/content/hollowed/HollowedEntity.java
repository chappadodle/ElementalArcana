package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.api.HollowedRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * One of the Hollowed (see their spec): someone the Hollow has hollowed out. What all three kinds
 * share: they stand together and hunt players; their attacks eat mana ({@link #eat}), and what they
 * eat heals them; their voices are low. They are no raiders: a raid has no use for them.
 */
public abstract class HollowedEntity extends AbstractIllager {
    private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(HollowedEntity.class, EntityDataSerializers.BOOLEAN);

    protected HollowedEntity(EntityType<? extends HollowedEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CASTING, false);
    }

    /** Whether it's winding up a spell (its arms raised). */
    public boolean isCasting() {
        return entityData.get(CASTING);
    }

    public void setCasting(boolean casting) {
        entityData.set(CASTING, casting);
    }

    /** The goals all three share: swim, wander, look about; hunt players, and whoever hurts one of them, with the rest. */
    protected void registerCommonGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, HollowedEntity.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /**
     * Eats up to {@code wanted} mana from {@code target}, if it's a player (half as much from one who
     * carries a Hungerward), and heals by {@code heal} if it ate anything. Someone with too little
     * mana left to feed it is bitten deeper instead ({@link HollowedRules#STARVING_DAMAGE} more).
     */
    public void eat(LivingEntity target, float wanted, float heal) {
        if (!(target instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()
                || !(level() instanceof ServerLevel level)) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        boolean warded = Hungerward.warded(player);
        float eaten = HollowedRules.manaEaten(data.mana(), wanted, warded);
        boolean starving = HollowedRules.starving(data.mana(), wanted * (warded ? HollowedRules.WARD_MANA_FACTOR : 1f));
        if (eaten > 0) {
            data.setMana(data.mana() - eaten);
            MagicAttachments.sync(player);
            heal(heal);
            // A thread of it, drawn out of them.
            Vec3 from = player.position().add(0, player.getBbHeight() * 0.6, 0);
            Vec3 to = position().add(0, getBbHeight() * 0.6, 0);
            for (int i = 0; i < 8; i++) {
                Vec3 at = from.lerp(to, i / 8.0);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1f, 0.6f);
        }
        if (starving) {
            SpellDamage.hurtMultiHit(player, ModHollowed.hunger(level, this, this), HollowedRules.STARVING_DAMAGE);
        }
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof HollowedEntity || super.isAlliedTo(other);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.75f;
    }

    @Override
    public void applyRaidBuffs(ServerLevel level, int wave, boolean unused) {
    }

    @Override
    public boolean canJoinRaid() {
        return false;
    }

    /** Never a patrol's banner-bearer: theirs is no illager band. */
    @Override
    public boolean canBeLeader() {
        return false;
    }

    @Override
    public SoundEvent getCelebrateSound() {
        return SoundEvents.EVOKER_CELEBRATE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }
}
