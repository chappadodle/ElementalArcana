package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.EssenceService;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.EssencePayload;
import com.chappadodle.elementalarcana.network.SelectSpellPayload;
import com.chappadodle.elementalarcana.network.SpellProgressPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * One spell's skill tree: its level and mastery, every tier from 1 to max, and the branch cards
 * at choice levels. From here you level the spell up, pick branches, select it, or respec.
 */
public class SpellDetailScreen extends Screen {
    private static final int WIDTH = 300;
    private static final int HEADER_HEIGHT = 66;
    private static final int FOOTER_HEIGHT = 30;
    private static final int GOLD = 0xFFFFC857;

    private final Spell spell;
    private final Screen parent;
    private final List<BranchCard> cards = new ArrayList<>();
    private int left;
    private int top;
    private int bottom;
    private int listTop;
    private int listBottom;
    private int scroll;
    private int contentHeight;
    private Button levelUpButton;
    private Button infuseButton;
    private Button selectButton;
    private Button respecButton;

    /** Where a branch card was drawn this frame, so clicks can find it. */
    private record BranchCard(int level, String branch, int x0, int y0, int x1, int y1) {
    }

    public SpellDetailScreen(Spell spell, Screen parent) {
        super(spell.displayName());
        this.spell = spell;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int panelHeight = Math.min(height - 20, 320);
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;
        bottom = top + panelHeight;
        listTop = top + HEADER_HEIGHT;
        listBottom = bottom - FOOTER_HEIGHT;

        int buttonY = bottom - 24;
        levelUpButton = addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.detail.level_up"),
                        button -> PacketDistributor.sendToServer(SpellProgressPayload.levelUp(spell)))
                .bounds(left + 8, buttonY, 96, 18).build());
        selectButton = addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.detail.select"), button -> {
                    MagicAttachments.get(minecraft.player).select(spell.id());
                    PacketDistributor.sendToServer(new SelectSpellPayload(spell.id()));
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
                })
                .bounds(left + 108, buttonY, 56, 18).build());
        respecButton = addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.detail.respec"), button -> confirmRespec())
                .bounds(left + 168, buttonY, 64, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.detail.back"), button -> onClose())
                .bounds(left + WIDTH - 62, buttonY, 54, 18).build());
        // Under the mastery bar (the header has room between the bar text and the list).
        infuseButton = addRenderableWidget(Button.builder(Component.empty(),
                        button -> PacketDistributor.sendToServer(EssencePayload.infuse(spell, Screen.hasShiftDown())))
                .bounds(left + 50, top + 51, 170, 13).build());
        updateButtons();
    }

    @Override
    public void tick() {
        updateButtons();
    }

    private void updateButtons() {
        MagicData data = MagicAttachments.get(minecraft.player);
        levelUpButton.active = data.canLevelUp(spell);
        levelUpButton.setTooltip(Tooltip.create(levelUpHint(data)));
        selectButton.active = data.canCast(spell) && data.selectedSpell() != spell;
        Component noRespec = SpellProgressPayload.whyNoRespec(data, spell, minecraft.player.level().getGameTime());
        respecButton.visible = !data.progress(spell).branches().isEmpty();
        respecButton.active = noRespec == null;
        respecButton.setTooltip(Tooltip.create(noRespec != null ? noRespec : Component.translatable("screen.elementalarcana.detail.respec.hint")));
        updateInfuseButton(data);
    }

    /** "Infuse 1 Fire Essence (+32%)", or with Shift "Infuse 3 Fire Essence (fill)"; disabled with a reason. */
    private void updateInfuseButton(MagicData data) {
        Element element = EssenceService.elementOf(spell);
        infuseButton.visible = element != null && spell.maxLevel() > 1;
        if (!infuseButton.visible) {
            return;
        }
        Component essenceName = ModItems.essence(element).getDescription();
        int have = EssenceService.count(minecraft.player, element);
        boolean fill = Screen.hasShiftDown();
        int count = EssenceService.infuseCount(data, spell, fill, have);
        int percent = Math.round(Progression.essenceBarFraction(data.spellLevel(spell)) * 100);
        infuseButton.setMessage(fill
                ? Component.translatable("screen.elementalarcana.detail.infuse_fill", Math.max(count, 1), essenceName)
                : Component.translatable("screen.elementalarcana.detail.infuse", essenceName, percent));
        Component reason = null;
        if (!data.canCast(spell)) {
            reason = Component.translatable("screen.elementalarcana.detail.locked");
        } else if (data.masteryToNextLevel(spell) <= 0) {
            reason = Component.translatable("screen.elementalarcana.detail.max_level");
        } else if (data.isMasteryFull(spell)) {
            reason = Component.translatable("screen.elementalarcana.detail.infuse.full");
        } else if (have <= 0) {
            reason = Component.translatable("screen.elementalarcana.detail.infuse.none", essenceName);
        }
        infuseButton.active = reason == null;
        infuseButton.setTooltip(Tooltip.create(reason != null ? reason
                : Component.translatable("screen.elementalarcana.detail.infuse.hint", have, essenceName)));
    }

    private Component levelUpHint(MagicData data) {
        int level = data.spellLevel(spell);
        if (level >= spell.maxLevel()) {
            return Component.translatable("screen.elementalarcana.detail.max_level");
        }
        if (!data.canCast(spell)) {
            return Component.translatable("screen.elementalarcana.detail.locked");
        }
        if (!data.isMasteryFull(spell)) {
            return Component.translatable("screen.elementalarcana.detail.need_mastery");
        }
        if (data.skillPoints() <= 0) {
            return Component.translatable("screen.elementalarcana.detail.need_point");
        }
        return Component.translatable("screen.elementalarcana.detail.level_up.hint", level + 1, spell.tierName(level + 1));
    }

    private void confirmRespec() {
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PacketDistributor.sendToServer(SpellProgressPayload.respec(spell));
            }
            minecraft.setScreen(this);
        }, Component.translatable("screen.elementalarcana.detail.respec.confirm.title", spell.displayName()),
                Component.translatable("screen.elementalarcana.detail.respec.confirm")));
    }

    private void confirmBranch(int level, String branch) {
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PacketDistributor.sendToServer(SpellProgressPayload.chooseBranch(spell, level, branch));
            }
            minecraft.setScreen(this);
        }, Component.translatable("screen.elementalarcana.detail.branch.confirm.title", spell.branchName(branch)),
                Component.translatable("screen.elementalarcana.detail.branch.confirm", spell.branchDescription(branch))));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        MagicData data = MagicAttachments.get(minecraft.player);
        int color = FastColor.ARGB32.opaque(spell.school().color());
        int level = data.spellLevel(spell);
        ArcanaDraw.panel(graphics, left, top, left + WIDTH, bottom);

        // Header: big icon, name, level, mastery bar, cost/cooldown
        ArcanaDraw.icon(graphics, spell, left + 10, top + 10, 32, data.canCast(spell) ? 1f : 0.35f, 1f);
        graphics.drawString(font, spell.displayName(), left + 50, top + 10, color);
        graphics.drawString(font, spell.school().displayName(), left + 50, top + 21, 0xFF8A8A9A, false);
        Component levelText = Component.translatable("screen.elementalarcana.detail.level", level, spell.maxLevel());
        graphics.drawString(font, levelText, left + WIDTH - 10 - font.width(levelText), top + 10, 0xFFC9A8FF);
        Component points = Component.translatable("screen.elementalarcana.detail.points", data.skillPoints());
        graphics.drawString(font, points, left + WIDTH - 10 - font.width(points), top + 21, data.skillPoints() > 0 ? GOLD : 0xFF6A6480, false);

        int needed = data.masteryToNextLevel(spell);
        int mastery = data.progress(spell).mastery();
        boolean full = data.isMasteryFull(spell);
        ArcanaDraw.bar(graphics, left + 50, top + 34, WIDTH - 60, 4, needed == 0 ? 1f : mastery / (float) needed, full ? GOLD : 0xFF4FB0E0, 1f);
        Component masteryText = needed == 0
                ? Component.translatable("screen.elementalarcana.detail.mastery_max")
                : Component.translatable("screen.elementalarcana.detail.mastery", mastery, needed);
        graphics.drawString(font, masteryText, left + 50, top + 42, full ? GOLD : 0xFF9A8FB8, false);
        Component stats = Component.translatable("screen.elementalarcana.detail.stats", spell.manaCost(level), String.format("%.1f", spell.cooldownTicks(level) / 20f));
        graphics.drawString(font, stats, left + WIDTH - 10 - font.width(stats), top + 42, 0xFF7FB2FF, false);
        graphics.fill(left + 8, listTop - 6, left + WIDTH - 8, listTop - 5, 0xFF3A3050);

        // Tier list
        cards.clear();
        graphics.enableScissor(left, listTop - 2, left + WIDTH, listBottom);
        int y = listTop - scroll;
        int pending = data.pendingBranchLevel(spell);
        for (int tier = 1; tier <= spell.maxLevel(); tier++) {
            y = drawTier(graphics, data, tier, level, pending, y, mouseX, mouseY) + 6;
        }
        contentHeight = y + scroll - listTop;
        graphics.disableScissor();
        scroll = Mth.clamp(scroll, 0, maxScroll());
        if (maxScroll() > 0) {
            int track = listBottom - listTop;
            int thumb = Math.max(12, track * track / contentHeight);
            int thumbY = listTop + (track - thumb) * scroll / maxScroll();
            graphics.fill(left + WIDTH - 4, thumbY, left + WIDTH - 2, thumbY + thumb, 0xFF8A7AB0);
        }

        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /** Draws one tier and returns the y just below it. */
    private int drawTier(GuiGraphics graphics, MagicData data, int tier, int level, int pending, int y, int mouseX, int mouseY) {
        boolean reached = tier <= level;
        boolean next = tier == level + 1;
        int schoolColor = FastColor.ARGB32.opaque(spell.school().color());
        int nameColor = reached ? schoolColor : next ? 0xFFE0D8F0 : 0xFF6A6480;
        String marker = reached ? "✔" : next ? "➤" : "○";
        graphics.drawString(font, marker, left + 10, y, reached ? schoolColor : next ? GOLD : 0xFF4A4460, false);
        graphics.drawString(font, Component.translatable("screen.elementalarcana.detail.tier", tier, spell.tierName(tier)), left + 22, y, nameColor, false);
        y += 11;

        List<String> branches = spell.branchOptions(tier);
        if (branches.isEmpty()) {
            for (FormattedCharSequence line : font.split(spell.tierDescription(tier), WIDTH - 40)) {
                graphics.drawString(font, line, left + 22, y, reached ? 0xFFB8B0C8 : 0xFF5A5470, false);
                y += 10;
            }
            return y;
        }

        // Branch level: side-by-side cards.
        String chosen = data.progress(spell).branches().get(tier);
        boolean choosable = tier == pending;
        if (choosable) {
            graphics.drawString(font, Component.translatable("screen.elementalarcana.detail.choose"), left + 22, y, GOLD, false);
            y += 11;
        }
        int gap = 6;
        int cardWidth = (WIDTH - 40 - gap) / 2;
        int cardHeight = 0;
        List<List<FormattedCharSequence>> texts = new ArrayList<>();
        for (String branch : branches) {
            List<FormattedCharSequence> lines = font.split(spell.branchDescription(branch), cardWidth - 8);
            texts.add(lines);
            cardHeight = Math.max(cardHeight, 16 + lines.size() * 10);
        }
        for (int i = 0; i < branches.size(); i++) {
            String branch = branches.get(i);
            int x0 = left + 22 + i * (cardWidth + gap);
            int x1 = x0 + cardWidth;
            boolean isChosen = branch.equals(chosen);
            boolean hovered = choosable && mouseX >= x0 && mouseX < x1 && mouseY >= y && mouseY < y + cardHeight
                    && mouseY >= listTop && mouseY < listBottom;
            int border = isChosen ? schoolColor : hovered ? GOLD : choosable ? 0xFF8A7AB0 : 0xFF3A3050;
            graphics.fill(x0 - 1, y - 1, x1 + 1, y + cardHeight + 1, border);
            graphics.fill(x0, y, x1, y + cardHeight, isChosen ? FastColor.ARGB32.color(70, spell.school().color()) : hovered ? 0xF0282038 : 0xF0181424);
            boolean dim = chosen != null && !isChosen || !reached && !choosable;
            graphics.drawString(font, spell.branchName(branch), x0 + 4, y + 4, dim ? 0xFF6A6480 : isChosen ? schoolColor : 0xFFE8E0F8, false);
            int lineY = y + 15;
            for (FormattedCharSequence line : texts.get(i)) {
                graphics.drawString(font, line, x0 + 4, lineY, dim ? 0xFF5A5470 : 0xFFB8B0C8, false);
                lineY += 10;
            }
            if (choosable) {
                cards.add(new BranchCard(tier, branch, x0, y, x1, y + cardHeight));
            }
        }
        return y + cardHeight;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - (listBottom - listTop));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= listTop && mouseY < listBottom) {
            for (BranchCard card : cards) {
                if (mouseX >= card.x0() && mouseX < card.x1() && mouseY >= card.y0() && mouseY < card.y1()) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
                    confirmBranch(card.level(), card.branch());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) (scrollY * 20), 0, maxScroll());
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
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
