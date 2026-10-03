package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;

/**
 * Fire's level-8 spell, Pyronado (registered as flame_burst, the ring of fire it grew out of, so
 * those who learned that have this now): three wheels of flame orbit the caster for 8 seconds (see
 * Pyronados). Whatever they sweep through is burned and thrown back, and the caster fights on as
 * they spin.
 */
public class PyronadoSpell extends Spell {

    public PyronadoSpell() {
        super(ModSchools.FIRE, 45, 400, 8);
    }

    @Override
    public CastResult cast(CastContext context) {
        Pyronados.start(context.caster(), context.power());
        return CastResult.SUCCESS;
    }
}
