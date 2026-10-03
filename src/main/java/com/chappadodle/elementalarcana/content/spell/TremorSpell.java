package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellTargets;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * Earth's ground spell: a shockwave runs along the ground for 10 blocks, a block a tick, throwing
 * creatures up and slowing them for 3 seconds, with a small hit of damage (see Tremors). It
 * Crystallizes anything burning, wet or frozen. Attuned Earth Magi cast it too.
 */
public class TremorSpell extends Spell {
    public TremorSpell() {
        super(ModSchools.EARTH, 30, 120);
    }

    @Override
    public CastResult cast(CastContext context) {
        LivingEntity caster = context.caster();
        if (!Tremors.start(caster, caster.getLookAngle(), context.power(), target -> SpellTargets.canAffect(caster, target))) {
            return CastResult.fail(Component.translatable("message.elementalarcana.tremor.no_ground"));
        }
        return CastResult.SUCCESS;
    }
}
