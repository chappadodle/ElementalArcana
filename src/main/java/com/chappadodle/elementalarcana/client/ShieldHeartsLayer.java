package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.SpellShield;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A magic shield's strength as frosty hearts, in their own row(s) just above the health bar
 * (drawn between health and armor, so the armor bar moves up to make room, like absorption).
 */
public class ShieldHeartsLayer implements LayeredDraw.Layer {
    private static final ResourceLocation CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation FULL = ResourceLocation.withDefaultNamespace("hud/heart/frozen_full");
    private static final ResourceLocation HALF = ResourceLocation.withDefaultNamespace("hud/heart/frozen_half");

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()) {
            return;
        }
        SpellShield shield = SpellShield.of(player);
        if (!shield.isActive()) {
            return;
        }
        int halves = Mth.ceil(shield.amount());
        int hearts = Mth.ceil(halves / 2f);
        int rows = Mth.ceil(hearts / 10f);
        int left = graphics.guiWidth() / 2 - 91;
        int baseY = graphics.guiHeight() - minecraft.gui.leftHeight + 10;
        for (int i = 0; i < hearts; i++) {
            int x = left + (i % 10) * 8;
            int y = baseY - (i / 10) * 10;
            graphics.blitSprite(CONTAINER, x, y, 9, 9);
            boolean half = i == hearts - 1 && halves % 2 == 1;
            graphics.blitSprite(half ? HALF : FULL, x, y, 9, 9);
        }
        minecraft.gui.leftHeight += rows * 10;
    }
}
