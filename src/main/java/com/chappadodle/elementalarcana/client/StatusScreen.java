package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.ModContent;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The isekai "Status" window: level, XP and skill points, mana stats, affinities (with the Awaken
 * button when a slot opens), and every spell grouped by element; click one for its skill tree.
 */
public class StatusScreen extends Screen {
    private static final int WIDTH = 290;
    private static final int HEADER_HEIGHT = 118;
    private static final int SCHOOL_ROW = 14;
    private static final int SPELL_ROW = 20;
    private static final int GOLD = 0xFFFFC857;

    /** A school header (spell == null) or a spell row. */
    private record Row(SpellSchool school, @Nullable Spell spell, int height) {
    }

    private final List<Row> rows = new ArrayList<>();
    private int left;
    private int top;
    private int bottom;
    private int listTop;
    private int scroll;
    private Button awakenButton;

    public StatusScreen() {
        super(Component.translatable("screen.elementalarcana.status"));
    }

    @Override
    protected void init() {
        rows.clear();
        for (SpellSchool school : SpellRegistries.SCHOOLS) {
            rows.add(new Row(school, null, SCHOOL_ROW));
            for (Spell spell : SpellRegistries.SPELLS) {
                if (spell.school() == school) {
                    rows.add(new Row(school, spell, SPELL_ROW));
                }
            }
        }
        int panelHeight = Math.min(height - 30, HEADER_HEIGHT + contentHeight() + 10);
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        bottom = top + panelHeight;
        listTop = top + HEADER_HEIGHT;
        scroll = Mth.clamp(scroll, 0, maxScroll());

        awakenButton = addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.status.awaken"),
                        button -> minecraft.setScreen(new AwakeningScreen()))
                .bounds(left + WIDTH - 70, top + 84, 60, 16)
                .build());
        updateAwakenButton();

        if (minecraft.player.hasPermissions(2)) {
            addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.status.dev"), button -> ArcanaClient.openDevMenu(minecraft))
                    .bounds(left + WIDTH - 36, top + 4, 30, 14)
                    .build());
        }
    }

    private int contentHeight() {
        return rows.stream().mapToInt(Row::height).sum();
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (bottom - 8 - listTop));
    }

    private void updateAwakenButton() {
        awakenButton.visible = MagicAttachments.get(minecraft.player).hasFreeAffinitySlot();
    }

    @Override
    public void tick() {
        updateAwakenButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        MagicData data = MagicAttachments.get(minecraft.player);
        ArcanaDraw.panel(graphics, left, top, left + WIDTH, bottom);

        // Header: name, level, XP
        graphics.drawCenteredString(font, title, width / 2, top + 7, 0xFFE8D8FF);
        graphics.drawString(font, minecraft.player.getName(), left + 10, top + 22, 0xFFFFFFFF);
        Component level = Component.translatable("screen.elementalarcana.status.level", data.level());
        graphics.drawString(font, level, left + WIDTH - 10 - font.width(level), top + 22, 0xFFC9A8FF);
        float xpFraction = data.xpToNextLevel() == 0 ? 1f : data.xp() / (float) data.xpToNextLevel();
        ArcanaDraw.bar(graphics, left + 10, top + 34, WIDTH - 20, 3, xpFraction, ArcanaDraw.XP_COLOR, 1f);
        Component xpText = data.xpToNextLevel() == 0
                ? Component.translatable("screen.elementalarcana.status.max_level")
                : Component.translatable("screen.elementalarcana.status.xp", data.xp(), data.xpToNextLevel());
        graphics.drawString(font, xpText, left + WIDTH - 10 - font.width(xpText), top + 40, 0xFF9A8FB8, false);
        int points = data.skillPoints();
        graphics.drawString(font, Component.translatable("screen.elementalarcana.status.skill_points", points),
                left + 10, top + 40, points > 0 ? GOLD : 0xFF6A6480, false);

        // Stats
        MobEffectInstance sickness = minecraft.player.getEffect(ModContent.MANA_SICKNESS);
        stat(graphics, 0, "screen.elementalarcana.status.mana", (int) data.mana() + " / " + (int) data.maxMana(), 0xFF9FCBFF);
        String regen = String.format("%.1f/s", data.regenPerSecond()) + (data.meditating() ? " ×3" : "");
        stat(graphics, 1, "screen.elementalarcana.status.regen", regen, 0xFF9FCBFF);
        stat(graphics, 2, "screen.elementalarcana.status.power", "+" + Math.round((data.power() - 1f) * 100) + "%", 0xFFFFC870);
        stat(graphics, 3, "screen.elementalarcana.status.condition", sickness == null
                ? Component.translatable("screen.elementalarcana.status.condition.normal").getString()
                : Component.translatable("screen.elementalarcana.status.condition.sick", sickness.getDuration() / 20).getString(),
                sickness == null ? 0xFF8FE08F : 0xFFC080FF);

        // Affinities
        graphics.drawString(font, Component.translatable("screen.elementalarcana.status.affinities"), left + 10, top + 88, 0xFFB0A8C8, false);
        int badgeX = left + 72;
        int slotCount = Math.min(MagicData.AFFINITY_SLOT_LEVELS.length, SpellRegistries.SCHOOLS.size());
        for (int slot = 0; slot < slotCount; slot++) {
            Component label;
            int color;
            if (slot < data.affinities().size()) {
                SpellSchool school = SpellRegistries.SCHOOLS.get(data.affinities().get(slot));
                if (school == null) {
                    continue;
                }
                label = school.displayName();
                color = FastColor.ARGB32.opaque(school.color());
            } else if (slot < data.affinitySlots()) {
                continue; // free slot: the Awaken button covers it
            } else {
                label = Component.translatable("screen.elementalarcana.status.slot_locked", MagicData.AFFINITY_SLOT_LEVELS[slot]);
                color = 0xFF6A6480;
            }
            int labelWidth = font.width(label) + 8;
            graphics.fill(badgeX, top + 85, badgeX + labelWidth, top + 99, FastColor.ARGB32.color(60, color));
            graphics.drawString(font, label, badgeX + 4, top + 88, color, false);
            badgeX += labelWidth + 4;
        }
        graphics.fill(left + 8, listTop - 6, left + WIDTH - 8, listTop - 5, 0xFF3A3050);

        // Skills list
        Spell hovered = null;
        Spell selected = data.selectedSpell();
        graphics.enableScissor(left, listTop - 2, left + WIDTH, bottom - 4);
        int rowY = listTop - scroll;
        for (Row row : rows) {
            if (rowY + row.height() > listTop - 2 && rowY < bottom - 4) {
                if (row.spell() == null) {
                    drawSchoolRow(graphics, data, row.school(), rowY);
                } else {
                    boolean isHovered = mouseY >= Math.max(rowY, listTop) && mouseY < Math.min(rowY + row.height(), bottom - 4)
                            && mouseX >= left + 6 && mouseX < left + WIDTH - 6;
                    drawSpellRow(graphics, data, row.spell(), rowY, row.spell() == selected, isHovered);
                    if (isHovered) {
                        hovered = row.spell();
                    }
                }
            }
            rowY += row.height();
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            int track = bottom - 4 - listTop;
            int thumb = Math.max(12, track * track / contentHeight());
            int thumbY = listTop + (track - thumb) * scroll / maxScroll();
            graphics.fill(left + WIDTH - 4, thumbY, left + WIDTH - 2, thumbY + thumb, 0xFF8A7AB0);
        }

        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (hovered != null) {
            graphics.renderComponentTooltip(font, tooltip(data, hovered), mouseX, mouseY);
        }
    }

    private void stat(GuiGraphics graphics, int index, String labelKey, String value, int valueColor) {
        int x = left + 10 + (index % 2) * (WIDTH / 2);
        int y = top + 54 + (index / 2) * 13;
        Component label = Component.translatable(labelKey);
        graphics.drawString(font, label, x, y, 0xFFB0A8C8, false);
        graphics.drawString(font, value, x + font.width(label) + 6, y, valueColor, false);
    }

    private void drawSchoolRow(GuiGraphics graphics, MagicData data, SpellSchool school, int y) {
        boolean owned = data.hasAffinity(school);
        int color = FastColor.ARGB32.opaque(school.color());
        graphics.drawString(font, school.displayName(), left + 10, y + 3, owned ? color : ArcanaDraw.withAlpha(color, 0.45f), false);
        if (!owned) {
            Component note = Component.translatable("screen.elementalarcana.status.not_awakened");
            graphics.drawString(font, note, left + 14 + font.width(school.displayName()), y + 3, 0xFF6A6480, false);
        }
    }

    private void drawSpellRow(GuiGraphics graphics, MagicData data, Spell spell, int y, boolean selected, boolean hovered) {
        boolean castable = data.canCast(spell);
        int color = FastColor.ARGB32.opaque(spell.school().color());
        if (selected) {
            graphics.fill(left + 6, y, left + WIDTH - 6, y + SPELL_ROW - 2, FastColor.ARGB32.color(70, spell.school().color()));
            graphics.fill(left + 6, y, left + 8, y + SPELL_ROW - 2, color);
        } else if (hovered) {
            graphics.fill(left + 6, y, left + WIDTH - 6, y + SPELL_ROW - 2, 0x30FFFFFF);
        }
        ArcanaDraw.icon(graphics, spell, left + 14, y + 1, 16, castable ? 1f : 0.3f, 1f);
        boolean levels = spell.maxLevel() > 1;
        int nameEnd = graphics.drawString(font, spell.displayName(), left + 36, levels ? y + 1 : y + 5, castable ? color : 0xFF6A6480, false);
        if (levels) {
            int spellLevel = data.spellLevel(spell);
            graphics.drawString(font, Component.translatable("screen.elementalarcana.status.spell_level", spellLevel),
                    nameEnd + 4, y + 1, castable ? 0xFFC9A8FF : 0xFF4A4460, false);
            if (castable) {
                int needed = data.masteryToNextLevel(spell);
                float fraction = needed == 0 ? 1f : data.progress(spell).mastery() / (float) needed;
                ArcanaDraw.bar(graphics, left + 36, y + 12, 90, 2, fraction, data.isMasteryFull(spell) ? GOLD : 0xFF4FB0E0, 1f);
            }
        }

        Component right;
        int rightColor;
        if (castable && data.pendingBranchLevel(spell) > 0) {
            right = Component.translatable("screen.elementalarcana.status.choose_path");
            rightColor = GOLD;
        } else if (castable && data.canLevelUp(spell)) {
            right = Component.translatable("screen.elementalarcana.status.level_up_ready");
            rightColor = GOLD;
        } else if (castable) {
            right = Component.translatable("screen.elementalarcana.cost", spell.manaCost(data.spellLevel(spell)));
            rightColor = 0xFF7FB2FF;
        } else if (!data.hasAffinity(spell.school())) {
            right = Component.literal("—");
            rightColor = 0xFF6A6480;
        } else {
            right = Component.translatable("screen.elementalarcana.status.requires_level", spell.requiredLevel());
            rightColor = 0xFFC08040;
        }
        graphics.drawString(font, right, left + WIDTH - 12 - font.width(right), y + 5, rightColor, false);
    }

    private static List<Component> tooltip(MagicData data, Spell spell) {
        List<Component> lines = new ArrayList<>();
        int color = spell.school().color();
        lines.add(spell.displayName().copy().withStyle(style -> style.withColor(color)));
        lines.add(spell.description().copy().withStyle(ChatFormatting.GRAY));
        if (spell.maxLevel() > 1) {
            int spellLevel = data.spellLevel(spell);
            lines.add(Component.translatable("tooltip.elementalarcana.tier", spellLevel, spell.maxLevel(), spell.tierName(spellLevel))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        lines.add(Component.translatable("tooltip.elementalarcana.mana_cost", spell.manaCost(data.spellLevel(spell))).withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.elementalarcana.cooldown", String.format("%.1f", spell.cooldownTicks(data.spellLevel(spell)) / 20f)).withStyle(ChatFormatting.BLUE));
        if (!data.hasAffinity(spell.school())) {
            lines.add(Component.translatable("tooltip.elementalarcana.requires_affinity", spell.school().displayName()).withStyle(ChatFormatting.RED));
        } else if (data.level() < spell.requiredLevel()) {
            lines.add(Component.translatable("tooltip.elementalarcana.requires_level", spell.requiredLevel()).withStyle(ChatFormatting.GOLD));
        }
        lines.add(Component.translatable("tooltip.elementalarcana.click_for_details").withStyle(ChatFormatting.DARK_GRAY));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= left + 6 && mouseX < left + WIDTH - 6 && mouseY >= listTop && mouseY < bottom - 4) {
            int rowY = listTop - scroll;
            for (Row row : rows) {
                if (mouseY >= rowY && mouseY < rowY + row.height()) {
                    if (row.spell() != null) {
                        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
                        minecraft.setScreen(new SpellDetailScreen(row.spell(), this));
                        return true;
                    }
                    break;
                }
                rowY += row.height();
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) (scrollY * SPELL_ROW), 0, maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (ArcanaClient.STATUS.matches(keyCode, scanCode) || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
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
