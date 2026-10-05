package com.chappadodle.elementalarcana.content.wanderer;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;

/**
 * A Wandering Mage (docs/superpowers/specs/2026-10-06-wandering-mage-design.md): a wandering trader
 * in a mage's clothes, with a mage's trades (see WanderingMageTrades): three rumours, four wares,
 * and one thing it buys, chosen when it's first traded with. It wanders, hides at night and leaves
 * as a wandering trader does.
 */
public class WanderingMageEntity extends WanderingTrader {

    public WanderingMageEntity(EntityType<? extends WanderingMageEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void updateTrades() {
        MerchantOffers offers = getOffers();
        addOffersFromItemListings(offers, WanderingMageTrades.RUMOURS, 3);
        addOffersFromItemListings(offers, WanderingMageTrades.WARES, 4);
        addOffersFromItemListings(offers, WanderingMageTrades.BUYS, 1);
    }
}
