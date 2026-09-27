package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Spell;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/** Small drawing helpers shared by the HUD and the magic screens. */
final class ArcanaDraw {
    static final int MANA_COLOR = 0xFF2F6FE0;
    static final int MANA_SICK_COLOR = 0xFF8A4FC0;
    static final int OVERCAST_COLOR = 0xFFD03030;
    static final int XP_COLOR = 0xFFB070FF;
    static final int PANEL_BG = 0xF0141020;
    static final int PANEL_BORDER = 0xFF5A4A7A;

    private ArcanaDraw() {
    }

    static int withAlpha(int argb, float alpha) {
        return FastColor.ARGB32.color(Math.round(FastColor.ARGB32.alpha(argb) * Mth.clamp(alpha, 0f, 1f)), argb);
    }

    static void panel(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        graphics.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, PANEL_BORDER);
        graphics.fill(x0, y0, x1, y1, PANEL_BG);
    }

    /** A filled bar with a dark backing and a light top edge. */
    static void bar(GuiGraphics graphics, int x, int y, int width, int height, float fraction, int color, float alpha) {
        int filled = Math.round(width * Mth.clamp(fraction, 0f, 1f));
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, withAlpha(0xC0000000, alpha));
        graphics.fill(x, y, x + filled, y + height, withAlpha(color, alpha));
        if (height > 2) {
            graphics.fill(x, y, x + filled, y + 1, withAlpha(0x60FFFFFF, alpha));
        }
    }

    static void icon(GuiGraphics graphics, Spell spell, int x, int y, int size, float brightness, float alpha) {
        RenderSystem.enableBlend();
        graphics.setColor(brightness, brightness, brightness, alpha);
        graphics.blit(spell.iconTexture(), x, y, size, size, 0, 0, 16, 16, 16, 16);
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
