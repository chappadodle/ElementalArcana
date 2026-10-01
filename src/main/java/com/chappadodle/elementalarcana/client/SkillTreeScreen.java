package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.SkillTree;
import com.chappadodle.elementalarcana.api.SkillTrees;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.content.EssenceService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.EssencePayload;
import com.chappadodle.elementalarcana.network.TreePayload;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The skill tree (see docs/superpowers/specs/2026-10-01-skill-tree-design.md): a map of nodes you
 * drag around and zoom. Click a lit node to take it, right-click one you hold to give it back (for
 * Essence). The bottom panel shows the last spell you pointed at: its level, its mastery bar (its
 * next level needs it full) and the Infuse button.
 */
public class SkillTreeScreen extends Screen {
    private static final int HEADER = 26;
    private static final int FOOTER = 44;
    private static final int GOLD = 0xFFFFC857;
    private static final float MIN_ZOOM = 0.4f;
    private static final float MAX_ZOOM = 2.2f;

    private final Screen parent;
    @Nullable
    private Spell focus;
    private boolean centred;
    private double panX;
    private double panY;
    private float zoom = 1f;
    private double dragged;
    private Button infuseButton;

    public SkillTreeScreen(Screen parent, @Nullable Spell focus) {
        super(Component.translatable("screen.elementalarcana.tree"));
        this.parent = parent;
        this.focus = focus;
    }

    @Override
    protected void init() {
        if (!centred) {
            centreOnStart();
            centred = true;
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.detail.back"), button -> onClose())
                .bounds(8, 5, 40, 16).build());
        infuseButton = addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.tree.infuse"),
                        button -> {
                            if (focus != null) {
                                PacketDistributor.sendToServer(EssencePayload.infuse(focus, hasShiftDown()));
                            }
                        })
                .bounds(width - 98, height - FOOTER + 14, 90, 18).build());
        updateInfuse();
    }

    /** Opens on the focused spell's node, or else the player's first start. */
    private void centreOnStart() {
        MagicData data = MagicAttachments.get(minecraft.player);
        SkillTree tree = SkillTrees.current();
        SkillTree.Node target = null;
        for (SkillTree.Node node : tree.nodes()) {
            boolean spellNode = node.type() == SkillTree.Type.SPELL || node.type() == SkillTree.Type.START;
            if (focus != null && spellNode && focus.id().toString().equals(node.spell())) {
                target = node;
                break;
            }
            if (target == null && node.type() == SkillTree.Type.START && data.heldNodes().contains(node.id())) {
                target = node;
            }
        }
        if (target != null) {
            panX = target.x();
            panY = target.y();
        }
    }

    private void updateInfuse() {
        MagicData data = MagicAttachments.get(minecraft.player);
        Spell spell = focus;
        boolean leveled = spell != null && data.canCast(spell) && data.masteryToNextLevel(spell) > 0;
        infuseButton.visible = leveled;
        if (!leveled) {
            return;
        }
        Element element = EssenceService.elementOf(spell);
        int have = element == null ? 0 : EssenceService.count(minecraft.player, element);
        infuseButton.active = have > 0 && !data.isMasteryFull(spell);
        infuseButton.setTooltip(Tooltip.create(Component.translatable("screen.elementalarcana.tree.infuse.hint",
                Math.round(EssenceService.infuseAmount(data, spell) * 100f / Math.max(1, data.masteryToNextLevel(spell))), have)));
    }

    @Override
    public void tick() {
        updateInfuse();
    }

    // ---- view ----

    private int viewLeft() {
        return 6;
    }

    private int viewTop() {
        return HEADER;
    }

    private int viewRight() {
        return width - 6;
    }

    private int viewBottom() {
        return height - FOOTER;
    }

    private double screenX(double x) {
        return (viewLeft() + viewRight()) / 2.0 + (x - panX) * zoom;
    }

    private double screenY(double y) {
        return (viewTop() + viewBottom()) / 2.0 + (y - panY) * zoom;
    }

    private boolean inView(double mouseX, double mouseY) {
        return mouseX >= viewLeft() && mouseX < viewRight() && mouseY >= viewTop() && mouseY < viewBottom();
    }

    private static int size(SkillTree.Node node) {
        return switch (node.type()) {
            case SMALL -> 8;
            case UPGRADE -> 10;
            case FORK -> 12;
            case SPELL, START -> 20;
        };
    }

    @Nullable
    private SkillTree.Node nodeAt(double mouseX, double mouseY) {
        if (!inView(mouseX, mouseY)) {
            return null;
        }
        for (SkillTree.Node node : SkillTrees.current().nodes()) {
            double half = Math.max(4, size(node) * zoom / 2) + 1;
            if (Math.abs(mouseX - screenX(node.x())) <= half && Math.abs(mouseY - screenY(node.y())) <= half) {
                return node;
            }
        }
        return null;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        dragged = 0;
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button == 1) {
            SkillTree.Node node = nodeAt(mouseX, mouseY);
            if (node != null) {
                PacketDistributor.sendToServer(new TreePayload(TreePayload.Action.REFUND, node.id()));
                return true;
            }
        }
        return inView(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && inView(mouseX, mouseY)) {
            panX -= dragX / zoom;
            panY -= dragY / zoom;
            dragged += Math.abs(dragX) + Math.abs(dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragged < 3) {
            SkillTree.Node node = nodeAt(mouseX, mouseY);
            if (node != null) {
                PacketDistributor.sendToServer(new TreePayload(TreePayload.Action.TAKE, node.id()));
            }
        }
        dragged = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!inView(mouseX, mouseY)) {
            return false;
        }
        // Zoom around the cursor: the point under it stays put.
        double worldX = (mouseX - (viewLeft() + viewRight()) / 2.0) / zoom + panX;
        double worldY = (mouseY - (viewTop() + viewBottom()) / 2.0) / zoom + panY;
        zoom = Mth.clamp(zoom * (float) Math.pow(1.15, scrollY), MIN_ZOOM, MAX_ZOOM);
        panX = worldX - (mouseX - (viewLeft() + viewRight()) / 2.0) / zoom;
        panY = worldY - (mouseY - (viewTop() + viewBottom()) / 2.0) / zoom;
        return true;
    }

    // ---- drawing ----

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        MagicData data = MagicAttachments.get(minecraft.player);
        SkillTree tree = SkillTrees.current();
        Set<String> held = data.heldNodes();
        // Worked out once a frame: the nodes that can be taken right now.
        Set<String> takeable = new HashSet<>();
        for (SkillTree.Node node : tree.nodes()) {
            if (!held.contains(node.id()) && data.checkTake(node.id()) == SkillTree.Check.OK) {
                takeable.add(node.id());
            }
        }

        ArcanaDraw.panel(graphics, viewLeft(), viewTop(), viewRight(), viewBottom());
        graphics.enableScissor(viewLeft(), viewTop(), viewRight(), viewBottom());
        drawLinks(graphics, tree, held, takeable);
        long time = Util.getMillis();
        for (SkillTree.Node node : tree.nodes()) {
            drawNode(graphics, node, held.contains(node.id()), takeable.contains(node.id()), time);
        }
        graphics.disableScissor();

        // Header
        graphics.drawCenteredString(font, title, width / 2, 9, 0xFFE8D8FF);
        int points = data.treePoints();
        Component pointsText = Component.translatable("screen.elementalarcana.tree.points", points);
        graphics.drawString(font, pointsText, width - 10 - font.width(pointsText), 9, points > 0 ? GOLD : 0xFF6A6480);

        SkillTree.Node hovered = nodeAt(mouseX, mouseY);
        if (hovered != null && hovered.spell() != null) {
            Spell spell = spellOf(hovered.spell());
            if (spell != null) {
                focus = spell;
            }
        }
        drawFooter(graphics, data);

        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (hovered != null) {
            graphics.renderComponentTooltip(font, tooltip(data, hovered, held.contains(hovered.id())), mouseX, mouseY);
        }
    }

    private void drawLinks(GuiGraphics graphics, SkillTree tree, Set<String> held, Set<String> takeable) {
        VertexConsumer consumer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f pose = graphics.pose().last().pose();
        for (List<String> pair : tree.linkPairs()) {
            SkillTree.Node a = tree.node(pair.get(0));
            SkillTree.Node b = tree.node(pair.get(1));
            if (a == null || b == null) {
                continue;
            }
            boolean heldA = held.contains(a.id());
            boolean heldB = held.contains(b.id());
            int color;
            float width;
            if (heldA && heldB) {
                color = GOLD;
                width = 2.2f;
            } else if (heldA && takeable.contains(b.id()) || heldB && takeable.contains(a.id())) {
                color = 0xFFB8A8E0;
                width = 1.6f;
            } else {
                color = 0xFF3E3458;
                width = 1.4f;
            }
            line(consumer, pose, screenX(a.x()), screenY(a.y()), screenX(b.x()), screenY(b.y()), Math.max(1f, width * zoom), color);
        }
        graphics.flush();
    }

    /** A thick line as a quad (both windings, so it shows whichever way it runs). */
    private static void line(VertexConsumer consumer, Matrix4f pose, double x0, double y0, double x1, double y1, float width, int color) {
        double length = Math.hypot(x1 - x0, y1 - y0);
        if (length < 0.5) {
            return;
        }
        float nx = (float) (-(y1 - y0) / length * width / 2);
        float ny = (float) ((x1 - x0) / length * width / 2);
        float[][] corners = {
                {(float) x0 + nx, (float) y0 + ny}, {(float) x0 - nx, (float) y0 - ny},
                {(float) x1 - nx, (float) y1 - ny}, {(float) x1 + nx, (float) y1 + ny}};
        int[][] orders = {{0, 1, 2, 3}, {3, 2, 1, 0}};
        for (int[] order : orders) {
            for (int i : order) {
                consumer.addVertex(pose, corners[i][0], corners[i][1], 0).setColor(color);
            }
        }
    }

    private void drawNode(GuiGraphics graphics, SkillTree.Node node, boolean held, boolean takeable, long time) {
        int half = Math.max(2, Math.round(size(node) * zoom / 2));
        int x = (int) Math.round(screenX(node.x()));
        int y = (int) Math.round(screenY(node.y()));
        if (x + half < viewLeft() || x - half > viewRight() || y + half < viewTop() || y - half > viewBottom()) {
            return;
        }
        int color = nodeColor(node);
        float pulse = 0.65f + 0.35f * Mth.sin(time / 250f);
        int border = held ? 0xFFFFFFFF : takeable ? ArcanaDraw.withAlpha(GOLD, pulse) : 0xFF2A2440;
        if (node.type() == SkillTree.Type.FORK) {
            border = held ? GOLD : border;
        }
        graphics.fill(x - half - 1, y - half - 1, x + half + 1, y + half + 1, border);
        Spell spell = node.spell() == null ? null : spellOf(node.spell());
        if ((node.type() == SkillTree.Type.SPELL || node.type() == SkillTree.Type.START) && spell != null) {
            graphics.fill(x - half, y - half, x + half, y + half, 0xFF141020);
            ArcanaDraw.icon(graphics, spell, x - half, y - half, half * 2, held ? 1f : takeable ? 0.75f : 0.3f, 1f);
        } else {
            float shade = held ? 1f : takeable ? 0.7f : 0.3f;
            graphics.fill(x - half, y - half, x + half, y + half, darken(color, shade));
        }
    }

    private static int darken(int color, float factor) {
        return FastColor.ARGB32.color(255, Math.round(FastColor.ARGB32.red(color) * factor),
                Math.round(FastColor.ARGB32.green(color) * factor), Math.round(FastColor.ARGB32.blue(color) * factor));
    }

    private static int nodeColor(SkillTree.Node node) {
        if (node.type() == SkillTree.Type.SMALL && node.stat() != null) {
            return statColor(node.stat());
        }
        Spell spell = node.spell() == null ? null : spellOf(node.spell());
        return spell != null ? FastColor.ARGB32.opaque(spell.school().color()) : 0xFF9A8FB8;
    }

    static int statColor(String key) {
        if (key.startsWith("affinity/")) {
            Element element = elementNamed(key.substring("affinity/".length()));
            return element == null ? 0xFF9A8FB8 : FastColor.ARGB32.opaque(element.color());
        }
        return switch (key) {
            case "reservoir" -> 0xFF4F8FFF;
            case "potency" -> 0xFFFF6A5A;
            case "focus" -> 0xFFFFD84F;
            case "ward" -> 0xFFB8C0D0;
            case "vitality" -> 0xFFFF8FB0;
            case "insight" -> 0xFFB57FFF;
            default -> 0xFF9A8FB8;
        };
    }

    private void drawFooter(GuiGraphics graphics, MagicData data) {
        int top = height - FOOTER + 6;
        Spell spell = focus;
        if (spell == null) {
            graphics.drawCenteredString(font, Component.translatable("screen.elementalarcana.tree.hint"), width / 2, top + 12, 0xFF9A8FB8);
            return;
        }
        boolean castable = data.canCast(spell);
        ArcanaDraw.icon(graphics, spell, 10, top + 4, 24, castable ? 1f : 0.35f, 1f);
        int color = castable ? FastColor.ARGB32.opaque(spell.school().color()) : 0xFF6A6480;
        int nameEnd = graphics.drawString(font, spell.displayName(), 40, top + 4, color, false);
        if (spell.maxLevel() > 1 && castable) {
            int level = data.spellLevel(spell);
            graphics.drawString(font, Component.translatable("screen.elementalarcana.tree.spell_level", level, spell.maxLevel()),
                    nameEnd + 6, top + 4, 0xFFC9A8FF, false);
            int needed = data.masteryToNextLevel(spell);
            int mastery = data.progress(spell).mastery();
            float fraction = needed == 0 ? 1f : mastery / (float) needed;
            ArcanaDraw.bar(graphics, 40, top + 17, 160, 4, fraction, data.isMasteryFull(spell) ? GOLD : 0xFF4FB0E0, 1f);
            Component masteryText = needed == 0 ? Component.translatable("screen.elementalarcana.tree.mastered")
                    : Component.translatable("screen.elementalarcana.tree.mastery", mastery, needed);
            graphics.drawString(font, masteryText, 206, top + 15, 0xFF9A8FB8, false);
        } else if (!castable) {
            graphics.drawString(font, Component.translatable("screen.elementalarcana.tree.locked_spell"), 40, top + 16, 0xFF6A6480, false);
        }
        graphics.drawCenteredString(font, Component.translatable("screen.elementalarcana.tree.hint"), width / 2, height - 10, 0xFF5A5070);
    }

    // ---- tooltips ----

    private List<Component> tooltip(MagicData data, SkillTree.Node node, boolean held) {
        List<Component> lines = new ArrayList<>();
        Spell spell = node.spell() == null ? null : spellOf(node.spell());
        switch (node.type()) {
            case SMALL -> lines.add(Component.translatable("screen.elementalarcana.tree.small", node.amount(),
                    statName(node.stat())).withColor(statColor(node.stat())));
            case START -> {
                if (spell != null) {
                    lines.add(spell.displayName().copy().withColor(spell.school().color()));
                    lines.add(Component.translatable("screen.elementalarcana.tree.start", elementName(node.element())).withStyle(ChatFormatting.GRAY));
                }
            }
            case SPELL -> {
                if (spell != null) {
                    lines.add(spell.displayName().copy().withColor(spell.school().color()));
                    lines.add(spell.description().copy().withStyle(ChatFormatting.GRAY));
                }
            }
            case UPGRADE -> {
                if (spell != null) {
                    lines.add(Component.translatable("screen.elementalarcana.tree.upgrade", spell.displayName(), node.spellLevel(),
                            spell.tierName(node.spellLevel())).withColor(spell.school().color()));
                    lines.add(spell.tierDescription(node.spellLevel()).copy().withStyle(ChatFormatting.GRAY));
                }
            }
            case FORK -> {
                if (spell != null) {
                    lines.add(Component.translatable("screen.elementalarcana.tree.upgrade", spell.displayName(), node.spellLevel(),
                            spell.branchName(node.branch())).withColor(spell.school().color()));
                    lines.add(spell.branchDescription(node.branch()).copy().withStyle(ChatFormatting.GRAY));
                    lines.add(Component.translatable("screen.elementalarcana.tree.fork").withStyle(ChatFormatting.GOLD));
                }
            }
        }
        if (node.requiresStat() != null) {
            boolean met = data.statTotal(node.requiresStat()) >= node.requiresMin();
            lines.add(Component.translatable("screen.elementalarcana.tree.requires", statName(node.requiresStat()), node.requiresMin())
                    .withStyle(met ? ChatFormatting.DARK_GREEN : ChatFormatting.RED));
        }
        int refundCost = Progression.refundCost(data.level());
        if (held) {
            SkillTree.Check refund = data.checkRefund(node.id());
            lines.add(refund == SkillTree.Check.OK
                    ? Component.translatable("screen.elementalarcana.tree.refund", refundCost).withStyle(ChatFormatting.DARK_GRAY)
                    : reason(refund).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            SkillTree.Check take = data.checkTake(node.id());
            lines.add(take == SkillTree.Check.OK
                    ? Component.translatable("screen.elementalarcana.tree.take").withStyle(ChatFormatting.GREEN)
                    : reason(take).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    private static Component statName(@Nullable String key) {
        if (key == null) {
            return Component.empty();
        }
        if (key.startsWith("affinity/")) {
            return Component.translatable("stat.elementalarcana.affinity", elementName(key.substring("affinity/".length())));
        }
        return Component.translatable("stat.elementalarcana." + key);
    }

    private static Component elementName(@Nullable String element) {
        return Component.translatable("school.elementalarcana." + (element == null ? "none" : element.toLowerCase(Locale.ROOT)));
    }

    private static MutableComponent reason(SkillTree.Check check) {
        return Component.translatable("screen.elementalarcana.tree.check." + check.name().toLowerCase(Locale.ROOT));
    }

    @Nullable
    private static Element elementNamed(String name) {
        for (Element element : Element.values()) {
            if (element.name().equalsIgnoreCase(name)) {
                return element;
            }
        }
        return null;
    }

    @Nullable
    private static Spell spellOf(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location == null ? null : SpellRegistries.SPELLS.get(location);
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
