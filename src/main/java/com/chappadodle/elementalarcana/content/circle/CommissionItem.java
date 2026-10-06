package com.chappadodle.elementalarcana.content.circle;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A Circle Commission (see Commissions and the Circle spec, part 2), a sealed letter from the
 * Archmagister: its name is its task, its tooltip the progress and the pay; finished, it glints.
 * Its terms live in its {@code commission} component.
 */
public class CommissionItem extends Item {
    public CommissionItem(Properties properties) {
        super(properties);
    }

    /** "Break a Hunger Obelisk", "Slay 5 of the Hollowed": the task, worded for one deed or several. */
    public static Component task(Commission commission) {
        String key = "item.elementalarcana.circle_commission.task." + commission.task() + (commission.needed() > 1 ? ".many" : "");
        return Component.translatable(key, commission.needed());
    }

    @Override
    public Component getName(ItemStack stack) {
        Commission commission = stack.get(ModCircle.COMMISSION.get());
        return commission == null ? super.getName(stack) : Component.translatable("item.elementalarcana.circle_commission.named", task(commission));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Commission commission = stack.get(ModCircle.COMMISSION.get());
        if (commission == null) {
            return;
        }
        tooltip.add(commission.complete()
                ? Component.translatable("item.elementalarcana.circle_commission.done").withStyle(ChatFormatting.GREEN)
                : Component.translatable("item.elementalarcana.circle_commission.progress", commission.done(), commission.needed())
                        .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.elementalarcana.circle_commission.pay", commission.marks()).withStyle(ChatFormatting.GOLD));
        if (commission.complete()) {
            tooltip.add(Component.translatable("item.elementalarcana.circle_commission.hand_in").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        Commission commission = stack.get(ModCircle.COMMISSION.get());
        return commission != null && commission.complete();
    }
}
