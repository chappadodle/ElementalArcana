package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.core.PlayerStats;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Sold by Expert Arcanists: reading it gives back every skill tree node you bought, for free (the
 * start nodes your elements give you stay). Refunding nodes one by one costs Essence; this is the
 * cheap way to rebuild.
 */
public class ScrollOfUnbindingItem extends Item {

    public ScrollOfUnbindingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        MagicData data = MagicAttachments.get(serverPlayer);
        int nodes = data.treeNodes().size();
        if (nodes == 0) {
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.unbinding.nothing")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        data.resetTree();
        PlayerStats.apply(serverPlayer);
        MagicAttachments.sync(serverPlayer);
        stack.consume(1, serverPlayer);
        serverPlayer.sendSystemMessage(Component.translatable("message.elementalarcana.unbinding.done", nodes)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        serverPlayer.serverLevel().sendParticles(ParticleTypes.ENCHANT, serverPlayer.getX(), serverPlayer.getY(1.0),
                serverPlayer.getZ(), 40, 0.6, 0.8, 0.6, 0.8);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1f, 0.8f);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1f, 0.7f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.scroll_of_unbinding.desc").withStyle(ChatFormatting.GRAY));
    }
}
