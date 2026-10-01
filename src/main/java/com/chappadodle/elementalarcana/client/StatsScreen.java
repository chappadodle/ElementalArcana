package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.api.StatRules;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.core.StatPoints;
import com.chappadodle.elementalarcana.network.StatPayload;
import com.chappadodle.elementalarcana.network.TreePayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Stats window: every level gives a stat point to spend here. One row per stat, plus one per
 * awakened element family's Affinity, each with its points (and the skill tree's bonus), what it
 * does right now and a "+" button. Right-click a row to give a point back for Essence. Hover a row
 * for what the stat is for. See StatRules for the numbers.
 */
public class StatsScreen extends Screen {
    private static final int WIDTH = 320;
    private static final int HEADER_HEIGHT = 40;
    private static final int ROW = 22;
    private static final int GOLD = 0xFFFFC857;

    /** A stat row: a Stat, or an element family's Affinity (stat == null). */
    private record Row(@Nullable Stat stat, @Nullable Element family, String key) {
    }

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> plusButtons = new ArrayList<>();
    private int left;
    private int top;

    public StatsScreen(Screen parent) {
        super(Component.translatable("screen.elementalarcana.stats"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        MagicData data = MagicAttachments.get(minecraft.player);
        rows.clear();
        plusButtons.clear();
        for (Stat stat : Stat.values()) {
            rows.add(new Row(stat, null, stat.key()));
        }
        for (Element element : Element.values()) {
            if (element.family() == element && data.affinityElements().stream().anyMatch(owned -> owned.family() == element)) {
                rows.add(new Row(null, element, StatPoints.affinityKey(element)));
            }
        }
        int panelHeight = HEADER_HEIGHT + rows.size() * ROW + 8;
        left = (width - WIDTH) / 2;
        top = Math.max(4, (height - panelHeight) / 2);

        for (int i = 0; i < rows.size(); i++) {
            String key = rows.get(i).key();
            plusButtons.add(addRenderableWidget(Button.builder(Component.literal("+"), button -> PacketDistributor.sendToServer(new StatPayload(key)))
                    .bounds(left + WIDTH - 26, rowY(i) + 2, 16, 16).build()));
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.detail.back"), button -> onClose())
                .bounds(left + 6, top + 4, 40, 14).build());
        updateButtons();
    }

    private int rowY(int index) {
        return top + HEADER_HEIGHT + index * ROW;
    }

    private void updateButtons() {
        boolean free = MagicAttachments.get(minecraft.player).statPoints() > 0;
        plusButtons.forEach(button -> button.active = free);
    }

    @Override
    public void tick() {
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        MagicData data = MagicAttachments.get(minecraft.player);
        ArcanaDraw.panel(graphics, left, top, left + WIDTH, rowY(rows.size()) + 8);

        graphics.drawCenteredString(font, title, width / 2, top + 7, 0xFFE8D8FF);
        Component level = Component.translatable("screen.elementalarcana.status.level", data.level());
        graphics.drawString(font, level, left + WIDTH - 10 - font.width(level), top + 7, 0xFFC9A8FF);
        int points = data.statPoints();
        Component unspent = Component.translatable("screen.elementalarcana.stats.unspent", points);
        graphics.drawCenteredString(font, unspent, width / 2, top + 22, points > 0 ? GOLD : 0xFF6A6480);

        Row hovered = null;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int y = rowY(i);
            boolean isHovered = mouseX >= left + 6 && mouseX < left + WIDTH - 30 && mouseY >= y && mouseY < y + ROW;
            if (isHovered) {
                graphics.fill(left + 6, y, left + WIDTH - 6, y + ROW - 2, 0x30FFFFFF);
                hovered = row;
            }
            int color = row.family() != null ? FastColor.ARGB32.opaque(row.family().color()) : 0xFFE0D8F0;
            graphics.drawString(font, name(row), left + 12, y + 6, color, false);
            int bonus = data.grants().stat(row.key());
            String value = data.stats().get(row.key()) + (bonus > 0 ? " +" + bonus : "");
            graphics.drawString(font, value, left + 124 - font.width(value), y + 6, 0xFFFFFFFF, false);
            graphics.drawString(font, effect(data, row), left + 130, y + 6, 0xFF9A8FB8, false);
        }

        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (hovered != null) {
            Component refund = data.canRefundStat(hovered.key())
                    ? Component.translatable("screen.elementalarcana.stats.refund", Progression.refundCost(data.level())).withStyle(ChatFormatting.DARK_GRAY)
                    : Component.translatable("screen.elementalarcana.stats.no_refund").withStyle(ChatFormatting.DARK_GRAY);
            graphics.renderComponentTooltip(font, List.of(name(hovered).copy().withStyle(ChatFormatting.WHITE),
                    description(hovered).copy().withStyle(ChatFormatting.GRAY), refund), mouseX, mouseY);
        }
    }

    private static Component familyName(Element family) {
        return Component.translatable("school.elementalarcana." + family.name().toLowerCase(Locale.ROOT));
    }

    private static Component name(Row row) {
        return row.stat() != null ? Component.translatable("stat.elementalarcana." + row.stat().key())
                : Component.translatable("stat.elementalarcana.affinity", familyName(row.family()));
    }

    private static Component description(Row row) {
        return row.stat() != null ? Component.translatable("stat.elementalarcana." + row.stat().key() + ".desc")
                : Component.translatable("stat.elementalarcana.affinity.desc", familyName(row.family()));
    }

    /** What the stat does right now, e.g. "Spell power ×1.22". */
    private static Component effect(MagicData data, Row row) {
        int points = data.statTotal(row.key());
        if (row.stat() == null) {
            return Component.translatable("stat.elementalarcana.affinity.effect", familyName(row.family()),
                    times(StatRules.effect(points, StatRules.AFFINITY_EXPONENT)));
        }
        String key = "stat.elementalarcana." + row.stat().key() + ".effect";
        return switch (row.stat()) {
            case RESERVOIR -> Component.translatable(key, (int) data.maxMana(), String.format(Locale.ROOT, "%.1f", data.regenPerSecond()));
            case POTENCY -> Component.translatable(key, times(StatRules.effect(points, Stat.POTENCY.exponent())));
            case FOCUS -> Component.translatable(key, percentLess(StatRules.cooldownFactor(points)));
            case WARD -> Component.translatable(key, percentLess(StatRules.wardFactor(points)));
            case VITALITY -> Component.translatable(key, times(StatRules.healthMultiplier(points)));
            case INSIGHT -> Component.translatable(key, times(StatRules.insightFactor(points)));
        };
    }

    private static String times(double multiplier) {
        return String.format(Locale.ROOT, "%.2f", multiplier);
    }

    private static String percentLess(double factor) {
        return String.valueOf(Math.round((1 - factor) * 100));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            for (int i = 0; i < rows.size(); i++) {
                int y = rowY(i);
                if (mouseX >= left + 6 && mouseX < left + WIDTH - 30 && mouseY >= y && mouseY < y + ROW) {
                    PacketDistributor.sendToServer(new TreePayload(TreePayload.Action.REFUND_STAT, rows.get(i).key()));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
