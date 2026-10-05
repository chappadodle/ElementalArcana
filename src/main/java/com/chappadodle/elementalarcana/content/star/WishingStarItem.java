package com.chappadodle.elementalarcana.content.star;

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
 * A Wishing Star (see the Starfall spec): made a wish on, it fills its maker's mana and health and
 * ends every cooldown, and is gone.
 */
public class WishingStarItem extends Item {
    public WishingStarItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.wishing_star.tooltip").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        MagicData data = MagicAttachments.get(server);
        data.fillMana();
        data.clearCooldowns();
        MagicAttachments.sync(server);
        server.setHealth(server.getMaxHealth());
        server.serverLevel().sendParticles(ParticleTypes.END_ROD, server.getX(), server.getY(1.0), server.getZ(), 40, 0.4, 0.8, 0.4, 0.15);
        server.serverLevel().sendParticles(ParticleTypes.FIREWORK, server.getX(), server.getY(1.2), server.getZ(), 30, 0.3, 0.3, 0.3, 0.2);
        level.playSound(null, server.getX(), server.getY(), server.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, 1.4f);
        level.playSound(null, server.getX(), server.getY(), server.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.6f);
        server.displayClientMessage(Component.translatable("message.elementalarcana.wish_granted").withStyle(ChatFormatting.GOLD), true);
        player.getCooldowns().addCooldown(this, 20);
        stack.consume(1, server);
        return InteractionResultHolder.consume(stack);
    }
}
