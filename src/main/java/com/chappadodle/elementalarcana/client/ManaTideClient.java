package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.joml.Vector3f;

/**
 * A mana tide as the player sees it (see ManaTides): the fog turns violet, and motes of mana drift up
 * around them. The server says when a tide runs.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID, value = Dist.CLIENT)
public final class ManaTideClient {
    private static final float TINT = 0.3f;
    private static final DustParticleOptions MOTE = new DustParticleOptions(new Vector3f(0.78f, 0.6f, 1f), 0.8f);

    private static boolean active;
    private static float strength;

    private ManaTideClient() {
    }

    public static boolean active() {
        return active;
    }

    public static void setActive(boolean now) {
        active = now;
    }

    @SubscribeEvent
    public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event) {
        active = false;
        strength = 0;
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (strength <= 0) {
            return;
        }
        float t = TINT * strength;
        event.setRed(Mth.lerp(t, event.getRed(), 0.55f));
        event.setGreen(Mth.lerp(t, event.getGreen(), 0.3f));
        event.setBlue(Mth.lerp(t, event.getBlue(), 0.85f));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        // Fade in and out over a few seconds rather than snapping.
        strength = Mth.clamp(strength + (active ? 0.01f : -0.01f), 0f, 1f);
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (strength <= 0 || player == null || minecraft.level == null || minecraft.isPaused() || player.tickCount % 3 != 0) {
            return;
        }
        RandomSource random = player.getRandom();
        double x = player.getX() + (random.nextDouble() - 0.5) * 16;
        double y = player.getY() + random.nextDouble() * 4 - 1;
        double z = player.getZ() + (random.nextDouble() - 0.5) * 16;
        minecraft.level.addParticle(MOTE, x, y, z, 0, 0.04, 0);
    }
}
