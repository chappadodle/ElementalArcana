package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.AttunementRewards;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.api.StatRules;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Pays out for creatures killed by a player. Every creature holds mana: its killer absorbs it as XP
 * (Progression#killXp: by the creature's level, size and rank, and the level gap). Elemental
 * creatures also drop Elemental Essence of their element (see AttunementRewards), more often for a
 * killer with Insight.
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
        int xp = killXp(player, dead);
        if (xp > 0) {
            CastingService.grantXp(player, xp);
            player.displayClientMessage(Component.translatable("message.elementalarcana.magic_xp", xp)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    /** XP {@code player} absorbs from killing {@code dead}: its mana by level, size and rank, by the level gap. */
    public static int killXp(ServerPlayer player, LivingEntity dead) {
        CreatureMagic magic = Attunement.get(dead);
        AttributeInstance health = dead.getAttribute(Attributes.MAX_HEALTH);
        double size = Progression.sizeFactor(health == null ? 20 : health.getBaseValue());
        return Progression.killXp(MagicAttachments.get(player).level(), CreatureLevels.levelOf(dead), size,
                magic == null ? 1.0 : magic.rank().xpMultiplier());
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player || !(dead.level() instanceof ServerLevel level)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CreatureMagic magic = Attunement.get(dead);
        Element element = magic != null ? magic.element() : CreatureElements.innateElementOf(dead);
        if (element == null) {
            return;
        }
        AttunementRank rank = magic == null ? null : magic.rank();
        float insight = StatRules.insightFactor(MagicAttachments.get(player).stat(Stat.INSIGHT));
        int count = AttunementRewards.essenceDrops(rank, insight, dead.getRandom()::nextDouble);
        if (count > 0) {
            event.getDrops().add(new ItemEntity(level, dead.getX(), dead.getY(), dead.getZ(),
                    new ItemStack(ModItems.essence(element), count)));
        }
    }
}
