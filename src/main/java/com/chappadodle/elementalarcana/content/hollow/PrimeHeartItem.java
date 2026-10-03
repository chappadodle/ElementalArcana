package com.chappadodle.elementalarcana.content.hollow;

import com.chappadodle.elementalarcana.api.HollowRules;
import com.chappadodle.elementalarcana.content.Awakenings;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * The Heart of the Prime, what binding the Hollow leaves (see the Hollow spec). Used, it is spent:
 * your magic remembers that it was one. Every element you don't hold awakens, opposites included,
 * and you get 5 tree points.
 */
public class PrimeHeartItem extends Item {

    public PrimeHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        Awakenings.wakeAll(serverPlayer);
        MagicData data = MagicAttachments.get(serverPlayer);
        for (int i = 0; i < HollowRules.PRIME_TREE_POINTS; i++) {
            data.addBonusTreePoint();
        }
        MagicAttachments.sync(serverPlayer);
        stack.consume(1, serverPlayer);
        serverPlayer.sendSystemMessage(Component.translatable("message.elementalarcana.prime_heart.used").withStyle(ChatFormatting.LIGHT_PURPLE));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.prime_heart.desc").withStyle(ChatFormatting.GRAY));
    }
}
