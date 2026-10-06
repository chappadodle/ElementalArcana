package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.content.hollowed.ModHollowed;
import com.chappadodle.elementalarcana.content.flora.ModFlora;
import com.chappadodle.elementalarcana.content.wanderer.ModWanderer;
import com.chappadodle.elementalarcana.api.DrakeRules;
import com.chappadodle.elementalarcana.content.drake.ModDrakes;
import com.chappadodle.elementalarcana.content.wild.ModWild;
import com.chappadodle.elementalarcana.content.infusion.ModInfusion;
import com.chappadodle.elementalarcana.content.cantrip.ModCantrips;
import com.chappadodle.elementalarcana.api.Relic;
import com.chappadodle.elementalarcana.content.relic.ModRelics;
import com.chappadodle.elementalarcana.content.crypt.ModCrypts;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
import com.chappadodle.elementalarcana.content.creature.ModCreatures;
import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import com.chappadodle.elementalarcana.content.hollow.ModHollow;
import com.chappadodle.elementalarcana.content.people.ModPeople;
import com.chappadodle.elementalarcana.content.sanctum.ModSanctums;
import com.chappadodle.elementalarcana.content.sanctum.SovereignHeartItem;
import com.chappadodle.elementalarcana.content.tower.GuardianCoreItem;
import com.chappadodle.elementalarcana.content.tower.MageTowerStructure;
import com.chappadodle.elementalarcana.content.tower.ModTowers;
import com.chappadodle.elementalarcana.content.star.ModStars;
import com.chappadodle.elementalarcana.content.pouch.ModPouch;
import com.chappadodle.elementalarcana.content.seeker.ModSeeker;
import com.chappadodle.elementalarcana.content.mentor.ModMentor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The mod's own creative tab: Essence, Catalysts, the Journal, gear (a focus of every element), the Arcanist's goods and wisp eggs. */
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
                for (var focus : ModGear.foci()) {
                    for (Element element : Element.values()) {
                        output.accept(focusOf(focus.get(), element));
                    }
                }
                ModGear.robes().forEach(item -> output.accept(item.get()));
                output.accept(ModPeople.ARCANE_LECTERN_ITEM.get());
                output.accept(ModPeople.SCROLL_OF_UNBINDING.get());
                output.accept(ModPeople.TOME_OF_INSIGHT.get());
                for (Element element : MageTowerStructure.TOWER_ELEMENTS) {
                    output.accept(GuardianCoreItem.of(element));
                }
                for (Element element : SovereignRules.ELEMENTS) {
                    output.accept(SovereignHeartItem.of(element));
                }
                output.accept(ModHollow.PRIME_KEY.get());
                output.accept(ModHollow.PRIME_HEART.get());
                output.accept(ModBrews.WISP_MOTE.get());
                output.accept(ModCreatures.BINDING_CHARM.get());
                ModBrews.potions().forEach(potion -> output.accept(PotionContents.createItemStack(Items.POTION, potion)));
                output.accept(ModTowers.ACOLYTE_EGG.get());
                output.accept(ModTowers.MAGISTER_EGG.get());
                for (Element element : Element.values()) {
                    output.accept(ModCreatures.wispEgg(element));
                }
                for (Element element : ModCreatures.GOLEM_ELEMENTS) {
                    output.accept(ModCreatures.golemEgg(element));
                }
                for (Element element : SovereignRules.ELEMENTS) {
                    output.accept(ModSanctums.sovereignEgg(element));
                }
                output.accept(ModHollow.HOLLOW_EGG.get());
                ModCrypts.items().forEach(item -> output.accept(item.get()));
                output.accept(ModCrypts.REVENANT_EGG.get());
                for (Relic relic : Relic.values()) {
                    output.accept(ModRelics.item(relic));
                }
                for (Element element : DrakeRules.ELEMENTS) {
                    output.accept(ModDrakes.scale(element));
                }
                for (Element element : DrakeRules.ELEMENTS) {
                    output.accept(ModDrakes.charm(element));
                }
                for (Element element : DrakeRules.ELEMENTS) {
                    output.accept(ModDrakes.egg(element));
                }
                for (Element element : DrakeRules.ELEMENTS) {
                    output.accept(ModDrakes.eggItem(element));
                }
                output.accept(ModDrakes.SADDLE.get());
                ModWild.items().forEach(item -> output.accept(item.get()));
                output.accept(ModInfusion.ALTAR_ITEM.get());
                ModCantrips.cantrips().forEach(cantrip -> output.accept(ModCantrips.scroll(cantrip)));
                ModStars.items().forEach(item -> output.accept(item.get()));
                output.accept(ModPouch.CHARM_POUCH.get());
                output.accept(ModSeeker.SEEKERS_COMPASS.get());
                output.accept(ModMentor.SENDING_STONE.get());
                output.accept(ModWanderer.WANDERING_MAGE_EGG.get());
                ModFlora.herbItems().forEach(herb -> output.accept(herb));
                ModFlora.tonics().forEach(tonic -> output.accept(PotionContents.createItemStack(Items.POTION, tonic)));
                ModHollowed.items().forEach(item -> output.accept(item.get()));
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
