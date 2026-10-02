package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.client.JournalBook;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Arcanist's Journal: the guide given when your magic first wakes, also crafted from a book and
 * any Essence. Its pages come from the language file (so they follow the mod's updates), plus a
 * chapter for each element you hold (see client/JournalBook).
 */
public class JournalItem extends Item {

    public JournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            JournalBook.open(player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
