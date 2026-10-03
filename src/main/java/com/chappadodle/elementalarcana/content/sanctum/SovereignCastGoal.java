package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.SovereignRules;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.content.mob.MobSpell;

import java.util.List;

/**
 * A Sovereign's spellcasting: its element's creature spells (quickened) and its own signature
 * spells, the second of them only from half health, with a shorter wind-up and gap than any
 * creature's.
 */
public class SovereignCastGoal extends CastMobSpellGoal {
    private final SovereignEntity sovereign;

    public SovereignCastGoal(SovereignEntity sovereign) {
        super(sovereign);
        this.sovereign = sovereign;
    }

    @Override
    protected List<MobSpell> spells(CreatureMagic magic) {
        return SovereignSpells.of(sovereign.element(), sovereign.isEnraged());
    }

    @Override
    protected int windupTicks() {
        return SovereignRules.WINDUP_TICKS;
    }

    @Override
    protected int minGapTicks() {
        return SovereignRules.MIN_GAP_TICKS;
    }

    @Override
    protected int cooldownOf(MobSpell spell) {
        return SovereignSpells.isSignature(spell) ? spell.cooldownTicks() : SovereignRules.cooldown(spell.cooldownTicks());
    }
}
