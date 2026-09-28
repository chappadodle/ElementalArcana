package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ElementalReactions;
import com.chappadodle.elementalarcana.api.SpellDamage;
import com.chappadodle.elementalarcana.content.ModSpells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Wind: Adepts slash wind blades, Magi dash away or in, Archmages throw you into the air. */
public final class WindMobSpells {
    public static final List<MobSpell> ALL = List.of(new Blade(), new CloseBurst(Element.WIND), new GaleDash(), new Updraft());

    private WindMobSpells() {
    }

    private static final class Blade implements MobSpell {
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
            MobCasting.shoot(caster, ModSpells.WIND_BLADE.get(), target);
            MobCasting.play(caster, SoundEvents.BREEZE_SHOOT, 1f, 1.1f);
        }
    }

    /** Too close: leap back out of reach. Too far: rush in. */
    private static final class GaleDash implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 100;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return caster.onGround() && (distance < 3 || distance > 8 && distance < 16);
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            Vec3 toward = target.position().subtract(caster.position()).multiply(1, 0, 1).normalize();
            Vec3 direction = MobCasting.distance(caster, target) < 3 ? toward.scale(-1) : toward;
            caster.setDeltaMovement(direction.scale(1.2).add(0, 0.35, 0));
            caster.hurtMarked = true;
            caster.resetFallDistance();
            ServerLevel level = (ServerLevel) caster.level();
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, caster.getX(), caster.getY(), caster.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.CLOUD, caster.getX(), caster.getY() + 0.2, caster.getZ(), 12, 0.3, 0.1, 0.3, 0.05);
            MobCasting.play(caster, SoundEvents.WIND_CHARGE_BURST.value(), 1f, 1f);
        }
    }

    private static final class Updraft implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 12 && target.onGround();
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ServerLevel level = (ServerLevel) caster.level();
            SpellDamage.hurtMultiHit(target, SpellDamage.source(level, Element.WIND, caster, caster), 2f * MobSpells.POWER);
            ElementalReactions.launchAirborne(target, 1.0);
            level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, target.getX(), target.getY(), target.getZ(), 1, 0, 0, 0, 0);
            for (int height = 0; height < 5; height++) {
                level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + height * 0.7, target.getZ(), 5, 0.3, 0.2, 0.3, 0.02);
            }
            MobCasting.play(target, SoundEvents.WIND_CHARGE_BURST.value(), 1f, 0.7f);
        }
    }
}
