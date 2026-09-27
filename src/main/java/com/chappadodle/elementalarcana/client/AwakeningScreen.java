package com.chappadodle.elementalarcana.client;

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
        boolean first = !MagicAttachments.get(minecraft.player).isAwakened();
        graphics.drawCenteredString(font, first ? title : Component.translatable("screen.elementalarcana.awakening.again"),
                width / 2, cardsTop - 30, 0xFFE8D8FF);
        graphics.drawCenteredString(font, Component.translatable(first
                        ? "screen.elementalarcana.awakening.subtitle" : "screen.elementalarcana.awakening.subtitle_again"),
                width / 2, cardsTop - 18, 0xFF9A8FB8);

        for (int i = 0; i < choices.size(); i++) {
            SpellSchool school = choices.get(i);
            int x = cardsLeft + i * (cardWidth + GAP);
            int color = FastColor.ARGB32.opaque(school.color());
            boolean hovered = isOverCard(i, mouseX, mouseY);
            boolean isPicked = school == picked;

            graphics.fill(x - 1, cardsTop - 1, x + cardWidth + 1, cardsTop + CARD_HEIGHT + 1, isPicked || hovered ? color : 0xFF3A3050);
            graphics.fill(x, cardsTop, x + cardWidth, cardsTop + CARD_HEIGHT,
                    isPicked ? FastColor.ARGB32.color(90, school.color()) : hovered ? 0xF0201830 : ArcanaDraw.PANEL_BG);

            Spell starter = starterSpell(school);
            if (starter != null) {
                ArcanaDraw.icon(graphics, starter, x + cardWidth / 2 - 16, cardsTop + 10, 32, 1f, 1f);
            }
            graphics.drawCenteredString(font, school.displayName(), x + cardWidth / 2, cardsTop + 48, color);

            int lineY = cardsTop + 62;
            for (FormattedCharSequence line : font.split(school.description(), cardWidth - 10)) {
                graphics.drawString(font, line, x + 5, lineY, 0xFFC8C0D8, false);
                lineY += 10;
            }
            if (starter != null) {
                List<FormattedCharSequence> starts = font.split(Component.translatable("screen.elementalarcana.awakening.starts_with", starter.displayName()), cardWidth - 10);
                int startsY = cardsTop + CARD_HEIGHT - 6 - starts.size() * 10;
                for (FormattedCharSequence line : starts) {
                    graphics.drawString(font, line, x + 5, startsY, 0xFF8A8A9A, false);
                    startsY += 10;
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
