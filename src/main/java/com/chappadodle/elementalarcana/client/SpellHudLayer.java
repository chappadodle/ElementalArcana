package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

/**
 * Bottom-left magic panel: selected spell with cooldown, mana bar (purple while mana-sick,
 * pulsing red when the next cast would overcast), XP bar and meditation indicator.
 * Fades away after a few seconds of full mana and nothing happening.
 */
public class SpellHudLayer implements LayeredDraw.Layer {
    private static final int BAR_WIDTH = 90;
    private static final int IDLE_TICKS_BEFORE_FADE = 100;
    private static final int FADE_TICKS = 20;

    private int lastBusyTick;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        Font font = minecraft.font;
        MagicData data = MagicAttachments.get(player);
        int x = 8;
        int y = graphics.guiHeight() - 34;

        if (!data.isAwakened()) {
            graphics.drawString(font, Component.translatable("hud.elementalarcana.not_awakened", Component.keybind("key.elementalarcana.status")),
                    x, y + 16, 0xFFB8A8E0);
            return;
        }

        Spell spell = data.selectedSpell();
        long gameTime = player.level().getGameTime();
        long cooldown = spell == null ? 0 : data.cooldownRemaining(spell.id(), gameTime);
        boolean sick = player.hasEffect(ModContent.MANA_SICKNESS);
        boolean busy = data.mana() < data.maxMana() || cooldown > 0 || sick || data.meditating() || spell == null;
        if (busy) {
            lastBusyTick = player.tickCount;
        }
        float alpha = 1f - Mth.clamp((player.tickCount - lastBusyTick - IDLE_TICKS_BEFORE_FADE) / (float) FADE_TICKS, 0f, 1f);
        if (alpha < 0.05f) {
            return;
        }

        graphics.fill(x - 2, y - 2, x + 18, y + 18, ArcanaDraw.withAlpha(0x90000000, alpha));
        if (spell != null) {
            ArcanaDraw.icon(graphics, spell, x, y, 16, 1f, alpha);
            int fullCooldown = spell.cooldownTicks(data.spellLevel(spell));
            if (cooldown > 0 && fullCooldown > 0) {
                int covered = (int) Math.min(16, Math.ceil(16 * cooldown / (double) fullCooldown));
                graphics.fill(x, y + 16 - covered, x + 16, y + 16, ArcanaDraw.withAlpha(0xB0000000, alpha));
                String seconds = String.valueOf((int) Math.ceil(cooldown / 20.0));
                graphics.drawString(font, seconds, x + 8 - font.width(seconds) / 2, y + 4, ArcanaDraw.withAlpha(0xFFFFFFFF, alpha));
            }
            int nameEnd = graphics.drawString(font, spell.displayName(), x + 22, y - 1,
                    ArcanaDraw.withAlpha(FastColor.ARGB32.opaque(spell.school().color()), alpha));
            if (spell.maxLevel() > 1) {
                graphics.drawString(font, Component.translatable("hud.elementalarcana.level", data.spellLevel(spell)), nameEnd + 3, y - 1,
                        ArcanaDraw.withAlpha(0xFF9A8FB8, alpha));
            }
        } else {
            graphics.drawString(font, Component.translatable("hud.elementalarcana.no_spell"), x + 22, y - 1, ArcanaDraw.withAlpha(0xFFAAAAAA, alpha));
        }

        int barX = x + 22;
        int barY = y + 9;
        int manaColor = sick ? ArcanaDraw.MANA_SICK_COLOR : ArcanaDraw.MANA_COLOR;
        if (spell != null && CastingService.healthCost(data, spell.manaCost(data.spellLevel(spell))) > 0) {
            float pulse = (Mth.sin((player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(true)) * 0.3f) + 1f) / 2f;
            manaColor = FastColor.ARGB32.lerp(pulse, manaColor, ArcanaDraw.OVERCAST_COLOR);
        }
        ArcanaDraw.bar(graphics, barX, barY, BAR_WIDTH, 5, data.mana() / data.maxMana(), manaColor, alpha);
        float xpFraction = data.xpToNextLevel() == 0 ? 1f : data.xp() / (float) data.xpToNextLevel();
        ArcanaDraw.bar(graphics, barX, barY + 7, BAR_WIDTH, 1, xpFraction, ArcanaDraw.XP_COLOR, alpha);

        String manaText = (int) data.mana() + "/" + (int) data.maxMana();
        graphics.drawString(font, manaText, barX + BAR_WIDTH + 4, barY - 1, ArcanaDraw.withAlpha(0xFF9FCBFF, alpha));
        graphics.drawString(font, Component.translatable("hud.elementalarcana.level", data.level()), barX + BAR_WIDTH + 4, barY + 7,
                ArcanaDraw.withAlpha(0xFFC9A8FF, alpha));

        if (data.freeCast()) {
            graphics.drawString(font, Component.translatable("hud.elementalarcana.free_cast"), x, y - 24, ArcanaDraw.withAlpha(0xFFFFB060, alpha));
        }
        if (data.meditating()) {
            graphics.drawString(font, Component.translatable("hud.elementalarcana.meditating"), x, y - 13, ArcanaDraw.withAlpha(0xFFE0D0FF, alpha));
        }
    }
}
