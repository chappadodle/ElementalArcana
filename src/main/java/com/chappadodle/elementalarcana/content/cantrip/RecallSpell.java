package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.network.chat.Component;

/**
 * Recall (a cantrip): after four seconds standing still, you're taken home (Recalls). Moving or being
 * hurt breaks it, and then its long cooldown is given back.
 */
public class RecallSpell extends Spell {
    public RecallSpell() {
        super(ModSchools.ARCANE, 50, 12000);
    }

    @Override
    public CastResult cast(CastContext context) {
        if (Recalls.isRecalling(context.caster())) {
            return CastResult.fail(Component.translatable("message.elementalarcana.cantrip.recalling"));
        }
        Recalls.begin(context.caster(), this);
        return CastResult.SUCCESS;
    }
}
