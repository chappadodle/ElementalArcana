package com.chappadodle.elementalarcana.content.spell;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModSchools;
import net.minecraft.network.chat.Component;

/**
 * Water's level-6 spell, Tsunami (registered as tidal_wave, the cone of water it grew out of, so
 * those who learned that have this now): a wall of water rises in front of the caster and rolls
 * away the way they face, 16 blocks in 1.6 seconds, sweeping up what it meets and putting out fires
 * (see Tsunamis). It needs a little open ground to rise on.
 */
public class TsunamiSpell extends Spell {

    public TsunamiSpell() {
        super(ModSchools.WATER, 40, 240, 6);
    }

    @Override
    public CastResult cast(CastContext context) {
        if (!Tsunamis.start(context.caster(), context.power())) {
            return CastResult.fail(Component.translatable("message.elementalarcana.tsunami.no_room"));
        }
        context.caster().clearFire();
        return CastResult.SUCCESS;
    }
}
