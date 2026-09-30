package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Keeps MagicAttachments#FROZEN_UNTIL in step with the Frozen effect, whatever applied it (Deep
 * Freeze, the Freeze reaction, Frost Shield...), so every player can see who's encased in ice.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class FrozenState {
    private FrozenState() {
    }

    private static boolean isFrozen(@Nullable MobEffectInstance effect) {
        return effect != null && effect.is(ModContent.FROZEN);
    }

    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && isFrozen(event.getEffectInstance())) {
            entity.setData(MagicAttachments.FROZEN_UNTIL, entity.level().getGameTime() + event.getEffectInstance().getDuration());
        }
    }

    @SubscribeEvent
    public static void onRemoved(MobEffectEvent.Remove event) {
        if (!event.getEntity().level().isClientSide() && event.getEffect().is(ModContent.FROZEN)) {
            event.getEntity().removeData(MagicAttachments.FROZEN_UNTIL);
        }
    }

    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        if (!event.getEntity().level().isClientSide() && isFrozen(event.getEffectInstance())) {
            event.getEntity().removeData(MagicAttachments.FROZEN_UNTIL);
        }
    }
}
