package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;

/**
 * Radiance's second spell: a circle of light 4 blocks across at the caster's feet, for 8 seconds
 * (see Sanctuaries). Each second it mends the caster and their allies inside, and sears the undead.
 * Radiance wisps of Magus rank raise one when they're hurt.
 */
public class SanctuarySpell extends Spell {

    public SanctuarySpell() {
        super(ModSchools.RADIANCE, 40, 600, 10);
    }

    @Override
    public CastResult cast(CastContext context) {
        Sanctuaries.place(context.level(), context.caster().position(), context.caster(), context.power(),
                target -> SpellTargets.canAffect(context.caster(), target));
        return CastResult.SUCCESS;
    }
}
