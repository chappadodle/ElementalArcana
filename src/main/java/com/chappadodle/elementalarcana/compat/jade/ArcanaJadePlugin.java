package com.chappadodle.elementalarcana.compat.jade;

import net.minecraft.world.entity.LivingEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * Jade integration (optional: Jade finds this class itself, and nothing else in the mod refers to
 * it, so the mod runs fine without Jade). Players who install Jade see creatures' levels and elements.
 */
@WailaPlugin
public class ArcanaJadePlugin implements IWailaPlugin {

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(CreatureLevelProvider.INSTANCE, LivingEntity.class);
        registration.registerEntityComponent(CreatureElementProvider.INSTANCE, LivingEntity.class);
    }
}
