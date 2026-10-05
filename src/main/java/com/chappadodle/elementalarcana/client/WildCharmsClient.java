package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.WildTrophyRules;
import com.chappadodle.elementalarcana.content.wild.ModWild;
import com.chappadodle.elementalarcana.content.wild.WildCharms;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Trophies of the Wild II that move their bearer (see their spec): the Plume of the Gale's leap
 * and the Crawler's Prism's climb. A player's movement is their own client's, so these happen here.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class WildCharmsClient {
    private static boolean wasOnGround = true;

    private WildCharmsClient() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.isPaused() || player.isSpectator() || player.getAbilities().flying) {
            wasOnGround = true;
            return;
        }
        boolean onGround = player.onGround();
        // A sprinting jump just left the ground: a gust carries it on.
        if (wasOnGround && !onGround && player.getDeltaMovement().y > 0.2 && player.isSprinting() && !player.isInWater()
                && WildCharms.carries(player, ModWild.PLUME_CHARM.get())) {
            Vec3 ahead = Vec3.directionFromRotation(0, player.getYRot());
            player.setDeltaMovement(player.getDeltaMovement().add(ahead.x * WildTrophyRules.LEAP_BOOST, WildTrophyRules.LEAP_LIFT,
                    ahead.z * WildTrophyRules.LEAP_BOOST));
            for (int i = 0; i < 8; i++) {
                player.level().addParticle(ParticleTypes.CLOUD, player.getX() - ahead.x * 0.4, player.getY() + 0.1 + i * 0.05,
                        player.getZ() - ahead.z * 0.4, -ahead.x * 0.08, 0.02, -ahead.z * 0.08);
            }
            player.playSound(SoundEvents.BREEZE_JUMP, 0.6f, 1.3f);
        }
        wasOnGround = onGround;
        // Walking into a wall: up it, like a crawler (sneaking holds still on it).
        if (player.horizontalCollision && player.zza > 0 && !player.isInWater() && !player.onClimbable()
                && WildCharms.carries(player, ModWild.PRISM_CHARM.get())) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, player.isShiftKeyDown() ? Math.max(motion.y, 0) : WildTrophyRules.CLIMB_SPEED, motion.z);
        }
    }
}
