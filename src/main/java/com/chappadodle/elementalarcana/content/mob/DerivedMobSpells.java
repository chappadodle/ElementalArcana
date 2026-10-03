package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.spell.ChainLightningSpell;
import com.chappadodle.elementalarcana.content.spell.PrismWards;
import com.chappadodle.elementalarcana.content.spell.Sanctuaries;
import com.chappadodle.elementalarcana.content.spell.Smites;
import com.chappadodle.elementalarcana.content.spell.ThunderclapSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * The derived elements' creature spells (their wisps cast them; creatures don't spawn Attuned to
 * these elements, see AttunementRules#ATTUNABLE): each element's first spell and its close burst,
 * and from Magus rank its second (a Prism Ward, a Thunderclap, a Sanctuary).
 */
public final class DerivedMobSpells {
    public static final List<MobSpell> CRYSTAL = List.of(new PrismBolt(), new CloseBurst(Element.CRYSTAL), new Ward());
    public static final List<MobSpell> LIGHTNING = List.of(new Zap(), new CloseBurst(Element.LIGHTNING), new Clap());
    public static final List<MobSpell> RADIANCE = List.of(new Smite(), new CloseBurst(Element.RADIANCE), new Mend());

    private DerivedMobSpells() {
    }

    private abstract static class Adept implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
        }
    }

    private abstract static class Magus implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.MAGUS;
        }
    }

    /** A Prism Ward, once its foe keeps its distance (and so likely shoots). */
    private static final class Ward extends Magus {
        @Override
        public int cooldownTicks() {
            return 300;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) >= 6 && !PrismWards.has(caster);
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            PrismWards.raise(caster, 100);
        }
    }

    /** A Thunderclap, when its foe comes close. */
    private static final class Clap extends Magus {
        @Override
        public int cooldownTicks() {
            return 200;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 4;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ThunderclapSpell.clap((ServerLevel) caster.level(), caster, MobSpells.POWER, other -> SpellTargets.canAffect(caster, other));
        }
    }

    /** A Sanctuary at its own feet, once it's been hurt: it mends it and its allies. */
    private static final class Mend extends Magus {
        @Override
        public int cooldownTicks() {
            return 400;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return caster.getHealth() < caster.getMaxHealth() * 0.7f;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            Sanctuaries.place((ServerLevel) caster.level(), caster.position(), caster, MobSpells.POWER, other -> SpellTargets.canAffect(caster, other));
        }
    }

    private static final class PrismBolt extends Adept {
        @Override
        public int cooldownTicks() {
            return 50;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 24;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            MobCasting.shoot(caster, ModSpells.PRISM_BOLT.get(), target);
        }
    }

    /** Chain Lightning from the caster to its target, jumping on to whoever else it may hurt. */
    private static final class Zap extends Adept {
        @Override
        public int cooldownTicks() {
            return 70;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            return MobCasting.distance(caster, target) <= 16;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            ChainLightningSpell.chain((ServerLevel) caster.level(), caster, MobCasting.castOrigin(caster, MobCasting.aimPoint(target)), 0,
                    target, MobSpells.POWER, other -> SpellTargets.canAffect(caster, other));
        }
    }

    /** A pillar of light where the target stands: they have three quarters of a second to move. */
    private static final class Smite extends Adept {
        @Override
        public int cooldownTicks() {
            return 90;
        }

        @Override
        public boolean canCast(Mob caster, LivingEntity target) {
            double distance = MobCasting.distance(caster, target);
            return distance >= 3 && distance <= 20;
        }

        @Override
        public void cast(Mob caster, LivingEntity target) {
            Smites.strike((ServerLevel) caster.level(), target.position(), caster, MobSpells.POWER,
                    other -> SpellTargets.canAffect(caster, other));
        }
    }
}
