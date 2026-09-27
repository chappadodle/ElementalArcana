package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.DevActionPayload;
import com.chappadodle.elementalarcana.network.DevActionPayload.Action;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Op-only testing panel: jump levels, set mana, toggle elements and free casting. */
public class DevScreen extends Screen {
    private static final int WIDTH = 330;
    private static final int ROW_HEIGHT = 24;
    private static final int LABEL_WIDTH = 62;
    private static final int BUTTON_HEIGHT = 18;
    private static final int ROWS = 6;

    private final Map<SpellSchool, Button> affinityButtons = new LinkedHashMap<>();
    private Button freeCastButton;
    private int left;
    private int top;
    // Where the next button goes while init() lays out a row.
    private int cursorX;
    private int cursorY;

    public DevScreen() {
        super(Component.translatable("screen.elementalarcana.dev"));
    }

    @Override
    protected void init() {
        affinityButtons.clear();
        left = (width - WIDTH) / 2;
        top = (height - (52 + ROWS * ROW_HEIGHT)) / 2;

        row(0);
        button("-1", 26, Action.ADD_LEVELS, -1);
        button("+1", 26, Action.ADD_LEVELS, 1);
        button("Lv 1", 34, Action.SET_LEVEL, 1);
        button("Lv 5", 34, Action.SET_LEVEL, 5);
        button("Lv 10", 38, Action.SET_LEVEL, 10);
        button("Lv 20", 38, Action.SET_LEVEL, 20);
        button("Max", 30, Action.SET_LEVEL, MagicData.MAX_LEVEL);

        row(1);
        button("+100 XP", 54, Action.ADD_XP, 100);
        button("+1000 XP", 60, Action.ADD_XP, 1000);

        row(2);
        button("0%", 30, Action.SET_MANA_PERCENT, 0);
        button("25%", 34, Action.SET_MANA_PERCENT, 25);
        button("50%", 34, Action.SET_MANA_PERCENT, 50);
        button("100%", 38, Action.SET_MANA_PERCENT, 100);
        addButton(Component.translatable("screen.elementalarcana.dev.sicken"), 46, DevActionPayload.of(Action.GIVE_SICKNESS, 0));
        addButton(Component.translatable("screen.elementalarcana.dev.cure"), 36, DevActionPayload.of(Action.CURE_SICKNESS, 0));

        row(3);
        for (SpellSchool school : SpellRegistries.SCHOOLS) {
            affinityButtons.put(school, addButton(Component.empty(), 50, DevActionPayload.toggleAffinity(school)));
        }
        addButton(Component.translatable("screen.elementalarcana.dev.reset"), 40, DevActionPayload.of(Action.RESET_AFFINITIES, 0));

        row(4);
        freeCastButton = addButton(Component.empty(), 86, DevActionPayload.of(Action.TOGGLE_FREE_CAST, 0));
        addButton(Component.translatable("screen.elementalarcana.dev.cooldowns"), 66, DevActionPayload.of(Action.RESET_COOLDOWNS, 0));
        addButton(Component.translatable("screen.elementalarcana.dev.heal"), 36, DevActionPayload.of(Action.HEAL, 0));
        addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.status"), b -> minecraft.setScreen(new StatusScreen()))
                .bounds(cursorX, cursorY, 50, BUTTON_HEIGHT).build());

        // Acts on the selected spell.
        row(5);
        spellButton("-1", 26, Action.ADD_SPELL_LEVELS, -1);
        spellButton("+1", 26, Action.ADD_SPELL_LEVELS, 1);
        spellButton("+10", 32, Action.ADD_SPELL_LEVELS, 10);
        spellButtonKey("screen.elementalarcana.dev.fill_mastery", 76, Action.FILL_MASTERY);
        spellButtonKey("screen.elementalarcana.dev.clear_paths", 70, Action.CLEAR_BRANCHES);

        updateLabels();
    }

    private int rowY(int row) {
        return top + 36 + row * ROW_HEIGHT;
    }

    private void row(int row) {
        cursorX = left + 10 + LABEL_WIDTH;
        cursorY = rowY(row);
    }

    private void spellButton(String label, int width, Action action, int value) {
        addSpellButton(Component.literal(label), width, action, value);
    }

    private void spellButtonKey(String key, int width, Action action) {
        addSpellButton(Component.translatable(key), width, action, 0);
    }

    private void addSpellButton(Component label, int width, Action action, int value) {
        addRenderableWidget(Button.builder(label, b -> {
            Spell spell = MagicAttachments.get(minecraft.player).selectedSpell();
            if (spell != null) {
                PacketDistributor.sendToServer(DevActionPayload.forSpell(action, spell, value));
            }
        }).bounds(cursorX, cursorY, width, BUTTON_HEIGHT).build());
        cursorX += width + 3;
    }

    private void button(String label, int width, Action action, int value) {
        addButton(Component.literal(label), width, DevActionPayload.of(action, value));
    }

    private Button addButton(Component label, int width, DevActionPayload payload) {
        Button button = addRenderableWidget(Button.builder(label, b -> PacketDistributor.sendToServer(payload))
                .bounds(cursorX, cursorY, width, BUTTON_HEIGHT).build());
        cursorX += width + 3;
        return button;
    }

    private void updateLabels() {
        MagicData data = MagicAttachments.get(minecraft.player);
        affinityButtons.forEach((school, button) -> {
            boolean owned = data.hasAffinity(school);
            button.setMessage(Component.literal(owned ? "✔ " : "✘ ").append(school.displayName())
                    .withStyle(style -> style.withColor(owned ? school.color() : 0x777777)));
        });
        freeCastButton.setMessage(Component.translatable(data.freeCast()
                ? "screen.elementalarcana.dev.free_cast_on" : "screen.elementalarcana.dev.free_cast_off"));
    }

    @Override
    public void tick() {
        updateLabels();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        MagicData data = MagicAttachments.get(minecraft.player);
        ArcanaDraw.panel(graphics, left, top, left + WIDTH, top + 52 + ROWS * ROW_HEIGHT);
        graphics.drawCenteredString(font, title, width / 2, top + 7, 0xFFFFB060);

        String status = "Lv " + data.level()
                + "   XP " + data.xp() + "/" + data.xpToNextLevel()
                + "   Mana " + (int) data.mana() + "/" + (int) data.maxMana()
                + (minecraft.player.hasEffect(ModContent.MANA_SICKNESS) ? "   [Sick]" : "")
                + "   Points " + data.skillPoints();
        Spell selected = data.selectedSpell();
        if (selected != null) {
            graphics.drawString(font, Component.translatable("screen.elementalarcana.dev.selected", selected.displayName(),
                            data.spellLevel(selected), data.progress(selected).mastery(), data.masteryToNextLevel(selected)),
                    left + 10, rowY(5) + ROW_HEIGHT, 0xFF8A8A9A, false);
        }
        graphics.drawCenteredString(font, status, width / 2, top + 20, 0xFFB0A8C8);

        String[] labels = {"level", "xp", "mana", "elements", "other", "spell"};
        for (int i = 0; i < labels.length; i++) {
            graphics.drawString(font, Component.translatable("screen.elementalarcana.dev.row." + labels[i]),
                    left + 10, rowY(i) + 5, FastColor.ARGB32.opaque(0xC9A8FF), false);
        }
        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ArcanaClient.DEV_MENU.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
