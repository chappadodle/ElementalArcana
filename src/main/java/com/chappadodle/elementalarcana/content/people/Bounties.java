package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.BountyRules;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.creature.WispEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * Arcanist Bounties (see the Bounties spec): rolling a contract's terms, counting the deeds of the
 * player who carries it (kills they land, rifts closed near them), and handing a finished one in at
 * an Arcane Lectern for its reward.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Bounties {
    private Bounties() {
    }

    /** A contract for {@code task}, its terms rolled; {@code land} is the element of the land it's offered in. */
    public static ItemStack contract(BountyRules.Task task, Element land, RandomSource random) {
        Element element = task.hasElement() ? land : null;
        Element essence = element != null ? element : land;
        String extra = switch (task) {
            case WISPS -> ElementalArcana.id("wisp_mote").toString();
            case MAGUS -> ElementalArcana.id("tome_of_insight").toString();
            case RIFT -> {
                Item catalyst = ModItems.catalyst(land.family());
                yield catalyst == null ? "" : BuiltInRegistries.ITEM.getKey(catalyst).toString();
            }
            case ARCHMAGE -> ElementalArcana.id("scroll_of_unbinding").toString();
            default -> "";
        };
        Bounty bounty = new Bounty(task.id(), element == null ? "" : element.name().toLowerCase(Locale.ROOT),
                BountyRules.count(task, random::nextDouble), 0, BountyRules.emeralds(task, random::nextDouble),
                BountyRules.essence(task), essence.name().toLowerCase(Locale.ROOT), extra, BountyRules.experience(task));
        ItemStack stack = new ItemStack(ModPeople.BOUNTY_CONTRACT.get());
        stack.set(ModPeople.BOUNTY.get(), bounty);
        return stack;
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CreatureMagic magic = Attunement.get(dead);
        boolean wisp = dead instanceof WispEntity;
        progress(player, bounty -> switch (bounty.kind()) {
            case ATTUNED -> magic != null && magic.element() == bounty.taskElement();
            case WISPS -> wisp;
            case MAGUS -> magic != null && magic.rank() == AttunementRank.MAGUS;
            case ARCHMAGE -> magic != null && magic.rank() == AttunementRank.ARCHMAGE;
            default -> false;
        });
    }

    /** A rift closed near {@code player} (RiftEntity). */
    public static void riftClosed(ServerPlayer player) {
        progress(player, bounty -> bounty.kind() == BountyRules.Task.RIFT);
    }

    /** One more deed on every unfinished contract {@code player} carries that {@code fits}; a word when one is done. */
    private static void progress(ServerPlayer player, Predicate<Bounty> fits) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            Bounty bounty = stack.get(ModPeople.BOUNTY.get());
            if (bounty == null || bounty.complete() || !fits.test(bounty)) {
                continue;
            }
            Bounty next = bounty.progressed();
            stack.set(ModPeople.BOUNTY.get(), next);
            if (next.complete()) {
                player.sendSystemMessage(Component.translatable("message.elementalarcana.bounty.done", stack.getHoverName())
                        .withStyle(style -> style.withColor(0xB070FF)));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.5f, 1.6f);
            }
        }
    }

    /**
     * Hands in {@code stack} at the lectern at {@code pos}, if it's a finished contract: its reward
     * pops out on top of the lectern. Returns whether it was handed in.
     */
    public static boolean handIn(ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        Bounty bounty = stack.get(ModPeople.BOUNTY.get());
        if (bounty == null || !bounty.complete()) {
            return false;
        }
        Vec3 top = Vec3.atCenterOf(pos).add(0, 0.7, 0);
        for (ItemStack reward : rewards(bounty)) {
            ItemEntity item = new ItemEntity(level, top.x, top.y, top.z, reward);
            item.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.1, 0.25, (level.random.nextDouble() - 0.5) * 0.1);
            level.addFreshEntity(item);
        }
        ExperienceOrb.award(level, top, bounty.experience());
        MagicTriggers.fire(player, "bounty", bounty.task(), 1);
        level.playSound(null, pos, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 1f, 1f);
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 1f);
        stack.shrink(1);
        return true;
    }

    /** What a contract pays. */
    static List<ItemStack> rewards(Bounty bounty) {
        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(new ItemStack(Items.EMERALD, bounty.emeralds()));
        Element essence = bounty.essenceElement();
        if (bounty.essence() > 0 && essence != null) {
            rewards.add(new ItemStack(ModItems.essence(essence), bounty.essence()));
        }
        if (!bounty.extra().isEmpty()) {
            Item extra = BuiltInRegistries.ITEM.get(ResourceLocation.parse(bounty.extra()));
            if (extra != Items.AIR) {
                rewards.add(new ItemStack(extra));
            }
        }
        return rewards;
    }

    @Nullable
    static Bounty of(ItemStack stack) {
        return stack.get(ModPeople.BOUNTY.get());
    }
}
