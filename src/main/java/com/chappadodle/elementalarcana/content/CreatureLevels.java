package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Progression;
import com.chappadodle.elementalarcana.api.StatRules;
import com.chappadodle.elementalarcana.api.ZoneLevels;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Every creature has a level (see ZoneLevels): set from where it is on its first tick, plus its
 * rank's bonus levels if it's Attuned. Players use their own level. A creature's stats follow the
 * shared rules (StatRules): a third of its points each in Vitality, Ward and Potency. Vitality is
 * applied to its max health here; Ward and Potency in LevelCombat.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class CreatureLevels {
    /** Biomes worth +10 levels (the deep dark). */
    public static final TagKey<Biome> DANGEROUS = TagKey.create(Registries.BIOME, ElementalArcana.id("dangerous"));
    private static final ResourceLocation VITALITY = ElementalArcana.id("creature_vitality");
    // The health bonus Attuned creatures had before ranks became bonus levels; removed on sight.
    private static final ResourceLocation LEGACY_HEALTH = ElementalArcana.id("attunement_health");

    private CreatureLevels() {
    }

    /** {@code entity}'s level: a player's own, or a creature's zone level plus its rank's bonus (assigned now if it has none yet). */
    public static int levelOf(LivingEntity entity) {
        if (entity instanceof Player player) {
            return MagicAttachments.get(player).level();
        }
        if (!entity.hasData(MagicAttachments.CREATURE_LEVEL) && entity.level() instanceof ServerLevel level) {
            assign(level, entity);
        }
        return withRank(entity, entity.getData(MagicAttachments.CREATURE_LEVEL));
    }

    /** Client-safe: a creature's level from its synced data, or 0 if not known yet (and for players). */
    public static int displayLevel(LivingEntity entity) {
        if (entity instanceof Player || !entity.hasData(MagicAttachments.CREATURE_LEVEL)) {
            return 0;
        }
        return withRank(entity, entity.getData(MagicAttachments.CREATURE_LEVEL));
    }

    private static int withRank(LivingEntity entity, int base) {
        CreatureMagic magic = entity.hasData(MagicAttachments.CREATURE_MAGIC) ? entity.getData(MagicAttachments.CREATURE_MAGIC) : null;
        return Math.clamp(base + (magic == null ? 0 : magic.rank().bonusLevels()), 1, Progression.MAX_LEVEL);
    }

    /** Sets a creature's zone level (before its rank's bonus) and updates its stats. */
    public static void setBaseLevel(LivingEntity entity, int level) {
        entity.setData(MagicAttachments.CREATURE_LEVEL, Math.clamp(level, 1, Progression.MAX_LEVEL));
        refreshStats(entity);
    }

    /** A creature's points in each of Vitality, Ward and Potency. */
    public static int creaturePoints(LivingEntity entity) {
        return StatRules.creaturePoints(levelOf(entity));
    }

    /**
     * Applies Vitality to a creature's max health, keeping its share of health. A creature without
     * a level yet is skipped: it gets one (and its stats) on its first tick. That also keeps this
     * from touching the world while a creature is still being spawned (or generated with a chunk).
     */
    public static void refreshStats(LivingEntity entity) {
        AttributeInstance health = entity.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || entity instanceof Player || !entity.hasData(MagicAttachments.CREATURE_LEVEL)) {
            return;
        }
        float share = entity.getMaxHealth() > 0 ? entity.getHealth() / entity.getMaxHealth() : 1f;
        health.removeModifier(LEGACY_HEALTH);
        health.removeModifier(VITALITY);
        double bonus = StatRules.healthMultiplier(creaturePoints(entity)) - 1;
        if (bonus > 0) {
            health.addPermanentModifier(new AttributeModifier(VITALITY, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        entity.setHealth(entity.getMaxHealth() * share);
    }

    private static void assign(ServerLevel level, LivingEntity entity) {
        BlockPos pos = entity.blockPosition();
        ZoneLevels.Dimension dimension = level.dimension() == Level.NETHER ? ZoneLevels.Dimension.NETHER
                : level.dimension() == Level.END ? ZoneLevels.Dimension.END : ZoneLevels.Dimension.OVERWORLD;
        BlockPos center = dimension == ZoneLevels.Dimension.OVERWORLD ? level.getSharedSpawnPos() : BlockPos.ZERO;
        double distance = Math.hypot(pos.getX() - center.getX(), pos.getZ() - center.getZ());
        boolean inStructure = !level.structureManager().getAllStructuresAt(pos).isEmpty();
        int zone = ZoneLevels.level(dimension, distance, pos.getY(), !level.canSeeSky(pos), inStructure,
                level.getBiome(pos).is(DANGEROUS), entity.getRandom().nextInt(ZoneLevels.SPREAD));
        entity.setData(MagicAttachments.CREATURE_LEVEL, zone);
        refreshStats(entity);
    }

    /** A creature without a level gets one on its first tick, wherever it came from (spawns, spawners, breeding, old saves). */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living && !(living instanceof Player)
                && living.level() instanceof ServerLevel level && !living.hasData(MagicAttachments.CREATURE_LEVEL)) {
            assign(level, living);
        }
    }
}
