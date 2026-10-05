package com.chappadodle.elementalarcana.content.pouch;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * What a player carries (see the Charm Pouch spec): every stack in their inventory, and every charm
 * in the Charm Pouches there. The charms' and relics' checks look through this, so a pouched charm
 * works as a loose one does. Also reading and writing a pouch's nine slots.
 */
public final class CharmPouches {
    public static final int SIZE = 9;

    private CharmPouches() {
    }

    /**
     * A carried stack and where it lies: loose ({@code pouch} empty, the stack itself) or in slot
     * {@code slot} of {@code pouch} (a copy of what's there).
     */
    public record Carried(ItemStack stack, ItemStack pouch, int slot) {
        /** Sets a component on the stack where it lies: in place if loose, written back into its pouch if not. */
        public <T> void set(DataComponentType<T> type, T value) {
            stack.set(type, value);
            if (!pouch.isEmpty()) {
                put(pouch, slot, stack);
            }
        }
    }

    /** Whether {@code player} carries a stack that passes {@code test}, loose or in a pouch. */
    public static boolean carries(Player player, Predicate<ItemStack> test) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (test.test(stack)) {
                return true;
            }
            if (stack.is(ModPouch.CHARM_POUCH)) {
                for (ItemStack inside : contents(stack).nonEmptyItems()) {
                    if (test.test(inside)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Everything {@code player} carries, loose or in a pouch, in inventory order (a pouch's charms right after it). */
    public static List<Carried> carried(Player player) {
        List<Carried> carried = new ArrayList<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            carried.add(new Carried(stack, ItemStack.EMPTY, slot));
            if (stack.is(ModPouch.CHARM_POUCH)) {
                NonNullList<ItemStack> items = items(stack);
                for (int i = 0; i < SIZE; i++) {
                    if (!items.get(i).isEmpty()) {
                        carried.add(new Carried(items.get(i), stack, i));
                    }
                }
            }
        }
        return carried;
    }

    /** Whether a pouch takes {@code stack}: a charm or relic, never another pouch. */
    public static boolean fits(ItemStack stack) {
        return stack.is(ModPouch.CHARMS) && !stack.is(ModPouch.CHARM_POUCH);
    }

    public static ItemContainerContents contents(ItemStack pouch) {
        return pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
    }

    /** The pouch's nine slots (copies, so changing them changes nothing until they're written back). */
    public static NonNullList<ItemStack> items(ItemStack pouch) {
        NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        contents(pouch).copyInto(items);
        return items;
    }

    public static void setItems(ItemStack pouch, List<ItemStack> items) {
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    /** How many of the pouch's slots are taken. */
    public static int count(ItemStack pouch) {
        int count = 0;
        for (ItemStack ignored : contents(pouch).nonEmptyItems()) {
            count++;
        }
        return count;
    }

    static void put(ItemStack pouch, int slot, ItemStack stack) {
        NonNullList<ItemStack> items = items(pouch);
        items.set(slot, stack);
        setItems(pouch, items);
    }

    /** Slips one of {@code stack} into the pouch's first free slot (shrinking it); false if it isn't a charm or there's no room. */
    public static boolean insert(ItemStack pouch, ItemStack stack) {
        if (stack.isEmpty() || !fits(stack)) {
            return false;
        }
        NonNullList<ItemStack> items = items(pouch);
        for (int i = 0; i < SIZE; i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.split(1));
                setItems(pouch, items);
                return true;
            }
        }
        return false;
    }

    /** Whether the pouch has a free slot. */
    public static boolean hasRoom(ItemStack pouch) {
        return count(pouch) < SIZE;
    }
}
