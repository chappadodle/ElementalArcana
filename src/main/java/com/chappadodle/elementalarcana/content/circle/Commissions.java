package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.CircleTalk;
import com.chappadodle.elementalarcana.api.CommissionRules;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.content.Attunement;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.crypt.RevenantEntity;
import com.chappadodle.elementalarcana.content.hollowed.HollowHerald;
import com.chappadodle.elementalarcana.content.hollowed.HollowedEntity;
import com.chappadodle.elementalarcana.content.sanctum.SovereignEntity;
import com.chappadodle.elementalarcana.content.tower.TowerMageEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.function.Predicate;

/**
 * The Archmagister's commissions (see the Circle spec, part 2): the deeds counted on the letters a
 * player carries (as bounties are: a kill when the player or their spell lands the killing blow, an
 * obelisk when they break it, a rift for everyone near it when it closes), and the letters handed
 * out and in at the Archmagister.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Commissions {
    /** The Circle's colour in chat: gold. */
    private static final int GOLD = 0xE2BA4A;

    private Commissions() {
    }

    /** A letter with {@code commission}'s terms. */
    public static ItemStack letter(Commission commission) {
        ItemStack stack = new ItemStack(ModCircle.CIRCLE_COMMISSION.get());
        stack.set(ModCircle.COMMISSION.get(), commission);
        return stack;
    }

    // Lowest priority: only a death that really happened (a boss changing form cancels its own).
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CreatureMagic magic = Attunement.get(dead);
        progress(player, task -> switch (task) {
            case HOLLOWED -> dead instanceof HollowedEntity;
            case ATTUNED -> magic != null;
            case MAGUS -> magic != null && magic.rank() == AttunementRank.MAGUS;
            case ARCHMAGE -> magic != null && magic.rank() == AttunementRank.ARCHMAGE;
            case HERALD -> dead instanceof HollowHerald;
            case MAGISTER -> dead instanceof TowerMageEntity mage && mage.isMagister();
            case REVENANT -> dead instanceof RevenantEntity;
            case SOVEREIGN -> dead instanceof SovereignEntity;
            default -> false;
        });
    }

    /** A rift closed near {@code player} (RiftEntity). */
    public static void riftClosed(ServerPlayer player) {
        progress(player, task -> task == CommissionRules.Task.RIFT);
    }

    /** {@code player} broke a Hunger Obelisk (HungerObeliskBlock). */
    public static void obeliskBroken(ServerPlayer player) {
        progress(player, task -> task == CommissionRules.Task.OBELISK);
    }

    /** One more deed on every unfinished commission {@code player} carries whose task {@code fits}; a word when one is done. */
    private static void progress(ServerPlayer player, Predicate<CommissionRules.Task> fits) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            Commission commission = stack.get(ModCircle.COMMISSION.get());
            if (commission == null || commission.complete() || !fits.test(commission.kind())) {
                continue;
            }
            Commission next = commission.progressed();
            stack.set(ModCircle.COMMISSION.get(), next);
            if (next.complete()) {
                player.sendSystemMessage(Component.translatable("message.elementalarcana.commission.done", CommissionItem.task(next))
                        .withStyle(style -> style.withColor(GOLD)));
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP,
                        SoundSource.PLAYERS, 0.5f, 1.4f);
            }
        }
    }

    /** The first commission {@code player} carries that {@code wanted} accepts, or an empty stack. */
    private static ItemStack carried(Player player, Predicate<Commission> wanted) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            Commission commission = stack.get(ModCircle.COMMISSION.get());
            if (commission != null && wanted.test(commission)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * A finished commission {@code player} carries, taken by the Archmagister: its marks and its
     * experience paid, with thanks. Returns whether there was one.
     */
    public static boolean handIn(CircleMageEntity archmagister, ServerPlayer player) {
        ItemStack stack = carried(player, Commission::complete);
        Commission commission = stack.get(ModCircle.COMMISSION.get());
        if (commission == null) {
            return false;
        }
        stack.shrink(1);
        ItemStack marks = new ItemStack(ModCircle.MARK.get(), commission.marks());
        if (!player.getInventory().add(marks)) {
            player.drop(marks, false);
        }
        ServerLevel level = player.serverLevel();
        ExperienceOrb.award(level, player.position(), CommissionRules.experience(commission.marks()));
        archmagister.say(player, Component.translatable("circle.elementalarcana.archmagister.thanks", commission.marks()));
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, archmagister.getX(), archmagister.getY() + 1.8, archmagister.getZ(),
                8, 0.4, 0.3, 0.4, 0);
        level.playSound(null, archmagister.getX(), archmagister.getY(), archmagister.getZ(), SoundEvents.VILLAGER_YES,
                SoundSource.NEUTRAL, 1f, 0.8f);
        MagicTriggers.fire(player, "commission", null, 1);
        return true;
    }

    /**
     * After the Archmagister's greeting: a word on the commission {@code player} carries, or, if they
     * carry none and their magic is awake ({@code stage}), a new one.
     */
    public static void offerOrRemind(CircleMageEntity archmagister, ServerPlayer player, CircleTalk.Stage stage) {
        Commission carried = carried(player, commission -> true).get(ModCircle.COMMISSION.get());
        if (carried != null) {
            archmagister.say(player, Component.translatable("circle.elementalarcana.archmagister.reminder", CommissionItem.task(carried)));
            return;
        }
        CommissionRules.Offer offer = CommissionRules.roll(stage, player.getRandom().nextDouble());
        if (offer == null) {
            return;
        }
        Commission commission = Commission.of(offer);
        ItemStack letter = letter(commission);
        if (!player.getInventory().add(letter)) {
            player.drop(letter, false);
        }
        archmagister.say(player, Component.translatable("circle.elementalarcana.archmagister.commission", CommissionItem.task(commission),
                commission.marks()));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
    }
}
