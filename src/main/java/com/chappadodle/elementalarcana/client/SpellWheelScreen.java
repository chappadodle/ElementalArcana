package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.SelectSpellPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Radial spell picker. Opens while the wheel key is held; point the mouse at a spell and
 * release the key (or click) to select it.
 */
public class SpellWheelScreen extends Screen {
    private static final int RADIUS = 62;
    private static final int DEAD_ZONE = 16;

    private List<Spell> spells = List.of();
    private int hovered = -1;

    public SpellWheelScreen() {
        super(Component.translatable("screen.elementalarcana.wheel"));
    }

    @Override
    protected void init() {
        spells = MagicAttachments.get(minecraft.player).castableSpells();
        if (spells.isEmpty()) {
            onClose();
        }
    }

    @Override
    public void tick() {
        InputConstants.Key key = ArcanaClient.SPELL_WHEEL.getKey();
        if (key.getType() == InputConstants.Type.KEYSYM && !InputConstants.isKeyDown(minecraft.getWindow().getWindow(), key.getValue())) {
            choose();
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (ArcanaClient.SPELL_WHEEL.matchesMouse(button)) {
            choose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && hovered >= 0) {
            choose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void choose() {
        if (hovered >= 0 && hovered < spells.size()) {
            Spell spell = spells.get(hovered);
            MagicAttachments.get(minecraft.player).select(spell.id());
            PacketDistributor.sendToServer(new SelectSpellPayload(spell.id()));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2f, 0.5f));
        }
        onClose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x40000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int centerX = width / 2;
        int centerY = height / 2;
        int count = spells.size();
        float step = Mth.TWO_PI / count;
        // Wider for many spells (cantrips and all), so the slots never overlap.
        int radius = Math.max(RADIUS, Math.round(count * 30 / Mth.TWO_PI));

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        if (dx * dx + dy * dy > DEAD_ZONE * DEAD_ZONE) {
            // Slot 0 sits at the top; angles run clockwise like the screen's y axis.
            double angle = Math.atan2(dy, dx) + Math.PI / 2 + step / 2;
            hovered = (int) (Mth.positiveModulo(angle, Mth.TWO_PI) / step) % count;
        } else {
            hovered = -1;
        }

        MagicData data = MagicAttachments.get(minecraft.player);
        Spell selected = data.selectedSpell();
        long gameTime = minecraft.player.level().getGameTime();

        for (int i = 0; i < count; i++) {
            Spell spell = spells.get(i);
            float angle = -Mth.HALF_PI + i * step;
            int slotX = centerX + Math.round(Mth.cos(angle) * radius);
            int slotY = centerY + Math.round(Mth.sin(angle) * radius);
            int half = i == hovered ? 15 : 12;
            int schoolColor = FastColor.ARGB32.opaque(spell.school().color());

            graphics.fill(slotX - half - 1, slotY - half - 1, slotX + half + 1, slotY + half + 1, i == hovered ? schoolColor : 0xFF3A3050);
            graphics.fill(slotX - half, slotY - half, slotX + half, slotY + half, i == hovered ? FastColor.ARGB32.color(120, spell.school().color()) : 0xD0101018);
            int iconSize = i == hovered ? 20 : 16;
            ArcanaDraw.icon(graphics, spell, slotX - iconSize / 2, slotY - iconSize / 2, iconSize, 1f, 1f);
            if (data.chargesReady(spell, gameTime) == 0) {
                graphics.fill(slotX - half, slotY - half, slotX + half, slotY + half, 0x90000000);
            }
            if (spell == selected) {
                graphics.fill(slotX - 2, slotY + half + 3, slotX + 2, slotY + half + 5, 0xFFFFFFFF);
            }
        }

        Spell focus = hovered >= 0 ? spells.get(hovered) : selected;
        if (focus != null) {
            int nameColor = FastColor.ARGB32.opaque(focus.school().color());
            graphics.drawCenteredString(font, focus.displayName(), centerX, centerY - 10, nameColor);
            Component cost = Component.translatable("screen.elementalarcana.cost", focus.manaCost(data.spellLevel(focus)));
            graphics.drawCenteredString(font, cost, centerX, centerY + 2, 0xFF7FB2FF);
        }
        graphics.drawCenteredString(font, Component.translatable("screen.elementalarcana.wheel.hint",
                ArcanaClient.SPELL_WHEEL.getTranslatedKeyMessage()), centerX, centerY + radius + 26, 0xFFB0A8C8);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
