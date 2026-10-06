package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.WonderRules;
import com.chappadodle.elementalarcana.content.wonder.SkyrayEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A skyray's wake (see the Wonders of the Wild spec): the local player, gliding (with elytra, or on
 * the mod's own glide, steered just before this in Gliding) within reach of a skyray, is borne
 * gently up. A player's movement is their own client's, so it's done here.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class SkyrayWake {
    private SkyrayWake() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !(player.isFallFlying() || Gliding.isGliding(player))
                || player.level().getEntitiesOfClass(SkyrayEntity.class, player.getBoundingBox().inflate(WonderRules.WAKE_REACH)).isEmpty()) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, WonderRules.wakeRise(motion.y), motion.z);
    }
}
