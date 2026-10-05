package com.chappadodle.elementalarcana.client;

import com.chappadodle.elementalarcana.api.MentorChapters;
import com.chappadodle.elementalarcana.network.MentorClaimPayload;
import com.chappadodle.elementalarcana.network.MentorPayload;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Sending Stone's screen (see The Voice in the Stone spec): Caelith's words for the chapter
 * you're on (their answer, once its task is done), the task, the reward, and a button to take it.
 * Taking it brings the next chapter up in its place.
 */
public class MentorScreen extends Screen {
    private static final int WIDTH = 260;
    private static final int HEIGHT = 222;
    private static final int TEXT = 0xFFDCD4EC;
    private static final int TITLE = 0xFFE8D8FF;
    private static final int SPEAKER = 0xFFC9A8FF;
    private static final int OPEN = 0xFFE8C872;
    private static final int DONE = 0xFF8FE08F;
    private static final int FAINT = 0xFF8A7FA8;

    private final MentorPayload state;
    private int left;
    private int top;

    public MentorScreen(MentorPayload state) {
        super(Component.translatable(chapterKey(state.chapter(), "title")));
        this.state = state;
    }

    /** Shows (or replaces) the stone's screen. */
    public static void show(MentorPayload payload) {
        Minecraft.getInstance().setScreen(new MentorScreen(payload));
    }

    private static String chapterKey(int index, String part) {
        MentorChapters.Chapter chapter = MentorChapters.at(index);
        return chapter == null ? "screen.elementalarcana.mentor.quiet." + part : "mentor.elementalarcana." + chapter.id() + "." + part;
    }

    private boolean told() {
        return MentorChapters.finished(state.chapter());
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        int y = top + HEIGHT - 28;
        if (!told() && state.done()) {
            addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.mentor.claim"),
                    button -> {
                        button.active = false;
                        PacketDistributor.sendToServer(new MentorClaimPayload(state.chapter()));
                    }).bounds(width / 2 - 104, y, 100, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.mentor.farewell"), button -> onClose())
                    .bounds(width / 2 + 4, y, 100, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("screen.elementalarcana.mentor.farewell"), button -> onClose())
                    .bounds(width / 2 - 50, y, 100, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        ArcanaDraw.panel(graphics, left, top, left + WIDTH, top + HEIGHT);
        graphics.drawCenteredString(font, title, width / 2, top + 8, TITLE);
        if (!told()) {
            Component count = Component.translatable("screen.elementalarcana.mentor.chapter", state.chapter() + 1, MentorChapters.CHAPTERS.size());
            graphics.drawCenteredString(font, count, width / 2, top + 20, FAINT);
        }
        graphics.fill(left + 10, top + 32, left + WIDTH - 10, top + 33, 0xFF3A3050);

        int y = top + 40;
        if (!told()) {
            graphics.drawString(font, Component.translatable("screen.elementalarcana.mentor.speaker"), left + 12, y, SPEAKER, false);
            y += 12;
        }
        String words = chapterKey(state.chapter(), told() ? "text" : state.done() ? "done" : "text");
        for (FormattedCharSequence line : font.split(Component.translatable(words), WIDTH - 24)) {
            graphics.drawString(font, line, left + 12, y, TEXT, false);
            y += 10;
        }

        ItemStack hovered = ItemStack.EMPTY;
        if (!told()) {
            int taskY = top + HEIGHT - 70;
            Component task = Component.translatable(chapterKey(state.chapter(), "task"));
            graphics.drawString(font, Component.translatable(state.done() ? "screen.elementalarcana.mentor.task_done"
                    : "screen.elementalarcana.mentor.task", task), left + 12, taskY, state.done() ? DONE : OPEN, false);
            int rewardY = taskY + 16;
            Component reward = Component.translatable("screen.elementalarcana.mentor.reward");
            graphics.drawString(font, reward, left + 12, rewardY + 4, FAINT, false);
            int x = left + 16 + font.width(reward);
            List<ItemStack> rewards = state.rewards();
            for (ItemStack stack : rewards) {
                graphics.renderItem(stack, x, rewardY);
                graphics.renderItemDecorations(font, stack, x, rewardY);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= rewardY && mouseY < rewardY + 16) {
                    hovered = stack;
                }
                x += 20;
            }
            if (state.xp() > 0) {
                graphics.drawString(font, Component.translatable("screen.elementalarcana.mentor.xp", state.xp()), x + 2, rewardY + 4,
                        ArcanaDraw.XP_COLOR, false);
            }
        }

        for (var renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
