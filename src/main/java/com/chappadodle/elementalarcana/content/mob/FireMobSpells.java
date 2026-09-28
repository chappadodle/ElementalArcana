package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.FireField;
import com.chappadodle.elementalarcana.content.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/** Fire: Adepts throw fireballs, Magi set the ground under you alight, Archmages call down Meteors. */
public final class FireMobSpells {
    public static final List<MobSpell> ALL = List.of(new Fireball(), new CloseBurst(Element.FIRE), new BurningGround(), new Meteor());

    private FireMobSpells() {
    }

    private static final class Fireball implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }

        @Override
        public int cooldownTicks() {
            return 60;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ModSpells.FIREBALL.get().shootForMob(caster, MobCasting.aimPoint(target), MobSpells.POWER, false);
            MobCasting.play(caster, SoundEvents.BLAZE_SHOOT, 1f, 1.1f);
        }
    }

    /** The ground under the target bursts into flames for 5 seconds. */
    private static final class BurningGround implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 160;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 3 && distance <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            FireField.spawn(level, target.position(), 2.5, 100, caster);
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.1, target.getZ(), 30, 1.2, 0.05, 1.2, 0.03);
            MobCasting.play(target, SoundEvents.FIRECHARGE_USE, 1f, 0.8f);
        }
    }

    private static final class Meteor implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 240;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 6 && distance <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ModSpells.FIREBALL.get().shootForMob(caster, target.position(), MobSpells.POWER, true);
            MobCasting.play(caster, SoundEvents.BLAZE_SHOOT, 1.5f, 0.6f);
        }
    }
}
