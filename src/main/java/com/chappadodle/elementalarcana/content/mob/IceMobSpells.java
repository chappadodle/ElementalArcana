package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.spell.FrostNovaSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/** Ice: Adepts throw icicles, Magi burst a Frost Nova when you're close, Archmages ward themselves in ice. */
public final class IceMobSpells {
    public static final List<MobSpell> ALL = List.of(new Icicle(), new CloseBurst(Element.ICE), new Nova(), new Ward());

    private IceMobSpells() {
    }

    private static final class Icicle implements MobSpell {
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
            MobCasting.shoot(caster, ModSpells.ICICLE.get(), target);
        }
    }

    private static final class Nova implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 120;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 4;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            FrostNovaSpell.burst((ServerLevel) caster.level(), caster, MobSpells.POWER, entity -> SpellTargets.canAffect(caster, entity));
        }
    }

    private static final class Ward implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ARCHMAGE;
        }

        @Override
        public int cooldownTicks() {
            return 400;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return !IceWards.has(caster) && MobCasting.distance(caster, target) <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            IceWards.raise(caster);
        }
    }
}
