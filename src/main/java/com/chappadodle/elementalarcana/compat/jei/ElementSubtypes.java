package com.chappadodle.elementalarcana.compat.jei;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/** Tells an item's element variants apart (the element component): a Fire Adept Staff isn't a Water one. */
enum ElementSubtypes implements ISubtypeInterpreter<ItemStack> {
    INSTANCE;

    @Override
    @Nullable
    public Object getSubtypeData(ItemStack stack, UidContext context) {
        return stack.get(ModGear.ELEMENT.get());
    }

    @Override
    public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
        Element element = stack.get(ModGear.ELEMENT.get());
        return element == null ? "" : element.name().toLowerCase(Locale.ROOT);
    }
}
