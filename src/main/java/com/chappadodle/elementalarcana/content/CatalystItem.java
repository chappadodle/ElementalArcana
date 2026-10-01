package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.api.Element;
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
 * A Catalyst of one element family (see Awakenings#useCatalyst): right-click to try to wake that
 * element in yourself. It's used up whether it works or not, unless it's refused.
 */
public class CatalystItem extends Item {
    private final Element family;

    public CatalystItem(Element family, Properties properties) {
        super(properties);
        this.family = family;
    }

    public Element family() {
        return family;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        if (Awakenings.useCatalyst(serverPlayer, family) && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId(stack)).withColor(family.color());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.elementalarcana.catalyst.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.elementalarcana.catalyst.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
