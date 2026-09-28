package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** The mod's items: one Elemental Essence per element, listed in the Ingredients creative tab. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final Map<Element, DeferredItem<ElementalEssenceItem>> ESSENCES = new EnumMap<>(Element.class);

    static {
        for (Element element : Element.values()) {
            ESSENCES.put(element, ITEMS.register(element.name().toLowerCase(Locale.ROOT) + "_essence",
                    () -> new ElementalEssenceItem(element, new Item.Properties().rarity(Rarity.UNCOMMON))));
        }
    }

    private ModItems() {
    }

    public static Item essence(Element element) {
        return ESSENCES.get(element).get();
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModItems::addToCreativeTabs);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            ESSENCES.values().forEach(event::accept);
        }
    }
}
