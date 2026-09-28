package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.AttunementRewards;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Pays out for elemental creatures killed by a player (see AttunementRewards): Magic XP for
 * Attuned ones (only to awakened players), and Elemental Essence of the creature's element.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class CreatureRewards {

    private CreatureRewards() {
    }

    // Lowest priority: runs only if nothing else (a totem, a shield's death save) cancelled the death.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CreatureMagic magic = Attunement.get(dead);
        int xp = AttunementRewards.magicXp(magic == null ? null : magic.rank());
        if (xp > 0 && MagicAttachments.get(player).isAwakened()) {
            CastingService.grantXp(player, xp);
            player.displayClientMessage(Component.translatable("message.elementalarcana.magic_xp", xp)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player || !(dead.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer)) {
            return;
        }
        CreatureMagic magic = Attunement.get(dead);
        Element element = magic != null ? magic.element() : CreatureElements.innateElementOf(dead);
        if (element == null) {
            return;
        }
        AttunementRank rank = magic == null ? null : magic.rank();
        int count = AttunementRewards.essenceDrops(rank, dead.getRandom()::nextDouble);
        if (count > 0) {
            event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY(), dead.getZ(),
                    new ItemStack(ModItems.essence(element), count)));
        }
    }
}
