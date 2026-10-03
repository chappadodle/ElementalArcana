package com.chappadodle.elementalarcana.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * JEI's runtime, for the dev autotest (client/AutoTest's jei_filter and jei_show steps), kept from
 * when JEI hands it to the plugin until it takes it back. Only called when JEI is loaded.
 */
public final class JeiHooks {
    @Nullable
    private static IJeiRuntime runtime;

    private JeiHooks() {
    }

    static void set(@Nullable IJeiRuntime runtime) {
        JeiHooks.runtime = runtime;
    }

    /** Filters JEI's ingredient list, as if typed in its search box ("" shows everything). */
    public static void filter(String text) {
        if (runtime != null) {
            runtime.getIngredientFilter().setFilterText(text);
        }
    }

    /** Opens JEI on the recipes for {@code stack}, and its info page if it has one. */
    public static void show(ItemStack stack) {
        if (runtime != null) {
            runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory()
                    .createFocus(RecipeIngredientRole.OUTPUT, VanillaTypes.ITEM_STACK, stack));
        }
    }
}
