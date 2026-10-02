package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AwakeningRules;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How a first element wakes (see AwakeningRules and the awakening spec). Until it does, a player has
 * dormant mana: each Minecraft day it may wake by itself (surely by day 16), and surviving a brush
 * with an element (fire, drowning, freezing, a hard fall, a falling block or suffocation) at 3 hearts or less may wake it too.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class AwakeningEvents {
    /** A brush counts when the hit leaves the player at this much health or less (3 hearts). */
    private static final float BRUSH_HEALTH = 6f;
    private static final int BRUSH_GAP_TICKS = 100;
    // After which game time another brush counts (burning ticks hit every second).
    private static final Map<UUID, Long> BRUSH_AT = new HashMap<>();

    private AwakeningEvents() {
    }

    /** Once a second, for players with no magic yet: starts the day count and rolls each new day. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0 || player.isSpectator()) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        if (data.isAwakened()) {
            return;
        }
        long dayTime = player.serverLevel().getDayTime();
        data.startAwakeningClock(dayTime);
        int day = data.awakeningDay(dayTime);
        if (data.awakening().lastRolledDay() < 0) {
            // First sight: record the day, don't roll yet.
            data.setLastRolledDay(day);
            MagicAttachments.sync(player);
            return;
        }
        if (day == data.awakening().lastRolledDay()) {
            return;
        }
        data.setLastRolledDay(day);
        if (player.getRandom().nextDouble() < AwakeningRules.dailyChance(day)) {
            Element element = AwakeningRules.pick(AwakeningRules.anyWeights(), player.getRandom().nextDouble());
            if (element != null) {
                Awakenings.wake(player, element, Component.translatable("message.elementalarcana.awakening.timer"));
                return;
            }
        }
        MagicAttachments.sync(player);
    }

    /** A brush: a hit that leaves a player with no magic at 3 hearts or less, from an element they survive. */
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.getHealth() > BRUSH_HEALTH) {
            return;
        }
        MagicData data = MagicAttachments.get(player);
        if (data.isAwakened()) {
            return;
        }
        String kind = kindOf(event.getSource());
        Element family = kind == null ? null : familyOf(kind);
        long now = player.level().getGameTime();
        if (family == null || now < BRUSH_AT.getOrDefault(player.getUUID(), 0L)) {
            return;
        }
        BRUSH_AT.put(player.getUUID(), now + BRUSH_GAP_TICKS);
        int day = data.awakeningDay(player.serverLevel().getDayTime());
        if (player.getRandom().nextDouble() < AwakeningRules.brushChance(day)) {
            Element element = AwakeningRules.pick(AwakeningRules.weights(family), player.getRandom().nextDouble());
            if (element != null) {
                Awakenings.wake(player, element, Component.translatable("message.elementalarcana.awakening.brush." + kind));
            }
        }
    }

    /** Which kind of brush a damage source is: fire, drowning, freezing, fall or earth (a falling block or suffocation); null for anything else. */
    @Nullable
    private static String kindOf(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return "fire";
        }
        if (source.is(DamageTypeTags.IS_DROWNING)) {
            return "drowning";
        }
        if (source.is(DamageTypeTags.IS_FREEZING)) {
            return "freezing";
        }
        if (source.is(DamageTypeTags.IS_FALL)) {
            return "fall";
        }
        if (source.is(DamageTypes.FALLING_BLOCK) || source.is(DamageTypes.FALLING_ANVIL) || source.is(DamageTypes.FALLING_STALACTITE)
                || source.is(DamageTypes.STALAGMITE) || source.is(DamageTypes.IN_WALL)) {
            return "earth";
        }
        return null;
    }

    /** The element family a kind of brush leans toward (cold is Water's family). */
    @Nullable
    private static Element familyOf(String kind) {
        return switch (kind) {
            case "fire" -> Element.FIRE;
            case "drowning", "freezing" -> Element.WATER;
            case "fall" -> Element.WIND;
            case "earth" -> Element.EARTH;
            default -> null;
        };
    }
}
