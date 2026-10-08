package com.chappadodle.elementalarcana.content.hollow;

import com.chappadodle.elementalarcana.api.HollowedRules;
import com.chappadodle.elementalarcana.content.hollowed.Hungerward;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.HollowRules;
import com.chappadodle.elementalarcana.content.sanctum.SovereignEntity;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The Hollow (see the Hollow spec): the hunger made a shape, a Sovereign's form gone black at three
 * times a man's height. Level 70. It fights in four forms, a quarter of its health each (Fire, Water,
 * Wind, Earth): Attuned to each in turn (so the element chart decides what hurts it), casting that
 * element's spells and both of its Sovereign's. Every change of form is a Devouring: it drags
 * everyone near toward it, then bursts. While it lives, everyone near it loses mana every second,
 * or health once they have none. When it falls, the Hollow is bound (HollowEvents).
 */
public class HollowEntity extends SovereignEntity {
    private static final EntityDataAccessor<Byte> FORM = SynchedEntityData.defineId(HollowEntity.class, EntityDataSerializers.BYTE);
    private static final int DEVOUR_PULL_TICKS = 12;

    public HollowEntity(EntityType<? extends HollowEntity> type, Level level) {
        super(type, level, Element.FIRE);
        this.xpReward = 1000;
    }

    /** 670 before Vitality: about 1020 at level 70, just under the game's cap on health (1024). */
    public static AttributeSupplier.Builder createAttributes() {
        return SovereignEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 520)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.FOLLOW_RANGE, 48);
    }

    /** Its current form. */
    @Override
    public Element element() {
        return HollowRules.FORMS.get(Mth.clamp(entityData.get(FORM), 0, HollowRules.FORMS.size() - 1));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, (byte) 0);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData);
        // Every form knows both of its Sovereign's spells.
        setEnraged(true);
        return result;
    }

    @Override
    protected int baseLevel(int zoneLevel) {
        return HollowRules.BASE_LEVEL;
    }

    @Override
    protected double leash() {
        return HollowRules.LEASH;
    }

    @Override
    protected boolean leashed() {
        return true;
    }

    /** It leaves no heart: binding it is the reward (HollowEvents). */
    @Override
    protected ItemStack trophy() {
        return ItemStack.EMPTY;
    }

    @Override
    public Component bossTitle() {
        return Component.translatable("bossbar.elementalarcana.hollow");
    }

    @Override
    public BossEvent.BossBarColor barColor() {
        return BossEvent.BossBarColor.PURPLE;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && level() instanceof ServerLevel level) {
            Element form = HollowRules.formAt(getHealth(), getMaxHealth());
            if (form != element()) {
                shift(level, form);
            }
        }
        return hurt;
    }

    /** It takes its next form: Attuned to it (its health kept), it devours, and says so. */
    private void shift(ServerLevel level, Element form) {
        entityData.set(FORM, (byte) HollowRules.FORMS.indexOf(form));
        setData(MagicAttachments.CREATURE_MAGIC, new CreatureMagic(form, AttunementRank.ARCHMAGE));
        devour(level);
        Component message = Component.translatable("message.elementalarcana.hollow.form." + form.name().toLowerCase(Locale.ROOT))
                .withColor(form.color());
        level.players().forEach(player -> player.sendSystemMessage(message));
    }

    /** It drags everyone within 16 blocks toward it; a moment later it bursts, hurting and throwing them back. */
    private void devour(ServerLevel level) {
        List<LivingEntity> caught = foesWithin(HollowRules.DEVOUR_RADIUS);
        for (LivingEntity foe : caught) {
            Vec3 toward = position().subtract(foe.position());
            toward = toward.lengthSqr() < 1.0e-4 ? Vec3.ZERO : toward.normalize();
            foe.setDeltaMovement(foe.getDeltaMovement().add(toward.scale(1.1)).add(0, 0.2, 0));
            foe.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 200, 4, 3, 4, 0.4);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3f, 0.6f);
        later(DEVOUR_PULL_TICKS, () -> {
            for (LivingEntity foe : caught) {
                if (!foe.isAlive()) {
                    continue;
                }
                foe.hurt(damageSources().indirectMagic(this, this), HollowRules.DEVOUR_DAMAGE);
                Vec3 away = foe.position().subtract(position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0e-4 ? new Vec3(1, 0, 0) : away.normalize();
                foe.setDeltaMovement(foe.getDeltaMovement().add(away.scale(1.4)).add(0, 0.4, 0));
                foe.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.SONIC_BOOM, getX(), getY(0.5), getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(0.5), getZ(), 120, 3, 2, 3, 0.3);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, 0.7f);
        });
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 20 == 0 && level() instanceof ServerLevel level) {
            hunger(level);
        }
    }

    /** Its hunger: everyone within 24 blocks loses mana, or health once they have none, drawn off toward it. */
    private void hunger(ServerLevel level) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(HollowRules.HUNGER_RADIUS),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
            MagicData data = MagicAttachments.get(player);
            boolean starving = HollowRules.starving(data.mana());
            // A Hungerward halves what it eats.
            data.setMana(data.mana() - HollowRules.manaEaten(data.mana()) * (Hungerward.warded(player) ? HollowedRules.WARD_MANA_FACTOR : 1f));
            MagicAttachments.sync(player);
            if (starving) {
                player.hurt(damageSources().source(ModHollow.HUNGER, this), HollowRules.STARVING_DAMAGE);
            }
            // A thread of it, drawn out of them.
            Vec3 from = player.position().add(0, player.getBbHeight() * 0.6, 0);
            Vec3 to = getBoundingBox().getCenter();
            for (int i = 0; i < 8; i++) {
                Vec3 at = from.lerp(to, i / 8.0);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
            }
        }
    }

    /** No one left to fight: it sinks back to its first form and heals. */
    @Override
    protected void calm() {
        super.calm();
        entityData.set(FORM, (byte) 0);
        setData(MagicAttachments.CREATURE_MAGIC, new CreatureMagic(HollowRules.FORMS.getFirst(), AttunementRank.ARCHMAGE));
        setEnraged(true);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            HollowEvents.bound(level);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WARDEN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WARDEN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("form", entityData.get(FORM));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FORM, tag.getByte("form"));
    }
}
