package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.api.HollowedRules;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.tower.KeepDistanceGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

/**
 * A Hollow Herald (see the Hollowed spec), a camp's leader, half again as tall as the rest, with a
 * purple bar: it throws three Hunger Bolts at once, every ten seconds or so makes the Pull (everyone
 * within 10 blocks dragged in, then a burst of hunger), and its hunger eats a little mana every
 * second from players near it. Hurt, it blinks away; at half health it calls two Acolytes.
 */
public class HollowHerald extends HollowedEntity {
    private static final int BLINK_COOLDOWN_TICKS = 100;
    private static final int PULL_COOLDOWN_TICKS = 200;

    private final ServerBossEvent bossEvent = new ServerBossEvent(getDisplayName(), BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS);
    @Nullable
    private BlockPos obelisk;
    private long nextBlinkAt;
    private boolean calledAcolytes;

    public HollowHerald(EntityType<? extends HollowHerald> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 130)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.SCALE, 1.4);
    }

    /** The camp's obelisk, which it keeps near when it blinks. */
    public void setObelisk(@Nullable BlockPos obelisk) {
        this.obelisk = obelisk == null ? null : obelisk.immutable();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        registerCommonGoals();
        goalSelector.addGoal(1, new PullGoal());
        goalSelector.addGoal(2, new HungerBoltGoal(this, 50, 3));
        goalSelector.addGoal(3, new KeepDistanceGoal(this, 5, 9));
    }

    @Override
    public IllagerArmPose getArmPose() {
        return isCasting() ? IllagerArmPose.SPELLCASTING : IllagerArmPose.CROSSED;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 20 == 0 && level() instanceof ServerLevel level) {
            // Its hunger: a little mana a second from every player near it.
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(HollowedRules.AURA_RADIUS),
                    player -> player.isAlive() && !player.isCreative() && !player.isSpectator())) {
                eat(player, HollowedRules.AURA_MANA, 0.5f);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && level() instanceof ServerLevel level) {
            if (!calledAcolytes && getHealth() < getMaxHealth() / 2) {
                calledAcolytes = true;
                callAcolytes(level);
            }
            if (level.getGameTime() >= nextBlinkAt) {
                nextBlinkAt = level.getGameTime() + BLINK_COOLDOWN_TICKS;
                blink(level);
            }
        }
        return hurt;
    }

    /** Teleports 4 to 8 blocks away onto open ground at about the same height (near its obelisk, if it has one). */
    private void blink(ServerLevel level) {
        Vec3 from = position();
        for (int tries = 0; tries < 12; tries++) {
            double angle = getRandom().nextDouble() * Math.PI * 2;
            double distance = 4 + getRandom().nextDouble() * 4;
            BlockPos target = BlockPos.containing(getX() + Math.cos(angle) * distance, getY(), getZ() + Math.sin(angle) * distance);
            if (obelisk != null && target.distSqr(obelisk) > 12 * 12) {
                continue;
            }
            if (level.getBlockState(target.below()).isSolid() && level.noCollision(this, getBoundingBox().move(Vec3.atBottomCenterOf(target).subtract(from)))) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(1.0), getZ(), 40, 0.4, 0.8, 0.4, 0.05);
                teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.6f);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(1.0), getZ(), 40, 0.4, 0.8, 0.4, 0.05);
                return;
            }
        }
    }

    private void callAcolytes(ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            BlockPos at = blockPosition().offset(getRandom().nextInt(5) - 2, 0, getRandom().nextInt(5) - 2);
            HollowedEntity acolyte = HollowedCamps.summon(level, ModHollowed.ACOLYTE.get(), at, MobSpawnType.MOB_SUMMONED);
            if (acolyte != null && getTarget() != null) {
                acolyte.setTarget(getTarget());
            }
        }
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.4f, 0.7f);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (obelisk != null) {
            tag.put("obelisk", NbtUtils.writeBlockPos(obelisk));
        }
        tag.putBoolean("called_acolytes", calledAcolytes);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        obelisk = NbtUtils.readBlockPos(tag, "obelisk").orElse(null);
        calledAcolytes = tag.getBoolean("called_acolytes");
        if (hasCustomName()) {
            bossEvent.setName(getDisplayName());
        }
    }

    /**
     * The Pull: with a player within 10 blocks, it raises its arms for a second (the dark streaming
     * in, a rising whine), drags everyone within 10 blocks toward it, and a moment later bursts:
     * hunger's harm to everyone within 4, thrown back.
     */
    private class PullGoal extends Goal {
        private static final int WINDUP_TICKS = 20;
        private static final int BURST_AFTER = 8;
        private long readyAt;
        private int timer;

        PullGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return getTarget() != null && level().getGameTime() >= readyAt && !players(HollowedRules.PULL_RADIUS).isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return timer > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            timer = WINDUP_TICKS + BURST_AFTER;
            getNavigation().stop();
            setCasting(true);
            playSound(SoundEvents.WARDEN_SONIC_CHARGE, 1.5f, 0.8f);
        }

        @Override
        public void tick() {
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            timer--;
            if (timer > BURST_AFTER) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 10, 3, 1, 3, 0.4);
            } else if (timer == BURST_AFTER) {
                for (ServerPlayer player : players(HollowedRules.PULL_RADIUS)) {
                    Vec3 toward = position().subtract(player.position()).normalize();
                    player.setDeltaMovement(player.getDeltaMovement().add(toward.x * 1.4, 0.35, toward.z * 1.4));
                    player.hurtMarked = true;
                }
                playSound(SoundEvents.WARDEN_SONIC_BOOM, 1f, 1.4f);
            } else if (timer == 0) {
                for (ServerPlayer player : players(4)) {
                    SpellDamage.hurtMultiHit(player, ModHollowed.hunger(level, HollowHerald.this, HollowHerald.this), HollowedRules.PULL_DAMAGE);
                    Vec3 away = player.position().subtract(position()).normalize();
                    player.setDeltaMovement(away.x * 1.2, 0.5, away.z * 1.2);
                    player.hurtMarked = true;
                }
                level.sendParticles(ParticleTypes.SQUID_INK, getX(), getY(0.5), getZ(), 60, 1.5, 0.8, 1.5, 0.2);
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY(0.5), getZ(), 80, 2, 1, 2, 0.6);
            }
        }

        @Override
        public void stop() {
            setCasting(false);
            timer = 0;
            readyAt = level().getGameTime() + PULL_COOLDOWN_TICKS;
        }

        private List<ServerPlayer> players(double radius) {
            return level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(radius),
                    player -> player.isAlive() && !player.isCreative() && !player.isSpectator() && distanceToSqr(player) <= radius * radius);
        }
    }
}
