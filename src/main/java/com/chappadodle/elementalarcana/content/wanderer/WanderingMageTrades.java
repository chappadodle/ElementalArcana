package com.chappadodle.elementalarcana.content.wanderer;

import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.content.cantrip.ModCantrips;
import com.chappadodle.elementalarcana.content.creature.ModCreatures;
import com.chappadodle.elementalarcana.content.pouch.ModPouch;
import com.chappadodle.elementalarcana.content.seeker.ModSeeker;
import com.chappadodle.elementalarcana.content.star.ModStars;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;

/**
 * What a Wandering Mage trades (see its spec): rumours (maps to the nearest crypt, mage tower, sky
 * isle, drake nest or ruin, marked with a red X), wares from far away, and the things it buys.
 */
final class WanderingMageTrades {
    private static final float PRICE_MULTIPLIER = 0.05f;
    private static final List<Element> RARE_ESSENCES = List.of(Element.CRYSTAL, Element.LIGHTNING, Element.RADIANCE);

    static final VillagerTrades.ItemListing[] RUMOURS = {
            rumour("crypts", 14), rumour("mage_towers", 16), rumour("sky_isles", 12), rumour("drake_nests", 16), rumour("ruins", 10),
            rumour("hollowed_camps", 12), rumour("enclaves", 10)};

    static final VillagerTrades.ItemListing[] WARES = {
            sell(WanderingMageTrades::anyCantripScroll, 12, 2),
            sell(random -> new ItemStack(ModStars.WISHING_STAR.get()), 24, 1),
            sell(random -> new ItemStack(ModStars.STAR_FRAGMENT.get(), 2), 8, 3),
            sell(random -> new ItemStack(ModItems.essence(RARE_ESSENCES.get(random.nextInt(RARE_ESSENCES.size()))), 2), 6, 4),
            sell(random -> new ItemStack(ModSeeker.SEEKERS_COMPASS.get()), 10, 1),
            sell(random -> new ItemStack(ModPouch.CHARM_POUCH.get()), 8, 1),
            sell(random -> new ItemStack(ModCreatures.BINDING_CHARM.get()), 8, 2),
            sell(random -> PotionContents.createItemStack(Items.POTION, ModBrews.MANA_DRAUGHT), 3, 4)};

    static final VillagerTrades.ItemListing[] BUYS = {
            (trader, random) -> new MerchantOffer(new ItemCost(ModBrews.WISP_MOTE.get(), 4), new ItemStack(Items.EMERALD, 2), 8, 2, PRICE_MULTIPLIER),
            (trader, random) -> new MerchantOffer(new ItemCost(ModStars.STAR_FRAGMENT.get(), 1), new ItemStack(Items.EMERALD, 5), 6, 2, PRICE_MULTIPLIER)};

    private WanderingMageTrades() {
    }

    /** A map to the nearest place of a kind the Seeker's Compass knows (elementalarcana:seekable/<kind>). */
    private static VillagerTrades.ItemListing rumour(String kind, int emeralds) {
        return new VillagerTrades.TreasureMapForEmeralds(emeralds, ModSeeker.structures(kind), "filled_map.elementalarcana.rumour." + kind,
                MapDecorationTypes.RED_X, 1, 5);
    }

    private static VillagerTrades.ItemListing sell(Function<RandomSource, ItemStack> item, int emeralds, int maxUses) {
        return (trader, random) -> new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), item.apply(random), maxUses, 2, PRICE_MULTIPLIER);
    }

    private static ItemStack anyCantripScroll(RandomSource random) {
        List<Spell> cantrips = ModCantrips.cantrips();
        return ModCantrips.scroll(cantrips.get(random.nextInt(cantrips.size())));
    }
}
