package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;

/**
 * Crystal's second spell: crystal facets circle the caster for 6 seconds (longer with more power),
 * and every projectile that would hit them (arrows, fireballs, spells) is turned back at whoever shot
 * it (see PrismWards). Crystal wisps of Magus rank raise it too.
 */
public class PrismWardSpell extends Spell {
    private static final int DURATION_TICKS = 120;

    public PrismWardSpell() {
        super(ModSchools.CRYSTAL, 35, 400, 10);
    }

    @Override
    public CastResult cast(CastContext context) {
        PrismWards.raise(context.caster(), Math.round(DURATION_TICKS * context.power()));
        return CastResult.SUCCESS;
    }
}
