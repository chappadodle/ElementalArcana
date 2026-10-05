package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.api.BountyRules;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.ModTabs;
import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import com.chappadodle.elementalarcana.content.cantrip.ModCantrips;
import com.chappadodle.elementalarcana.api.Spell;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * What an Arcanist trades (see the Arcanist spec), level by level. Where a trade names "an element",
 * it's picked when the trade is offered, so each Arcanist stocks its own.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ArcanistTrades {
    private static final float PRICE_MULTIPLIER = 0.05f;

    private ArcanistTrades() {
    }

    @SubscribeEvent
    public static void onTrades(VillagerTradesEvent event) {
        if (event.getType() != ModPeople.ARCANIST.get()) {
            return;
        }
        Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
        // Bounties: a contract at every level (often two early on), its task rolled when offered.
        trades.get(1).add(bounty(BountyRules.Task.ATTUNED));
        trades.get(1).add(bounty(BountyRules.Task.ATTUNED));
        trades.get(2).add(bounty(BountyRules.Task.WISPS));
        trades.get(2).add(bounty(BountyRules.Task.ATTUNED));
        trades.get(3).add(bounty(BountyRules.Task.MAGUS));
        trades.get(4).add(bounty(BountyRules.Task.RIFT));
        trades.get(5).add(bounty(BountyRules.Task.ARCHMAGE));
        // Novice
        trades.get(1).add(buy(random -> new ItemCost(ModItems.essence(anyElement(random)), 2), 1, 12, 2));
        trades.get(1).add(buy(random -> new ItemCost(Items.AMETHYST_SHARD, 4), 1, 16, 2));
        trades.get(1).add(sell(random -> new ItemStack(ModItems.JOURNAL.get()), 1, new ItemCost(Items.BOOK, 1), 12, 1));
        trades.get(1).add(sell(random -> ModTabs.focusOf(ModGear.APPRENTICE_WAND.get(), anyElement(random)), 4, null, 6, 3));
        // Apprentice
        trades.get(2).add(buy(random -> new ItemCost(Items.GLOWSTONE_DUST, 4), 1, 16, 5));
        trades.get(2).add(sell(random -> new ItemStack(ModItems.essence(anyElement(random))), 5, null, 8, 5));
        trades.get(2).add(sell(random -> PotionContents.createItemStack(Items.POTION, ModBrews.MANA_DRAUGHT), 2, null, 12, 5));
        trades.get(2).add(sell(ArcanistTrades::anyCantripScroll, 10, new ItemCost(Items.PAPER, 1), 3, 5));
        trades.get(2).add(sell(random -> new ItemStack(oneOf(random, ModGear.APPRENTICE_HOOD.get(), ModGear.APPRENTICE_ROBE.get(),
                ModGear.APPRENTICE_TROUSERS.get(), ModGear.APPRENTICE_BOOTS.get())), 6, null, 4, 5));
        // Journeyman
        trades.get(3).add(sell(random -> new ItemStack(ModItems.catalyst(anyFamily(random))), 20, new ItemCost(Items.DIAMOND, 1), 2, 15));
        trades.get(3).add(sell(random -> ModTabs.focusOf(ModGear.ADEPT_STAFF.get(), anyElement(random)), 18, null, 3, 10));
        trades.get(3).add(sell(random -> PotionContents.createItemStack(Items.POTION, ModBrews.CLARITY_ELIXIR), 6, null, 6, 10));
        trades.get(3).add(sell(ArcanistTrades::anyCantripScroll, 8, new ItemCost(Items.PAPER, 1), 3, 10));
        // Expert
        trades.get(4).add(sell(random -> new ItemStack(oneOf(random, ModGear.ADEPT_HOOD.get(), ModGear.ADEPT_ROBE.get(),
                ModGear.ADEPT_TROUSERS.get(), ModGear.ADEPT_BOOTS.get())), 16, null, 3, 15));
        trades.get(4).add((trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, 24),
                Optional.of(new ItemCost(ModItems.essence(anyElement(random)), 2)), new ItemStack(ModPeople.SCROLL_OF_UNBINDING.get()),
                2, 20, PRICE_MULTIPLIER));
        // Master
        trades.get(5).add(sell(random -> new ItemStack(ModPeople.TOME_OF_INSIGHT.get()), 32, new ItemCost(Items.DIAMOND, 1), 1, 30));
        trades.get(5).add(sell(random -> new ItemStack(ModItems.catalyst(anyFamily(random))), 12, new ItemCost(Items.DIAMOND, 1), 2, 30));
        // A map to the nearest sanctum of one element (none if there's none within 1600 blocks).
        trades.get(5).add((trader, random) -> {
            Element element = SovereignRules.ELEMENTS.get(random.nextInt(SovereignRules.ELEMENTS.size()));
            String name = element.name().toLowerCase(Locale.ROOT);
            return new VillagerTrades.TreasureMapForEmeralds(24, TagKey.create(Registries.STRUCTURE, ElementalArcana.id("sanctum/" + name)),
                    "filled_map.elementalarcana.sanctum." + name, MapDecorationTypes.RED_X, 1, 30).getOffer(trader, random);
        });
    }

    /** A Bounty Contract for {@code task}, rolled for the land the Arcanist lives in. */
    private static VillagerTrades.ItemListing bounty(BountyRules.Task task) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, BountyRules.PRICE),
                Bounties.contract(task, Attunement.landElement(trader.level(), trader.blockPosition(), random), random), 1, 5, PRICE_MULTIPLIER);
    }

    /** The Arcanist buys {@code cost} for emeralds. */
    private static VillagerTrades.ItemListing buy(Function<RandomSource, ItemCost> cost, int emeralds, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(cost.apply(random), new ItemStack(Items.EMERALD, emeralds), maxUses, xp, PRICE_MULTIPLIER);
    }

    /** The Arcanist sells an item for emeralds (and, if {@code extra} isn't null, something more). */
    private static VillagerTrades.ItemListing sell(Function<RandomSource, ItemStack> item, int emeralds, ItemCost extra, int maxUses, int xp) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), Optional.ofNullable(extra), item.apply(random),
                maxUses, xp, PRICE_MULTIPLIER);
    }

    /** One of the elements creatures are commonly Attuned to, so its Essence is easy to come by. */
    private static Element anyElement(RandomSource random) {
        return AttunementRules.ATTUNABLE.get(random.nextInt(AttunementRules.ATTUNABLE.size()));
    }

    /** An element family that has a Catalyst (Fire, Water, Wind or Earth). */
    private static Element anyFamily(RandomSource random) {
        Element[] families = Arrays.stream(Element.values()).filter(element -> ModItems.catalyst(element) != null).toArray(Element[]::new);
        return families[random.nextInt(families.length)];
    }

    /** A scroll of any cantrip (see the Cantrips spec). */
    private static ItemStack anyCantripScroll(RandomSource random) {
        List<Spell> cantrips = ModCantrips.cantrips();
        return ModCantrips.scroll(cantrips.get(random.nextInt(cantrips.size())));
    }

    private static Item oneOf(RandomSource random, Item... items) {
        return items[random.nextInt(items.length)];
    }
}
