package com.chappadodle.elementalarcana.content.mentor;

import com.chappadodle.elementalarcana.core.ArcanaServerConfig;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.MentorChapters;
import com.chappadodle.elementalarcana.api.SchoolElements;
import com.chappadodle.elementalarcana.content.MagicTriggers;
import com.chappadodle.elementalarcana.content.ModItems;
import com.chappadodle.elementalarcana.content.pouch.CharmPouches;
import com.chappadodle.elementalarcana.core.CastingService;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import com.chappadodle.elementalarcana.network.MentorPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Caelith's tale on the server (see The Voice in the Stone spec): listening through the stone (the
 * chapter sent to the player's screen), taking a chapter's reward once its task is done, the stone
 * growing warm when it is, and the stone itself, given once to every awakened mage.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Mentor {

    private Mentor() {
    }

    /** Opens the stone: the player's chapter, whether its task is done, and what it's worth. */
    public static void listen(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload(player, player.getData(ModMentor.TALE).chapter()));
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    private static MentorPayload payload(ServerPlayer player, int index) {
        MentorChapters.Chapter chapter = MentorChapters.at(index);
        if (chapter == null) {
            return new MentorPayload(index, false, List.of(), 0);
        }
        return new MentorPayload(index, done(player, chapter), rewards(player, chapter),
                MentorChapters.xp(chapter, MagicAttachments.get(player).level()));
    }

    /** Whether the chapter's task is done (its advancement made; the first chapter's always is). */
    static boolean done(ServerPlayer player, MentorChapters.Chapter chapter) {
        if (chapter.advancement() == null) {
            return true;
        }
        AdvancementHolder advancement = player.server.getAdvancements().get(ResourceLocation.parse(chapter.advancement()));
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    private static List<ItemStack> rewards(ServerPlayer player, MentorChapters.Chapter chapter) {
        List<ItemStack> stacks = new ArrayList<>();
        for (MentorChapters.Reward reward : chapter.rewards()) {
            if (reward.item().equals(MentorChapters.OWN_ESSENCE)) {
                stacks.add(new ItemStack(ModItems.essence(ownElement(player)), reward.count()));
            } else {
                stacks.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(reward.item())), reward.count()));
            }
        }
        return stacks;
    }

    /** The player's first element (Fire, for a mage somehow without one). */
    private static Element ownElement(ServerPlayer player) {
        for (ResourceLocation school : MagicAttachments.get(player).affinities()) {
            Element element = SchoolElements.of(school);
            if (element != null) {
                return element;
            }
        }
        return Element.FIRE;
    }

    /** Takes chapter {@code index}'s reward (if it's the player's chapter and its task is done), and moves on to the next. */
    public static void claim(ServerPlayer player, int index) {
        ModMentor.Tale tale = player.getData(ModMentor.TALE);
        MentorChapters.Chapter chapter = MentorChapters.at(tale.chapter());
        if (chapter == null || tale.chapter() != index || !done(player, chapter)) {
            return;
        }
        int xp = MentorChapters.xp(chapter, MagicAttachments.get(player).level());
        for (ItemStack stack : rewards(player, chapter)) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        player.setData(ModMentor.TALE, new ModMentor.Tale(tale.chapter() + 1, tale.stoneGiven()));
        if (xp > 0) {
            CastingService.grantXp(player, xp);
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 1f, 1.1f);
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS, 0.5f, 1.4f);
        MagicTriggers.fire(player, "mentor", chapter.id(), tale.chapter() + 1);
        PacketDistributor.sendToPlayer(player, payload(player, tale.chapter() + 1));
    }

    /** Sets a player's chapter (the dev command). */
    public static void setChapter(ServerPlayer player, int index) {
        ModMentor.Tale tale = player.getData(ModMentor.TALE);
        player.setData(ModMentor.TALE, new ModMentor.Tale(index, tale.stoneGiven()));
    }

    /** Gives the player their Sending Stone, once ever (unless the server's settings say not to). */
    public static void giveStone(ServerPlayer player) {
        ModMentor.Tale tale = player.getData(ModMentor.TALE);
        if (tale.stoneGiven() || !ArcanaServerConfig.SENDING_STONE.get()) {
            return;
        }
        player.setData(ModMentor.TALE, new ModMentor.Tale(tale.chapter(), true));
        ItemStack stone = new ItemStack(ModMentor.SENDING_STONE.get());
        if (!player.getInventory().add(stone)) {
            player.drop(stone, false);
        }
        player.sendSystemMessage(Component.translatable("message.elementalarcana.mentor.given").withStyle(ChatFormatting.LIGHT_PURPLE));
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
    }

    /** A mage awakened before the stone existed gets theirs when they next join. */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagicData data = MagicAttachments.get(player);
            if (data.isAwakened()) {
                giveStone(player);
            }
        }
    }

    /** The task of the chapter a player is on, done: the stone grows warm (if they carry it). */
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MentorChapters.Chapter chapter = MentorChapters.at(player.getData(ModMentor.TALE).chapter());
        if (chapter == null || chapter.advancement() == null || !chapter.advancement().equals(event.getAdvancement().id().toString())
                || !CharmPouches.carries(player, stack -> stack.is(ModMentor.SENDING_STONE))) {
            return;
        }
        player.sendSystemMessage(Component.translatable("message.elementalarcana.mentor.warm").withStyle(ChatFormatting.LIGHT_PURPLE));
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.3f);
    }
}
