package com.chappadodle.elementalarcana.content.pouch;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Charm Pouch (see its spec): used, it opens on its nine slots; a charm right-clicked onto it
 * (or it onto a charm) slips in, as with a bundle; its tooltip shows what's inside.
 */
public class CharmPouchItem extends Item {

    /** The tooltip's picture: the pouch's nine slots. */
    public record Contents(List<ItemStack> items) implements TooltipComponent {
    }

    public CharmPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, opener) -> new CharmPouchMenu(id, inventory, slot),
                    stack.getHoverName()), buffer -> buffer.writeVarInt(slot));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_LEATHER, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** The pouch on the cursor, right-clicked onto a charm in a slot: the charm goes in. */
    @Override
    public boolean overrideStackedOnOther(ItemStack pouch, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || pouch.getCount() != 1) {
            return false;
        }
        ItemStack charm = slot.getItem();
        if (charm.isEmpty() || !CharmPouches.fits(charm) || !CharmPouches.hasRoom(pouch)) {
            return false;
        }
        ItemStack taken = slot.safeTake(charm.getCount(), 1, player);
        if (CharmPouches.insert(pouch, taken)) {
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
        }
        return true;
    }

    /** A charm on the cursor, right-clicked onto the pouch in a slot: the charm goes in. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pouch, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || pouch.getCount() != 1 || !slot.allowModification(player)) {
            return false;
        }
        if (CharmPouches.insert(pouch, other)) {
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.8f + player.level().getRandom().nextFloat() * 0.4f);
            return true;
        }
        return false;
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.of(new Contents(CharmPouches.items(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int count = CharmPouches.count(stack);
        tooltip.add(Component.translatable("item.elementalarcana.charm_pouch.count", count, CharmPouches.SIZE).withStyle(ChatFormatting.GRAY));
        if (count == 0) {
            tooltip.add(Component.translatable("item.elementalarcana.charm_pouch.tip").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
