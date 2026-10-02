package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.spell.TremorSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Earth: Adepts throw boulders, Magi send a Tremor along the ground, Archmages ward themselves in stone. */
public final class EarthMobSpells {
    public static final List<MobSpell> ALL = List.of(new Boulder(), new CloseBurst(Element.EARTH), new Tremor(), new Ward());

    private EarthMobSpells() {
    }

    private static final class Boulder implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }

        @Override
        public int cooldownTicks() {
            return 70;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 22;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ModSpells.BOULDER.get().shootForMob(caster, MobCasting.aimPoint(target), MobSpells.POWER);
        }
    }

    private static final class Tremor implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }

        @Override
        public int cooldownTicks() {
            return 140;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 3 && distance <= 10;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            Vec3 toward = target.position().subtract(caster.position());
            TremorSpell.shockwave((ServerLevel) caster.level(), caster, new Vec3(toward.x, 0, toward.z), MobSpells.POWER,
                    entity -> SpellTargets.canAffect(caster, entity));
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
            return !StoneWards.has(caster) && MobCasting.distance(caster, target) <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            StoneWards.raise(caster);
        }
    }
}
