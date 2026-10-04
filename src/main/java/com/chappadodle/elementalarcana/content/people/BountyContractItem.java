package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * A Bounty Contract (see Bounties and the Bounties spec): its name is its task, its tooltip the
 * progress and the reward; finished, it glints. Its terms live in its {@code bounty} component.
 */
public class BountyContractItem extends Item {
    public BountyContractItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Bounty bounty = stack.get(ModPeople.BOUNTY.get());
        if (bounty == null) {
            return super.getName(stack);
        }
        Element element = bounty.taskElement();
        return element == null
                ? Component.translatable("item.elementalarcana.bounty_contract.task." + bounty.task())
                : Component.translatable("item.elementalarcana.bounty_contract.task." + bounty.task(), elementName(element));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Bounty bounty = stack.get(ModPeople.BOUNTY.get());
        if (bounty == null) {
            return;
        }
        tooltip.add(bounty.complete()
                ? Component.translatable("item.elementalarcana.bounty_contract.done").withStyle(ChatFormatting.GREEN)
                : Component.translatable("item.elementalarcana.bounty_contract.progress", bounty.done(), bounty.needed()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.elementalarcana.bounty_contract.reward").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  " + bounty.emeralds() + " ").append(Component.translatable("item.minecraft.emerald"))
                .withStyle(ChatFormatting.GRAY));
        Element essence = bounty.essenceElement();
        if (bounty.essence() > 0 && essence != null) {
            tooltip.add(Component.literal("  " + bounty.essence() + " ")
                    .append(Component.translatable("item.elementalarcana." + essence.name().toLowerCase(Locale.ROOT) + "_essence"))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (!bounty.extra().isEmpty()) {
            Item extra = BuiltInRegistries.ITEM.get(ResourceLocation.parse(bounty.extra()));
            tooltip.add(Component.literal("  1 ").append(extra.getDescription()).withStyle(ChatFormatting.GRAY));
        }
        if (bounty.complete()) {
            tooltip.add(Component.translatable("item.elementalarcana.bounty_contract.hand_in").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        Bounty bounty = stack.get(ModPeople.BOUNTY.get());
        return bounty != null && bounty.complete();
    }

    static Component elementName(Element element) {
        return Component.translatable("school.elementalarcana." + element.name().toLowerCase(Locale.ROOT));
    }
}
