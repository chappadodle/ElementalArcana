package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.content.brew.ModBrews;
import com.chappadodle.elementalarcana.content.hollowed.ModHollowed;
import com.chappadodle.elementalarcana.content.people.ModPeople;
import com.chappadodle.elementalarcana.content.seeker.ModSeeker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

/**
 * The Circle's stores (see the Circle spec, part 2): what the Archmagister sells for Marks of the
 * Circle, each so many times a day. The rumours are maps to the nearest crypt, sanctum and sky isle
 * not yet on anyone's map, found afresh each morning.
 */
final class CircleStores {
    private static final String[] RUMOURS = {"crypts", "sanctums", "sky_isles"};

    private CircleStores() {
    }

    /** Today's stores, for the Archmagister at {@code archmagister}'s place. */
    static MerchantOffers today(CircleMageEntity archmagister) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(sell(new ItemStack(ModCircle.SIGIL.get()), 5, 2));
        offers.add(sell(new ItemStack(ModPeople.TOME_OF_INSIGHT.get()), 6, 1));
        offers.add(sell(new ItemStack(ModPeople.SCROLL_OF_UNBINDING.get()), 3, 1));
        offers.add(sell(new ItemStack(ModHollowed.HUNGERWARD.get()), 4, 1));
        offers.add(sell(PotionContents.createItemStack(Items.POTION, ModBrews.MANA_DRAUGHT), 1, 4));
        offers.add(sell(PotionContents.createItemStack(Items.POTION, ModBrews.CLARITY_ELIXIR), 2, 2));
        offers.add(sell(new ItemStack(ModBrews.WISP_MOTE.get(), 3), 1, 4));
        if (archmagister.level() instanceof ServerLevel level) {
            for (String kind : RUMOURS) {
                MerchantOffer rumour = rumour(level, archmagister.blockPosition(), kind);
                if (rumour != null) {
                    offers.add(rumour);
                }
            }
        }
        return offers;
    }

    private static MerchantOffer sell(ItemStack item, int marks, int uses) {
        return new MerchantOffer(new ItemCost(ModCircle.MARK.get(), marks), item, uses, 0, 0f);
    }

    /** A map to the nearest place of {@code kind} the Seeker's Compass knows, not yet on a map, for two marks; or null if there's none. */
    @Nullable
    private static MerchantOffer rumour(ServerLevel level, BlockPos from, String kind) {
        BlockPos found = level.findNearestMapStructure(ModSeeker.structures(kind), from, 100, true);
        if (found == null) {
            return null;
        }
        ItemStack map = MapItem.create(level, found.getX(), found.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, map);
        MapItemSavedData.addTargetDecoration(map, found, "+", MapDecorationTypes.RED_X);
        map.set(DataComponents.ITEM_NAME, Component.translatable("filled_map.elementalarcana.rumour." + kind));
        return new MerchantOffer(new ItemCost(ModCircle.MARK.get(), 2), map, 1, 0, 0f);
    }
}
