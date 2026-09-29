package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.AffinityRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.AwakenPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FastColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Choose the element your magic awakens to: first on joining, then for each new affinity slot. */
public class AwakeningScreen extends Screen {
    private static final int GAP = 8;
    private static final int CARD_HEIGHT = 136;

    private final List<SpellSchool> choices = new ArrayList<>();
    @Nullable
    private SpellSchool picked;
    private Button awakenButton;
    private int cardWidth;
    private int cardsLeft;
    private int cardsTop;

    public AwakeningScreen() {
        super(Component.translatable("screen.elementalarcana.awakening"));
    }

    @Override
    protected void init() {
        MagicData data = MagicAttachments.get(minecraft.player);
        choices.clear();
        SpellRegistries.SCHOOLS.forEach(school -> {
            if (!data.hasAffinity(school)) {
                choices.add(school);
            }
        });
        if (choices.isEmpty() || !data.hasFreeAffinitySlot()) {
            onClose();
            return;
        }
        int count = choices.size();
        cardWidth = Math.min(96, (width - 20 - (count - 1) * GAP) / count);
        cardsLeft = (width - (count * cardWidth + (count - 1) * GAP)) / 2;
        cardsTop = height / 2 - CARD_HEIGHT / 2 - 6;

        awakenButton = addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.awakening.confirm"), button -> confirm())
                .bounds(width / 2 - 60, cardsTop + CARD_HEIGHT + 12, 120, 20)
                .build());
        awakenButton.active = picked != null;
    }

    private void confirm() {
        if (picked != null) {
            PacketDistributor.sendToServer(new AwakenPayload(picked.id()));
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        MagicData data = MagicAttachments.get(minecraft.player);
        boolean first = !data.isAwakened();
        graphics.drawCenteredString(font, first ? title : Component.translatable("screen.elementalarcana.awakening.again"),
                width / 2, cardsTop - 30, 0xFFE8D8FF);
        graphics.drawCenteredString(font, Component.translatable(first
                        ? "screen.elementalarcana.awakening.subtitle" : "screen.elementalarcana.awakening.subtitle_again"),
                width / 2, cardsTop - 18, 0xFF9A8FB8);

        for (int i = 0; i < choices.size(); i++) {
            SpellSchool school = choices.get(i);
            int x = cardsLeft + i * (cardWidth + GAP);
            // Opposed to an element you hold: shown, but locked until Magic Level 30.
            Element blocker = data.opposedBy(school);
            boolean locked = blocker != null;
            int color = locked ? 0xFF5A5468 : FastColor.ARGB32.opaque(school.color());
            boolean hovered = !locked && isOverCard(i, mouseX, mouseY);
            boolean isPicked = school == picked;

            graphics.fill(x - 1, cardsTop - 1, x + cardWidth + 1, cardsTop + CARD_HEIGHT + 1, isPicked || hovered ? color : 0xFF3A3050);
            graphics.fill(x, cardsTop, x + cardWidth, cardsTop + CARD_HEIGHT,
                    isPicked ? FastColor.ARGB32.color(90, school.color()) : hovered ? 0xF0201830 : ArcanaDraw.PANEL_BG);

            Spell starter = starterSpell(school);
            if (starter != null) {
                ArcanaDraw.icon(graphics, starter, x + cardWidth / 2 - 16, cardsTop + 10, 32, locked ? 0.35f : 1f, 1f);
            }
            graphics.drawCenteredString(font, school.displayName(), x + cardWidth / 2, cardsTop + 48, color);

            int lineY = cardsTop + 62;
            for (FormattedCharSequence line : font.split(school.description(), cardWidth - 10)) {
                graphics.drawString(font, line, x + 5, lineY, locked ? 0xFF6A6480 : 0xFFC8C0D8, false);
                lineY += 10;
            }
            Component footer = locked
                    ? Component.translatable("screen.elementalarcana.awakening.opposed",
                            Component.translatable("school.elementalarcana." + blocker.name().toLowerCase(Locale.ROOT)),
                            AffinityRules.OPPOSITES_UNLOCK_LEVEL)
                    : starter != null ? Component.translatable("screen.elementalarcana.awakening.starts_with", starter.displayName()) : null;
            if (footer != null) {
                List<FormattedCharSequence> lines = font.split(footer, cardWidth - 10);
                int footerY = cardsTop + CARD_HEIGHT - 6 - lines.size() * 10;
                for (FormattedCharSequence line : lines) {
                    graphics.drawString(font, line, x + 5, footerY, locked ? 0xFFD07070 : 0xFF8A8A9A, false);
                    footerY += 10;
                }
            }
        }
    }

    private boolean isOverCard(int index, double mouseX, double mouseY) {
        int x = cardsLeft + index * (cardWidth + GAP);
        return mouseX >= x && mouseX < x + cardWidth && mouseY >= cardsTop && mouseY < cardsTop + CARD_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < choices.size(); i++) {
            if (button == 0 && isOverCard(i, mouseX, mouseY)) {
                if (MagicAttachments.get(minecraft.player).opposedBy(choices.get(i)) != null) {
                    return true;
                }
                picked = choices.get(i);
                awakenButton.active = true;
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Nullable
    private static Spell starterSpell(SpellSchool school) {
        Spell best = null;
        for (Spell spell : SpellRegistries.SPELLS) {
            if (spell.school() == school && (best == null || spell.requiredLevel() < best.requiredLevel())) {
                best = spell;
            }
        }
        return best;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
