package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;

/**
 * Radiance's third spell, Dawnbreak (docs/superpowers/specs/2026-10-05-dawnbreak-design.md): a small
 * sun rises over the place the caster stands and shines there for 12 seconds, burning the undead,
 * scorching and showing up every other foe, and clearing darkness from the caster and their allies
 * (see Dawnbreaks).
 */
public class DawnbreakSpell extends Spell {

    public DawnbreakSpell() {
        super(ModSchools.RADIANCE, 70, 700, 15);
    }

    @Override
    public CastResult cast(CastContext context) {
        Dawnbreaks.start(context.level(), context.caster(), context.power());
        return CastResult.SUCCESS;
    }
}
