package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own creative tab: Essence, Catalysts, the Journal, and gear (a focus of every element). */
public final class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ElementalArcana.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.elementalarcana"))
            .icon(() -> new ItemStack(ModItems.JOURNAL.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.JOURNAL.get());
                for (Element element : Element.values()) {
                    output.accept(ModItems.essence(element));
                }
                for (Element element : Element.values()) {
                    Item catalyst = ModItems.catalyst(element);
                    if (catalyst != null) {
                        output.accept(catalyst);
                    }
                }
                for (Item focus : new Item[]{ModGear.APPRENTICE_WAND.get(), ModGear.ADEPT_STAFF.get()}) {
                    for (Element element : Element.values()) {
                        output.accept(focusOf(focus, element));
                    }
                }
                ModGear.all().stream().skip(2).forEach(item -> output.accept(item.get()));
            })
            .build());

    private ModTabs() {
    }

    /** A focus attuned to {@code element}. */
    public static ItemStack focusOf(Item focus, Element element) {
        ItemStack stack = new ItemStack(focus);
        stack.set(ModGear.ELEMENT.get(), element);
        return stack;
    }
}
