package com.chappadodle.elementalarcana.content.relic;

import com.chappadodle.elementalarcana.api.Relic;
import com.chappadodle.elementalarcana.api.RelicRules;
import com.chappadodle.elementalarcana.content.GlowParticleOptions;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import java.util.Locale;

/**
 * A relic (see the Relics spec): use it to bind it to yourself (the one you bound last is the one
 * that works, while it's anywhere in your inventory). Its tooltip tells its power, its stat, whether
 * it's bound, and for the Phylactery when it's ready.
 */
public class RelicItem extends Item {
    private static final int PHYLACTERY_COLOR = 0x9E8CFF;

    private final Relic relic;

    public RelicItem(Relic relic, Properties properties) {
        super(properties);
        this.relic = relic;
    }

    public Relic relic() {
        return relic;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer) {
            stack.set(ModRelics.BOND.get(), new RelicBond(player.getUUID(), server.getGameTime()));
            Relics.forget(player);
            int color = relic.element() == null ? PHYLACTERY_COLOR : relic.element().color();
            for (int i = 0; i < 24; i++) {
                double angle = Math.PI * 2 * i / 24;
                server.sendParticles(GlowParticleOptions.of(ModContent.FLARE.get(), color, color, 0.4f, 14),
                        player.getX() + Math.cos(angle) * 1.2, player.getY() + 1, player.getZ() + Math.sin(angle) * 1.2, 1, 0, 0.05, 0, 0.01);
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.8f, 1.5f);
            serverPlayer.displayClientMessage(Component.translatable("message.elementalarcana.relic.bound", stack.getHoverName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            MagicTriggers.fire(serverPlayer, "relic", relic.id(), 1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModRelics.BOND.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana." + relic.id() + ".power").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.elementalarcana.relic.stat", RelicRules.STAT_BONUS,
                Component.translatable("stat.elementalarcana." + relic.stat().key())).withStyle(ChatFormatting.BLUE));
        Long readyAt = stack.get(ModRelics.READY_AT.get());
        Level level = context.level();
        if (readyAt != null && level != null && !RelicRules.ready(readyAt, level.getGameTime())) {
            int seconds = RelicRules.secondsLeft(readyAt, level.getGameTime());
            tooltip.add(Component.translatable("item.elementalarcana.relic.recharging", seconds / 60, String.format(Locale.ROOT, "%02d", seconds % 60))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(stack.has(ModRelics.BOND.get())
                ? Component.translatable("item.elementalarcana.relic.bound").withStyle(ChatFormatting.LIGHT_PURPLE)
                : Component.translatable("item.elementalarcana.relic.unbound").withStyle(ChatFormatting.DARK_GRAY));
    }
}
