package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.content.MagicTriggers;
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
 * A Cantrip Scroll (see the Cantrips spec): read it (use) and its cantrip is yours for good, and
 * the scroll crumbles. To a mage whose magic still sleeps, its words mean nothing yet.
 */
public class CantripScrollItem extends Item {
    public CantripScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Spell cantrip = ModCantrips.cantripOf(stack);
        return cantrip == null ? super.getName(stack) : Component.translatable("item.elementalarcana.cantrip_scroll.of", cantrip.displayName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Spell cantrip = ModCantrips.cantripOf(stack);
        if (cantrip != null) {
            tooltip.add(cantrip.description().copy().withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("item.elementalarcana.cantrip_scroll.use").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Spell cantrip = ModCantrips.cantripOf(stack);
        if (cantrip == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.success(stack);
        }
        MagicData data = MagicAttachments.get(server);
        if (!data.isAwakened()) {
            server.displayClientMessage(Component.translatable("message.elementalarcana.cantrip.asleep"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (!data.learnCantrip(cantrip.id())) {
            server.displayClientMessage(Component.translatable("message.elementalarcana.cantrip.known", cantrip.displayName()), true);
            return InteractionResultHolder.fail(stack);
        }
        if (data.selectedSpell() == null) {
            data.select(cantrip.id());
        }
        MagicAttachments.sync(server);
        server.sendSystemMessage(Component.translatable("message.elementalarcana.cantrip.learned", cantrip.displayName())
                .withStyle(style -> style.withColor(cantrip.school().color())));
        server.serverLevel().sendParticles(ParticleTypes.ENCHANT, server.getX(), server.getY(1.2), server.getZ(), 40, 0.4, 0.5, 0.4, 0.6);
        level.playSound(null, server.getX(), server.getY(), server.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f);
        level.playSound(null, server.getX(), server.getY(), server.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        MagicTriggers.fire(server, "cantrip", cantrip.id().getPath(), data.cantrips().size());
        stack.consume(1, server);
        return InteractionResultHolder.consume(stack);
    }
}
