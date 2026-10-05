package com.chappadodle.elementalarcana.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A Charm Pouch's tooltip picture (see its spec): its nine slots, three by three, with what's in them. */
public class CharmPouchTooltip implements ClientTooltipComponent {
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("container/bundle/background");
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/bundle/slot");
    private static final int COLUMNS = 3;
    private static final int SLOT_WIDTH = 18;
    private static final int SLOT_HEIGHT = 20;

    private final List<ItemStack> items;

    public CharmPouchTooltip(List<ItemStack> items) {
        this.items = items;
    }

    private int rows() {
        return (items.size() + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public int getHeight() {
        return rows() * SLOT_HEIGHT + 2 + 4;
    }

    @Override
    public int getWidth(Font font) {
        return COLUMNS * SLOT_WIDTH + 2;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        graphics.blitSprite(BACKGROUND, x, y, COLUMNS * SLOT_WIDTH + 2, rows() * SLOT_HEIGHT + 2);
        for (int i = 0; i < items.size(); i++) {
            int slotX = x + 1 + (i % COLUMNS) * SLOT_WIDTH;
            int slotY = y + 1 + (i / COLUMNS) * SLOT_HEIGHT;
            graphics.blitSprite(SLOT, slotX, slotY, 0, SLOT_WIDTH, SLOT_HEIGHT);
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                graphics.renderItem(stack, slotX + 1, slotY + 1, i);
                graphics.renderItemDecorations(font, stack, slotX + 1, slotY + 1);
            }
        }
    }
}
