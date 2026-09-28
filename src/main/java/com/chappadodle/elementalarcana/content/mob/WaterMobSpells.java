package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.content.Whirlpool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

import java.util.List;

/** Water: Adepts spray a jet, Magi heal the other monsters around them, Archmages open a whirlpool under you. */
public final class WaterMobSpells {
    public static final List<MobSpell> ALL = List.of(new JetBurst(), new HealAllies(), new Maelstrom());

    private static final double HEAL_RADIUS = 8;
    private static final float HEAL_AMOUNT = 4f;
    // Heal only when some other monster nearby is below this share of its health.
    private static final float HEAL_BELOW = 0.7f;
    // A healed creature can't be healed again (by any healer) for this long, so several Magi
    // together still give each monster at most 4 health per 10 seconds.
    private static final int HEAL_LOCK_TICKS = 200;
    private static final String TAG_HEALED_UNTIL = "ea_mob_healed_until";

    private WaterMobSpells() {
    }

    /** Other monsters nearby that are hurt and haven't been healed recently. Never the caster itself. */
    private static List<LivingEntity> alliesToHeal(Mob caster) {
        long now = caster.level().getGameTime();
        return caster.level().getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(HEAL_RADIUS),
                entity -> entity != caster && entity instanceof Enemy && entity.isAlive() && entity.distanceTo(caster) <= HEAL_RADIUS
                        && entity.getHealth() < entity.getMaxHealth() * HEAL_BELOW
                        && now >= entity.getPersistentData().getLong(TAG_HEALED_UNTIL));
    }

    private static final class JetBurst implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }

        @Override
        public int cooldownTicks() {
            return 80;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 10;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            MobJet.start(caster, target, MobSpells.POWER);
        }
    }

    private static final class HealAllies implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return !alliesToHeal(caster).isEmpty();
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            long lockUntil = level.getGameTime() + HEAL_LOCK_TICKS;
            for (LivingEntity ally : alliesToHeal(caster)) {
                ally.heal(HEAL_AMOUNT);
                ally.getPersistentData().putLong(TAG_HEALED_UNTIL, lockUntil);
                level.sendParticles(ParticleTypes.SPLASH, ally.getX(), ally.getY(0.8), ally.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
                level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0) + 0.3, ally.getZ(), 2, 0.3, 0.1, 0.3, 0);
            }
            MobCasting.play(caster, SoundEvents.GENERIC_SPLASH, 1f, 1.4f);
        }
    }

    private static final class Maelstrom implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 300;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 4 && distance <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            Whirlpool.spawn((ServerLevel) caster.level(), target.position(), 1.5f * MobSpells.POWER, 80, caster);
        }
    }
}
