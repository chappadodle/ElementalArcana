package com.chappadodle.elementalarcana.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Big spells felt on screen: a camera shake (Meteor impacts) and a white flash (Sunfire). Both
 * fade within about half a second, and both can be turned off (ArcanaClientConfig#SCREEN_EFFECTS).
 */
public final class ScreenEffects {
    private static final float MAX_SHAKE_DEGREES = 2.5f;
    private static float shake;
    private static float oldShake;
    private static float flash;
    private static float oldFlash;

    private ScreenEffects() {
    }

    /** Shakes the camera; {@code amount} 0..1. */
    public static void shake(float amount) {
        if (ArcanaClientConfig.SCREEN_EFFECTS.get()) {
            shake = Math.min(1f, Math.max(shake, amount));
        }
    }

    /** Flashes the screen white; {@code amount} 0..1 is how opaque it starts. */
    public static void flash(float amount) {
        if (ArcanaClientConfig.SCREEN_EFFECTS.get()) {
            flash = Math.min(0.9f, Math.max(flash, amount));
        }
    }

    /** Every client tick: both fade out. */
    static void tick() {
        oldShake = shake;
        shake = shake < 0.02f ? 0f : shake * 0.82f;
        oldFlash = flash;
        flash = flash < 0.02f ? 0f : flash * 0.8f;
    }

    /** Jitters the camera with a few out-of-step waves, so it doesn't look like a regular wobble. */
    static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float partialTick = (float) event.getPartialTick();
        float amount = Mth.lerp(partialTick, oldShake, shake);
        if (amount <= 0f) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float time = (minecraft.level != null ? minecraft.level.getGameTime() : 0) + partialTick;
        float degrees = amount * amount * MAX_SHAKE_DEGREES;
        event.setYaw(event.getYaw() + degrees * (Mth.sin(time * 2.1f) + 0.5f * Mth.sin(time * 5.3f)));
        event.setPitch(event.getPitch() + degrees * (Mth.sin(time * 2.7f + 1f) + 0.5f * Mth.sin(time * 4.4f)));
        event.setRoll(event.getRoll() + degrees * 0.6f * Mth.sin(time * 3.1f + 2f));
    }

    /** The flash, drawn over the whole screen. */
    public static final LayeredDraw.Layer FLASH_LAYER = (GuiGraphics graphics, DeltaTracker deltaTracker) -> {
        float amount = Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(false), oldFlash, flash);
        if (amount > 0.01f) {
            int alpha = Math.round(amount * 255);
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFF8E6);
        }
    };
}
