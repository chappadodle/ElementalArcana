package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;

/**
 * Lightning's third spell, Stormcall (docs/superpowers/specs/2026-10-05-stormcall-design.md): a
 * storm cloud gathers over the caster and follows them for 10 seconds, soaking what stands under it
 * and striking the foes around them (see Stormcalls). The caster fights on while it rages.
 */
public class StormcallSpell extends Spell {

    public StormcallSpell() {
        super(ModSchools.LIGHTNING, 60, 600, 15);
    }

    @Override
    public CastResult cast(CastContext context) {
        Stormcalls.start(context.caster(), context.power());
        return CastResult.SUCCESS;
    }
}
