package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Relic;
import com.chappadodle.elementalarcana.api.RelicRules;
import com.chappadodle.elementalarcana.content.relic.Relics;
import com.chappadodle.elementalarcana.network.RelicJumpPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Feather of the Gale's mid-air jump (see the Relics spec). A player's movement is their own
 * client's, so the jump happens here: pressing jump again while in the air (not flying, swimming,
 * riding or on elytra) once per jump. The server is told, for the gust everyone sees and hears.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class RelicJump {
    private static boolean wasJumping;
    private static boolean used;

    private RelicJump() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) {
            return;
        }
        boolean jumping = player.input.jumping;
        if (player.onGround() || player.isInWater() || player.onClimbable()) {
            used = false;
        }
        if (jumping && !wasJumping && !used && !player.onGround() && !player.getAbilities().flying && !player.isPassenger()
                && !player.isFallFlying() && !player.isInWater() && !player.isInLava() && !player.onClimbable()
                && Relics.bears(player, Relic.FEATHER_OF_THE_GALE)) {
            used = true;
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, RelicRules.FEATHER_JUMP, motion.z);
            player.resetFallDistance();
            PacketDistributor.sendToServer(RelicJumpPayload.INSTANCE);
        }
        wasJumping = jumping;
    }
}
