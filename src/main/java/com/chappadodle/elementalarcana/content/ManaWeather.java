package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.ManaWeatherRules;
import com.chappadodle.elementalarcana.api.Stat;
import com.chappadodle.elementalarcana.content.hollow.ModHollow;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import com.chappadodle.elementalarcana.core.MagicData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The mana in the air, and auras (see the mana sense and weather spec). Once a second, for each
 * player: how rich the air is where they stand (rich land, a dead zone, their elements' lands or an
 * opposed element's, a tide), kept on their magic for the regeneration and the mana bar; and the
 * Pressure of any hostile creature 15 or more levels above them within 12 blocks. A player who hides their
 * aura is noticed at half the distance.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ManaWeather {
    public static final TagKey<Biome> DEAD_ZONES = TagKey.create(Registries.BIOME, ElementalArcana.id("dead_zones"));
    private static final Map<Element, TagKey<Biome>> LANDS = new EnumMap<>(Element.class);
    private static final double PRESSURE_RADIUS = 12;
    private static final int PRESSURE_TICKS = 50;

    static {
        for (Element element : Element.values()) {
            LANDS.put(element, TagKey.create(Registries.BIOME, ElementalArcana.id("attunes/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    private ManaWeather() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || (player.tickCount + player.getId()) % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        MagicData data = MagicAttachments.get(player);
        BlockPos pos = player.blockPosition();
        Holder<Biome> biome = level.getBiome(pos);
        boolean dead = biome.is(DEAD_ZONES) || level.dimension() == ModHollow.THE_HOLLOW;
        float weather = ManaWeatherRules.regenFactor(CreatureLevels.zoneLevelAt(level, pos), dead, comfort(data, biome), ManaTides.active());
        if (data.setWeather(weather)) {
            MagicAttachments.sync(player);
        }
        pressure(level, player, data);
    }

    /** +1 in the lands of one of the player's elements, -1 in the lands of an element opposed to theirs, else 0. */
    private static int comfort(MagicData data, Holder<Biome> biome) {
        boolean opposed = false;
        for (Element element : Element.values()) {
            if (!biome.is(LANDS.get(element))) {
                continue;
            }
            if (data.affinityElements().contains(element)) {
                return 1;
            }
            opposed |= data.affinityElements().stream().anyMatch(held -> held.opposes(element));
        }
        return opposed ? -1 : 0;
    }

    /** The heaviest Pressure from the creatures around: a rank per 10 levels of gap past 15, less a level per 4 Ward. */
    private static void pressure(ServerLevel level, ServerPlayer player, MagicData data) {
        int own = CreatureLevels.levelOf(player);
        int ward = data.stat(Stat.WARD);
        int ranks = 0;
        for (LivingEntity creature : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(PRESSURE_RADIUS),
                creature -> creature.isAlive() && creature instanceof Enemy)) {
            ranks = Math.max(ranks, ManaWeatherRules.pressureRanks(CreatureLevels.levelOf(creature) - own, ward));
        }
        if (ranks > 0) {
            player.addEffect(new MobEffectInstance(ModContent.PRESSURE, PRESSURE_TICKS, ranks - 1, true, true, true));
        }
    }

    /** What Pressure leaves of the player's spell power. */
    public static float powerFactor(Player player) {
        MobEffectInstance pressure = player.getEffect(ModContent.PRESSURE);
        return pressure == null ? 1f : ManaWeatherRules.pressurePower(pressure.getAmplifier() + 1);
    }

    /** What Pressure leaves of the player's mana regeneration. */
    public static float regenFactor(Player player) {
        MobEffectInstance pressure = player.getEffect(ModContent.PRESSURE);
        return pressure == null ? 1f : ManaWeatherRules.pressureRegen(pressure.getAmplifier() + 1);
    }

    /** A hidden aura: creatures notice the player at half the distance. */
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && MagicAttachments.get(player).auraHidden()) {
            event.modifyVisibility(ManaWeatherRules.HIDDEN_VISIBILITY);
        }
    }
}
