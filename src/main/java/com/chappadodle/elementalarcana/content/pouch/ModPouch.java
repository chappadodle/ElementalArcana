package com.chappadodle.elementalarcana.content.pouch;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Charm Pouch (docs/superpowers/specs/2026-10-05-charm-pouch-design.md): nine charms or relics
 * in one slot, all of them still working.
 */
public final class ModPouch {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ElementalArcana.MODID);

    /** What a pouch takes: the Drakescale Charms, the Trophies of the Wild, the relics (and whatever a datapack adds). */
    public static final TagKey<Item> CHARMS = TagKey.create(Registries.ITEM, ElementalArcana.id("charms"));

    public static final DeferredItem<CharmPouchItem> CHARM_POUCH = ITEMS.register("charm_pouch",
            () -> new CharmPouchItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)
                    .component(DataComponents.CONTAINER, ItemContainerContents.EMPTY)));
    public static final DeferredHolder<MenuType<?>, MenuType<CharmPouchMenu>> MENU = MENUS.register("charm_pouch",
            () -> IMenuTypeExtension.create(CharmPouchMenu::fromNetwork));

    private ModPouch() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
    }
}
