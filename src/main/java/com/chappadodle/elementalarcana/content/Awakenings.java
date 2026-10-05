package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AwakeningRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.mentor.Mentor;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Locale;

/**
 * Where an element wakes in a player (see AwakeningRules and the awakening spec): the moment, with
 * its title, sound, particles and a chat line saying what woke it, and what using a Catalyst does.
 */
public final class Awakenings {

    private Awakenings() {
    }

    @Nullable
    public static SpellSchool schoolOf(Element element) {
        return SpellRegistries.SCHOOLS.get(ElementalArcana.id(element.name().toLowerCase(Locale.ROOT)));
    }

    /**
     * Wakes {@code element} in {@code player} (nothing if they hold it or an opposed element holds it
     * back) and plays the moment. {@code cause} is the chat line saying why. Returns whether it woke.
     */
    public static boolean wake(ServerPlayer player, Element element, Component cause) {
        SpellSchool school = schoolOf(element);
        MagicData data = MagicAttachments.get(player);
        if (school == null || !data.awaken(school)) {
            return false;
        }
        moment(player, element, school, cause);
        return true;
    }

    /** Admin: wakes {@code element} whatever opposes it (for commands). */
    public static boolean force(ServerPlayer player, Element element, Component cause) {
        SpellSchool school = schoolOf(element);
        MagicData data = MagicAttachments.get(player);
        if (school == null || data.hasAffinity(school)) {
            return false;
        }
        data.forceAffinity(school);
        moment(player, element, school, cause);
        return true;
    }

    /**
     * The Heart of the Prime: wakes every element {@code player} doesn't hold yet, opposites
     * included, in one moment. Returns how many woke.
     */
    public static int wakeAll(ServerPlayer player) {
        MagicData data = MagicAttachments.get(player);
        int woke = 0;
        for (Element element : Element.values()) {
            SpellSchool school = schoolOf(element);
            if (school != null && !data.hasAffinity(school)) {
                data.forceAffinity(school);
                dust(player, element, 12);
                woke++;
            }
        }
        if (data.selectedSpell() == null) {
            data.castableSpells().stream().findFirst().ifPresent(spell -> data.select(spell.id()));
        }
        MagicAttachments.sync(player);
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.elementalarcana.prime")
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.elementalarcana.prime.sub")));
        player.playNotifySound(ModContent.AWAKEN_SOUND.get(), SoundSource.PLAYERS, 1f, 0.8f);
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 80, 0.5, 1, 0.5, 0.4);
        return woke;
    }

    private static void moment(ServerPlayer player, Element element, SpellSchool school, Component cause) {
        MagicData data = MagicAttachments.get(player);
        if (data.selectedSpell() == null) {
            data.castableSpells().stream().findFirst().ifPresent(spell -> data.select(spell.id()));
        }
        MagicAttachments.sync(player);
        Component name = school.displayName().copy().withStyle(style -> style.withColor(school.color()));
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.elementalarcana.awakened")
                .withStyle(style -> style.withColor(school.color()))));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("title.elementalarcana.awakened.sub", name)));
        player.sendSystemMessage(cause.copy().withStyle(ChatFormatting.GRAY));
        player.playNotifySound(ModContent.AWAKEN_SOUND.get(), SoundSource.PLAYERS, 1f, 1f);
        player.serverLevel().sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
        dust(player, element, 24);
        if (!data.awakening().journalGiven()) {
            // The first time: a journal to make sense of it all.
            data.awakening().setJournalGiven(true);
            ItemStack journal = new ItemStack(ModItems.JOURNAL.get());
            if (!player.getInventory().add(journal)) {
                player.drop(journal, false);
            }
            player.sendSystemMessage(Component.translatable("message.elementalarcana.journal_given").withStyle(ChatFormatting.LIGHT_PURPLE));
            MagicAttachments.sync(player);
        }
        // And a stone with a voice in it (see The Voice in the Stone spec).
        Mentor.giveStone(player);
    }

    /** A puff of the element's colour around the player. */
    public static void dust(ServerPlayer player, Element element, int count) {
        int color = element.color();
        DustParticleOptions dust = new DustParticleOptions(
                new Vector3f((color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f), 1.3f);
        player.serverLevel().sendParticles(dust, player.getX(), player.getY(1.0), player.getZ(), count, 0.4, 0.6, 0.4, 0.05);
    }

    /**
     * Uses a Catalyst of {@code family}: refused (and not used up) if you have no magic yet, already
     * hold that family, or an opposed element holds it back; otherwise it's used up and rolls
     * AwakeningRules#catalystChance. Returns whether it was used up.
     */
    public static boolean useCatalyst(ServerPlayer player, Element family) {
        MagicData data = MagicAttachments.get(player);
        SpellSchool school = schoolOf(family);
        if (school == null) {
            return false;
        }
        if (!data.isAwakened()) {
            refuse(player, "awakening.catalyst.dormant");
            return false;
        }
        if (data.holdsFamily(family)) {
            refuse(player, "awakening.catalyst.held");
            return false;
        }
        Element opposed = data.opposedBy(school);
        if (opposed != null) {
            player.displayClientMessage(Component.translatable("message.elementalarcana.awakening.catalyst.opposed",
                    Component.translatable("school.elementalarcana." + opposed.name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.RED), true);
            return false;
        }
        int held = data.familiesHeld();
        double chance = AwakeningRules.catalystChance(held, data.level(), data.awakening().failures(held));
        if (player.getRandom().nextDouble() < chance) {
            data.resetCatalystFailures();
            wake(player, family, Component.translatable("message.elementalarcana.awakening.catalyst.success"));
        } else {
            data.addCatalystFailure();
            dust(player, family, 20);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1f, 0.7f);
            player.displayClientMessage(Component.translatable("message.elementalarcana.awakening.catalyst.fail").withStyle(ChatFormatting.GRAY), true);
            MagicAttachments.sync(player);
        }
        return true;
    }

    private static void refuse(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.elementalarcana." + key).withStyle(ChatFormatting.RED), true);
    }
}
