package com.chappadodle.elementalarcana.compat.jei;

import com.chappadodle.elementalarcana.content.wonder.ModWonders;
import com.chappadodle.elementalarcana.content.hollowed.ModHollowed;
import com.chappadodle.elementalarcana.content.flora.ModFlora;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import com.chappadodle.elementalarcana.content.hollow.ModHollow;
import com.chappadodle.elementalarcana.content.people.ModPeople;
import com.chappadodle.elementalarcana.content.sanctum.ModSanctums;
import com.chappadodle.elementalarcana.content.sanctum.SovereignHeartItem;
import com.chappadodle.elementalarcana.content.tower.GuardianCoreItem;
import com.chappadodle.elementalarcana.content.tower.MageTowerStructure;
import com.chappadodle.elementalarcana.content.tower.ModTowers;
import com.chappadodle.elementalarcana.content.infusion.ModInfusion;
import com.chappadodle.elementalarcana.content.wild.ModWild;
import com.chappadodle.elementalarcana.content.cantrip.ModCantrips;
import com.chappadodle.elementalarcana.content.star.ModStars;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * JEI integration (optional: JEI finds this class itself, and nothing else in the mod refers to it,
 * so the mod runs fine without JEI). Wands, staves, Guardian Cores and Sovereign Hearts are told
 * apart by their element (each its own entry, with its own recipe), and the things that come from
 * the world rather than a crafting table have a page saying where they're found and what they do.
 */
@JeiPlugin
public class ArcanaJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ElementalArcana.id("jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ModGear.foci().forEach(focus -> registration.registerSubtypeInterpreter(focus.get(), ElementSubtypes.INSTANCE));
        registration.registerSubtypeInterpreter(ModTowers.GUARDIAN_CORE.get(), ElementSubtypes.INSTANCE);
        registration.registerSubtypeInterpreter(ModSanctums.SOVEREIGN_HEART.get(), ElementSubtypes.INSTANCE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addItemStackInfo(stacks(Arrays.stream(Element.values()).map(ModItems::essence).toList()), info("essence"));
        registration.addItemStackInfo(stacks(Arrays.stream(Element.values()).map(ModItems::catalyst).filter(Objects::nonNull).toList()),
                info("catalyst"));
        registration.addIngredientInfo(ModBrews.WISP_MOTE.get(), info("wisp_mote"));
        registration.addItemStackInfo(MageTowerStructure.TOWER_ELEMENTS.stream().map(GuardianCoreItem::of).toList(), info("guardian_core"));
        registration.addItemStackInfo(SovereignRules.ELEMENTS.stream().map(SovereignHeartItem::of).toList(), info("sovereign_heart"));
        registration.addIngredientInfo(ModHollow.PRIME_KEY.get(), info("prime_key"));
        registration.addIngredientInfo(ModHollow.PRIME_HEART.get(), info("prime_heart"));
        registration.addIngredientInfo(ModPeople.ARCANE_LECTERN_ITEM.get(), info("arcane_lectern"));
        registration.addIngredientInfo(ModPeople.SCROLL_OF_UNBINDING.get(), info("scroll_of_unbinding"));
        registration.addIngredientInfo(ModPeople.TOME_OF_INSIGHT.get(), info("tome_of_insight"));
        registration.addIngredientInfo(ModItems.JOURNAL.get(), info("arcanist_journal"));
        registration.addIngredientInfo(ModWild.HEARTWOOD.get(), info("heartwood"));
        registration.addIngredientInfo(ModWild.WRAITH_SILK.get(), info("wraith_silk"));
        registration.addIngredientInfo(ModWild.SALAMANDER_SCALE.get(), info("salamander_scale"));
        registration.addIngredientInfo(ModWild.HARPY_PLUME.get(), info("harpy_plume"));
        registration.addIngredientInfo(ModWild.PRISM_CORE.get(), info("prism_core"));
        registration.addIngredientInfo(ModWild.BOG_PEARL.get(), info("bog_pearl"));
        registration.addIngredientInfo(ModStars.STAR_FRAGMENT.get(), info("star_fragment"));
        registration.addIngredientInfo(ModStars.STARLIT_LANTERN_ITEM.get(), info("starlit_lantern"));
        registration.addIngredientInfo(ModInfusion.ALTAR_ITEM.get(), info("infusion_altar"));
        registration.addItemStackInfo(ModCantrips.cantrips().stream().map(ModCantrips::scroll).toList(), info("cantrip_scroll"));
        registration.addItemStackInfo(stacks(ModFlora.herbItems()), info("herb"));
        registration.addIngredientInfo(ModHollowed.HOLLOW_SHARD.get(), info("hollow_shard"));
        registration.addIngredientInfo(ModHollowed.HUNGERWARD.get(), info("hungerward_charm"));
        registration.addItemStackInfo(ModWonders.items().subList(0, Element.values().length), info("bottled_glowmoth"));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        JeiHooks.set(runtime);
    }

    @Override
    public void onRuntimeUnavailable() {
        JeiHooks.set(null);
    }

    private static List<ItemStack> stacks(List<Item> items) {
        return items.stream().map(ItemStack::new).toList();
    }

    private static Component info(String item) {
        return Component.translatable("jei.elementalarcana.info." + item);
    }
}
