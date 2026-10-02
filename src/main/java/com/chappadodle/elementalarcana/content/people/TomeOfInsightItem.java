package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
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
 * Sold by Master Arcanists: reading it gives one bonus skill tree point, like condensing Essence.
 * It's used up. Its words mean nothing to someone whose magic still sleeps.
 */
public class TomeOfInsightItem extends Item {

    public TomeOfInsightItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }
        MagicData data = MagicAttachments.get(serverPlayer);
        if (!data.isAwakened()) {
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.insight.dormant")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        data.addBonusTreePoint();
        MagicAttachments.sync(serverPlayer);
        stack.consume(1, serverPlayer);
        serverPlayer.sendSystemMessage(Component.translatable("message.elementalarcana.insight.done")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        serverPlayer.serverLevel().sendParticles(ParticleTypes.END_ROD, serverPlayer.getX(), serverPlayer.getY(1.0),
                serverPlayer.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1f, 1.1f);
        level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.6f, 1.4f);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.tome_of_insight.desc").withStyle(ChatFormatting.GRAY));
    }
}
