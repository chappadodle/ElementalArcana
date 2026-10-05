package com.chappadodle.elementalarcana.content.pouch;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * An open Charm Pouch (see its spec): its nine slots over the player's inventory, three by three
 * as a dispenser's (a little lower, for the leather they sit on). Only charms and relics go in. Every change is written straight back into the
 * pouch, and the pouch itself can't be moved while it's open (no picking it up, no swapping it
 * away with a number key), so nothing can be in two places at once.
 */
public class CharmPouchMenu extends AbstractContainerMenu {
    private final Player player;
    /** Where the pouch is in the player's inventory (a slot of the main inventory, or the offhand's). */
    private final int pouchSlot;
    private final ItemStack pouch;
    private final SimpleContainer contents;
    private boolean loading;

    public CharmPouchMenu(int containerId, Inventory inventory, int pouchSlot) {
        super(ModPouch.MENU.get(), containerId);
        this.player = inventory.player;
        this.pouchSlot = pouchSlot;
        this.pouch = inventory.getItem(pouchSlot);
        this.contents = new SimpleContainer(CharmPouches.SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                save();
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        };
        loading = true;
        NonNullList<ItemStack> items = CharmPouches.items(pouch);
        for (int i = 0; i < CharmPouches.SIZE; i++) {
            contents.setItem(i, items.get(i));
        }
        loading = false;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new Slot(contents, column + row * 3, 62 + column * 18, 22 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return CharmPouches.fits(stack);
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addInventorySlot(inventory, column + row * 9 + 9, 8 + column * 18, 90 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            addInventorySlot(inventory, column, 8 + column * 18, 148);
        }
    }

    /** On the client, from the server's word of which slot the pouch is in. */
    static CharmPouchMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        return new CharmPouchMenu(containerId, inventory, data.readVarInt());
    }

    private void addInventorySlot(Inventory inventory, int index, int x, int y) {
        addSlot(index != pouchSlot ? new Slot(inventory, index, x, y) : new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return false;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
    }

    /** Writes the slots back into the pouch (on the server; the client hears of it with the pouch's next sync). */
    private void save() {
        if (loading || player.level().isClientSide()) {
            return;
        }
        List<ItemStack> items = new ArrayList<>(CharmPouches.SIZE);
        for (int i = 0; i < CharmPouches.SIZE; i++) {
            items.add(contents.getItem(i));
        }
        CharmPouches.setItems(pouch, items);
    }

    private boolean isPouch(int slotId) {
        if (slotId < 0 || slotId >= slots.size()) {
            return false;
        }
        Slot slot = slots.get(slotId);
        return slot.container == player.getInventory() && slot.getContainerSlot() == pouchSlot;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isPouch(slotId) || clickType == ClickType.SWAP && button == pouchSlot) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        if (index < CharmPouches.SIZE) {
            if (!moveItemStackTo(stack, CharmPouches.SIZE, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!CharmPouches.fits(stack) || !moveItemStackTo(stack, 0, CharmPouches.SIZE, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == before.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive() && player.getInventory().getItem(pouchSlot) == pouch;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        save();
    }
}
