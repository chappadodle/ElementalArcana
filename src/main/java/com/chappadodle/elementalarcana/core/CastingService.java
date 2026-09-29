package com.chappadodle.elementalarcana.core;

import com.chappadodle.elementalarcana.api.CastContext;
import com.chappadodle.elementalarcana.api.CastResult;
import com.chappadodle.elementalarcana.api.ConjureSpell;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellHold;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.event.SpellCastEvent;
import com.chappadodle.elementalarcana.content.BubblePrisons;
import com.chappadodle.elementalarcana.content.ModContent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The single casting pipeline every spell goes through. */
public final class CastingService {
    /** Health paid per point of missing mana when overcasting (1 heart per 20 mana). */
    private static final float HEALTH_PER_MISSING_MANA = 0.1f;
    private static final int MANA_SICKNESS_TICKS = 15 * 20;
    private static final long FIZZLE_SOUND_INTERVAL = 20;
    private static final Map<UUID, Long> LAST_FIZZLE = new HashMap<>();
    private static final Map<UUID, ActiveHold> HOLDS = new HashMap<>();

    private record ActiveHold(Spell spell, SpellHold hold, long startTick) {
    }

    private CastingService() {
    }

    /**
     * Why the selected spell can't be cast right now, or null if it can. Mana is not checked -
     * a shortfall is paid with health (see {@link #healthCost}). Runs on both sides.
     */
    @Nullable
    public static Component whyNotCastable(Player player, MagicData data, @Nullable Spell spell) {
        if (!data.isAwakened()) {
            return Component.translatable("message.elementalarcana.not_awakened");
        }
        if (spell == null) {
            return Component.translatable("message.elementalarcana.no_spell");
        }
        if (BubblePrisons.isTrapped(player)) {
            return Component.translatable("message.elementalarcana.bubble.trapped");
        }
        if (player.isCreative() || data.freeCast()) {
            return null;
        }
        long remaining = data.cooldownRemaining(spell.id(), player.level().getGameTime());
        if (remaining > 0) {
            return Component.translatable("message.elementalarcana.cooldown", spell.displayName(), String.format("%.1f", remaining / 20f));
        }
        return null;
    }

    /** Health an overcast of {@code manaCost} would take right now (0 when mana suffices). */
    public static float healthCost(MagicData data, int manaCost) {
        return Math.max(0f, manaCost - data.mana()) * HEALTH_PER_MISSING_MANA;
    }

    /** The cast key went down. */
    public static void tryCast(ServerPlayer player) {
        if (HOLDS.containsKey(player.getUUID())) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        Spell spell = data.selectedSpell();
        Component problem = whyNotCastable(player, data, spell);
        if (problem != null) {
            fizzle(player, problem);
            return;
        }
        if (spell instanceof ConjureSpell) {
            Conjuring.press(player);
            return;
        }

        int spellLevel = data.spellLevel(spell);
        SpellCastEvent.Pre pre = NeoForge.EVENT_BUS.post(new SpellCastEvent.Pre(player, spell, spell.manaCost(spellLevel)));
        if (pre.isCanceled()) {
            return;
        }
        int cost = pre.manaCost();
        if (!canAfford(player, data, cost)) {
            return;
        }

        CastContext context = new CastContext(player, player.serverLevel(), InteractionHand.MAIN_HAND, data.power(),
                spellLevel, data.progress(spell).branches());
        CastResult result = spell.cast(context);
        if (!result.success()) {
            if (result.failReason() != null) {
                fizzle(player, result.failReason());
            }
            return;
        }

        pay(player, data, spell, cost);
        if (context.hold() == null && !isFree(player, data)) {
            data.startCooldown(spell.id(), player.level().getGameTime(), spell.cooldownTicks(data.spellLevel(spell), data.level()));
        }
        if (context.hold() != null) {
            HOLDS.put(player.getUUID(), new ActiveHold(spell, context.hold(), player.level().getGameTime()));
        }
        castFeedback(player, spell);
        MagicAttachments.sync(player);
        NeoForge.EVENT_BUS.post(new SpellCastEvent.Post(player, spell));
    }

    /** Creative mode and the dev menu's free casting: no mana, no cooldowns. */
    static boolean isFree(ServerPlayer player, MagicData data) {
        return player.isCreative() || data.freeCast();
    }

    /**
     * Whether {@code cost} can be paid right now: mana, with health covering a shortfall, as long as
     * that leaves the caster at least half a heart. Fizzles with a message and returns false if not.
     */
    static boolean canAfford(ServerPlayer player, MagicData data, int cost) {
        float healthCost = isFree(player, data) ? 0f : healthCost(data, cost);
        if (healthCost > 0 && player.getHealth() - healthCost < 1f) {
            fizzle(player, Component.translatable("message.elementalarcana.too_exhausted"));
            return false;
        }
        return true;
    }

    /**
     * Pays for a cast: takes the mana (health for any shortfall, with Mana Sickness), then grants
     * Magic XP and mastery in {@code spell} for it. Nothing for creative or free casting.
     */
    static void pay(ServerPlayer player, MagicData data, Spell spell, int cost) {
        data.interruptMeditation();
        if (isFree(player, data)) {
            return;
        }
        float healthCost = healthCost(data, cost);
        data.setMana(data.mana() - cost);
        if (healthCost > 0) {
            overcast(player, healthCost);
        }
        if (healthCost > 0 || data.mana() <= 0f) {
            player.addEffect(new MobEffectInstance(ModContent.MANA_SICKNESS, MANA_SICKNESS_TICKS));
        }
        int oldLevel = data.level();
        if (data.addXp(cost) > 0) {
            onLevelUp(player, data, oldLevel);
        }
        boolean wasFull = data.isMasteryFull(spell);
        data.addMastery(spell, cost);
        if (!wasFull && data.isMasteryFull(spell)) {
            player.sendSystemMessage(Component.translatable("message.elementalarcana.mastery_full", spell.displayName(),
                    Component.keybind("key.elementalarcana.status")).withStyle(style -> style.withColor(spell.school().color())));
        }
    }

    /** The caster swings their arm and the spell's school sound plays. */
    static void castFeedback(ServerPlayer player, Spell spell) {
        player.swing(InteractionHand.MAIN_HAND, true);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), spell.school().castSound(),
                SoundSource.PLAYERS, 1f, 0.9f + player.getRandom().nextFloat() * 0.2f);
    }

    /** The cast key came back up: finish any hold-to-cast spell. */
    public static void release(ServerPlayer player) {
        Conjuring.releaseKey(player);
        ActiveHold active = HOLDS.remove(player.getUUID());
        if (active == null) {
            return;
        }
        active.hold().release(heldTicks(player, active));
        endHold(player, active);
    }

    /** Server tick for a player's active hold: ends it when the spell says so or time runs out. */
    public static void tickHold(ServerPlayer player) {
        ActiveHold active = HOLDS.get(player.getUUID());
        if (active == null) {
            return;
        }
        int held = heldTicks(player, active);
        if (!active.hold().tick(held) || held >= active.hold().maxHoldTicks()) {
            release(player);
        }
    }

    /** The hold broke (death, logout, dimension change): cancel it but still start the cooldown. */
    public static void cancelHold(ServerPlayer player) {
        Conjuring.cancel(player);
        ActiveHold active = HOLDS.remove(player.getUUID());
        if (active != null) {
            active.hold().cancel();
            endHold(player, active);
        }
    }

    private static int heldTicks(ServerPlayer player, ActiveHold active) {
        return (int) (player.level().getGameTime() - active.startTick());
    }

    private static void endHold(ServerPlayer player, ActiveHold active) {
        MagicData data = MagicAttachments.get(player);
        if (!player.isCreative() && !data.freeCast()) {
            data.startCooldown(active.spell().id(), player.level().getGameTime(), active.spell().cooldownTicks(data.spellLevel(active.spell()), data.level()));
            MagicAttachments.sync(player);
        }
    }

    private static void overcast(ServerPlayer player, float healthCost) {
        player.setHealth(player.getHealth() - healthCost);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 0.8f, 0.8f);
        player.serverLevel().sendParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY(1.0), player.getZ(), 4, 0.3, 0.2, 0.3, 0.1);
        player.displayClientMessage(Component.translatable("message.elementalarcana.overcast").withStyle(ChatFormatting.DARK_RED), true);
    }

    /**
     * Gives Magic XP from anything other than casting (e.g. defeating Attuned creatures), with the
     * same level-up title, sound and messages as casting XP, and syncs.
     */
    public static void grantXp(ServerPlayer player, int amount) {
        MagicData data = MagicAttachments.get(player);
        int oldLevel = data.level();
        if (data.addXp(amount) > 0) {
            onLevelUp(player, data, oldLevel);
        }
        MagicAttachments.sync(player);
    }

    private static void onLevelUp(ServerPlayer player, MagicData data, int oldLevel) {
        int newLevel = data.level();
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable("title.elementalarcana.level_up", newLevel).withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable("title.elementalarcana.level_up.sub", (int) data.maxMana()).withStyle(ChatFormatting.GRAY)));
        player.playNotifySound(ModContent.LEVEL_UP_SOUND.get(), SoundSource.PLAYERS, 1f, 1f);
        player.serverLevel().sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 30, 0.5, 0.8, 0.5, 0.05);

        for (Spell spell : SpellRegistries.SPELLS) {
            if (data.hasAffinity(spell.school()) && spell.requiredLevel() > oldLevel && spell.requiredLevel() <= newLevel) {
                player.sendSystemMessage(Component.translatable("message.elementalarcana.new_spell", spell.displayName())
                        .withStyle(style -> style.withColor(spell.school().color())));
            }
        }
        for (int slotLevel : MagicData.AFFINITY_SLOT_LEVELS) {
            if (slotLevel > oldLevel && slotLevel <= newLevel && data.hasFreeAffinitySlot()) {
                player.sendSystemMessage(Component.translatable("message.elementalarcana.new_affinity_slot",
                        Component.keybind("key.elementalarcana.status")).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    static void fizzle(ServerPlayer player, Component reason) {
        player.displayClientMessage(reason.copy().withStyle(ChatFormatting.RED), true);
        // Holding the cast key retries quickly; don't turn that into a stream of fizzles.
        long now = player.level().getGameTime();
        Long last = LAST_FIZZLE.get(player.getUUID());
        if (last == null || now - last >= FIZZLE_SOUND_INTERVAL) {
            LAST_FIZZLE.put(player.getUUID(), now);
            player.playNotifySound(ModContent.FIZZLE_SOUND.get(), SoundSource.PLAYERS, 0.6f, 1f);
        }
    }

    static void forget(ServerPlayer player) {
        cancelHold(player);
        LAST_FIZZLE.remove(player.getUUID());
    }
}
