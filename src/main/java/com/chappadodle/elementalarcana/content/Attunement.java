package com.chappadodle.elementalarcana.content;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.AttunementRank;
import com.chappadodle.elementalarcana.api.AttunementRules;
import com.chappadodle.elementalarcana.api.CreatureElements;
import com.chappadodle.elementalarcana.api.CreatureMagic;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.mob.CastMobSpellGoal;
import com.chappadodle.elementalarcana.core.MagicAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Creature magic (design spec, Part 2): now and then a hostile mob spawns Attuned, with an element
 * and a rank. This class owns the attunement data and its knockback bonus (the rank's bonus levels
 * are applied by CreatureLevels). It also rolls attunement when creatures spawn, and plays the faint
 * element hints; their spells are cast by CastMobSpellGoal.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class Attunement {
    /** Entity types that can spawn Attuned. */
    public static final TagKey<EntityType<?>> CAN_ATTUNE = TagKey.create(Registries.ENTITY_TYPE, ElementalArcana.id("can_attune"));
    private static final ResourceLocation ARCHMAGE_KNOCKBACK = ElementalArcana.id("archmage_knockback");
    private static final double ARCHMAGE_KNOCKBACK_RESISTANCE = 0.75;
    private static final Map<Element, TagKey<Biome>> BIOME_TAGS = new EnumMap<>(Element.class);
    // Only these spawns roll: never spawn eggs, commands, breeding or conversions.
    private static final Set<MobSpawnType> ROLLED_SPAWNS = EnumSet.of(MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION,
            MobSpawnType.SPAWNER, MobSpawnType.STRUCTURE, MobSpawnType.PATROL);
    private static final double PLAYER_SEARCH_RADIUS = 128;
    private static final int HINT_INTERVAL_TICKS = 40;

    static {
        for (Element element : Element.values()) {
            BIOME_TAGS.put(element, TagKey.create(Registries.BIOME,
                    ElementalArcana.id("attunes/" + element.name().toLowerCase(Locale.ROOT))));
        }
    }

    private Attunement() {
    }

    /** The creature's attunement, or null if it isn't Attuned. */
    @Nullable
    public static CreatureMagic get(Entity entity) {
        return entity.hasData(MagicAttachments.CREATURE_MAGIC) ? entity.getData(MagicAttachments.CREATURE_MAGIC) : null;
    }

    /**
     * Makes {@code mob} Attuned, replacing any earlier attunement, and heals it to its new max
     * health. An innate creature can only be attuned to its own element: returns false otherwise.
     */
    public static boolean attune(Mob mob, Element element, AttunementRank rank) {
        Element innate = CreatureElements.innateElementOf(mob);
        if (innate != null && innate != element) {
            return false;
        }
        mob.setData(MagicAttachments.CREATURE_MAGIC, new CreatureMagic(element, rank));
        // The rank's bonus levels raise its Vitality (see CreatureLevels).
        CreatureLevels.refreshStats(mob);
        mob.setHealth(mob.getMaxHealth());
        boolean archmage = rank == AttunementRank.ARCHMAGE;
        setBonus(mob, Attributes.KNOCKBACK_RESISTANCE, ARCHMAGE_KNOCKBACK,
                archmage ? ARCHMAGE_KNOCKBACK_RESISTANCE : 0, AttributeModifier.Operation.ADD_VALUE);
        if (archmage) {
            mob.setPersistenceRequired();
        }
        if (!mob.level().isClientSide()) {
            ensureCastGoal(mob);
        }
        return true;
    }

    /** Makes {@code mob} normal again. Returns false if it wasn't Attuned. */
    public static boolean clear(Mob mob) {
        if (!mob.hasData(MagicAttachments.CREATURE_MAGIC)) {
            return false;
        }
        mob.removeData(MagicAttachments.CREATURE_MAGIC);
        CreatureLevels.refreshStats(mob);
        setBonus(mob, Attributes.KNOCKBACK_RESISTANCE, ARCHMAGE_KNOCKBACK, 0, AttributeModifier.Operation.ADD_VALUE);
        mob.setHealth(Math.min(mob.getHealth(), mob.getMaxHealth()));
        return true;
    }

    /** Gives an Attuned mob its spellcasting goal, once. The goal does nothing if it isn't Attuned. */
    public static void ensureCastGoal(Mob mob) {
        boolean hasGoal = mob.goalSelector.getAvailableGoals().stream().anyMatch(wrapped -> wrapped.getGoal() instanceof CastMobSpellGoal);
        if (!hasGoal) {
            mob.goalSelector.addGoal(1, new CastMobSpellGoal(mob));
        }
    }

    /** Dev menu: summons a zombie 3 blocks in front of the player and attunes it. */
    public static void spawnForTesting(ServerPlayer player, Element element, AttunementRank rank) {
        Vec3 ahead = player.position().add(Vec3.directionFromRotation(0, player.getYRot()).scale(3));
        Zombie zombie = EntityType.ZOMBIE.spawn(player.serverLevel(), BlockPos.containing(ahead), MobSpawnType.COMMAND);
        if (zombie != null) {
            attune(zombie, element, rank);
        }
    }

    /** Rolls whether a freshly spawned creature is Attuned (see AttunementRules for the odds). */
    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (!ROLLED_SPAWNS.contains(event.getSpawnType()) || !mob.getType().is(CAN_ATTUNE)) {
            return;
        }
        ServerLevel level = event.getLevel().getLevel();
        Player player = level.getNearestPlayer(event.getX(), event.getY(), event.getZ(), PLAYER_SEARCH_RADIUS,
                entity -> entity instanceof Player nearby && MagicAttachments.get(nearby).isAwakened());
        if (player == null) {
            // Nobody nearby has magic: creatures stay normal.
            return;
        }
        BlockPos worldSpawn = level.getSharedSpawnPos();
        double distance = Math.hypot(event.getX() - worldSpawn.getX(), event.getZ() - worldSpawn.getZ());
        AttunementRank rank = AttunementRules.rollRank(distance, mob.getRandom()::nextDouble);
        if (rank == null) {
            return;
        }
        Element innate = CreatureElements.innateElementOf(mob);
        Element element = innate != null ? innate : AttunementRules.pickElement(
                biomeElements(event.getLevel(), BlockPos.containing(event.getX(), event.getY(), event.getZ())), mob.getRandom()::nextDouble);
        attune(mob, element, rank);
    }

    /** Attuned mobs loaded from disk get their spellcasting goal back. */
    @SubscribeEvent
    public static void onJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && get(mob) != null) {
            ensureCastGoal(mob);
        }
    }

    /** The elements of the biome at {@code pos} (the attunes/<element> biome tags). */
    private static Set<Element> biomeElements(LevelReader level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        Set<Element> elements = EnumSet.noneOf(Element.class);
        BIOME_TAGS.forEach((element, tag) -> {
            if (biome.is(tag)) {
                elements.add(element);
            }
        });
        return elements;
    }

    /** Replaces the modifier {@code id} on {@code attribute}; an amount of 0 just removes it. Saved with the mob. */
    private static void setBonus(Mob mob, Holder<Attribute> attribute, ResourceLocation id, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0) {
            instance.addPermanentModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /** Adepts and Magi now and then give off one faint particle of their element (no label, ever). */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || (entity.tickCount + entity.getId()) % HINT_INTERVAL_TICKS != 0) {
            return;
        }
        CreatureMagic magic = get(entity);
        if (magic == null || magic.rank() == AttunementRank.ARCHMAGE) {
            return;
        }
        level.sendParticles(hintParticle(magic.element()), entity.getRandomX(0.5), entity.getRandomY(), entity.getRandomZ(0.5),
                1, 0, 0, 0, 0.01);
    }

    private static ParticleOptions hintParticle(Element element) {
        return switch (element) {
            case FIRE -> ModContent.EMBER.get();
            case WATER -> ParticleTypes.DRIPPING_WATER;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case WIND -> ModContent.WIND_STREAK.get();
        };
    }
}
