package com.chappadodle.elementalarcana.content.mob;

import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSpells;
import com.chappadodle.elementalarcana.content.spell.ChainLightningSpell;
import com.chappadodle.elementalarcana.content.spell.Smites;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * The derived elements' creature spells (their wisps cast them; creatures don't spawn Attuned to
 * these elements, see AttunementRules#ATTUNABLE): each element's first spell, and its close burst.
 */
public final class DerivedMobSpells {
    public static final List<MobSpell> CRYSTAL = List.of(new PrismBolt(), new CloseBurst(Element.CRYSTAL));
    public static final List<MobSpell> LIGHTNING = List.of(new Zap(), new CloseBurst(Element.LIGHTNING));
    public static final List<MobSpell> RADIANCE = List.of(new Smite(), new CloseBurst(Element.RADIANCE));

    private DerivedMobSpells() {
    }

    private abstract static class Adept implements MobSpell {
        @Override
        public AttunementRank rank() {
            return AttunementRank.ADEPT;
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
