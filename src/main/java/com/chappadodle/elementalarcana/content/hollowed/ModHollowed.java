package com.chappadodle.elementalarcana.content.hollowed;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.WildCharmItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Hollowed (docs/superpowers/specs/2026-10-06-the-hollowed-design.md): the three of them and the
 * Hunger Bolt, the Hunger Obelisk, their camps, the Hollow Shard and the Hungerward Charm made from
 * it, and the hunger their attacks do (a damage type, data).
 */
public final class ModHollowed {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    /** Hunger: the Hollowed's harm (armour doesn't stop it). */
    public static final ResourceKey<DamageType> HOLLOWED_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, ElementalArcana.id("hollowed"));

    public static final DeferredHolder<EntityType<?>, EntityType<HollowedAcolyte>> ACOLYTE = ENTITY_TYPES.register("hollowed_acolyte",
            () -> EntityType.Builder.<HollowedAcolyte>of(HollowedAcolyte::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ElementalArcana.MODID + ":hollowed_acolyte"));
    public static final DeferredHolder<EntityType<?>, EntityType<HollowedDevourer>> DEVOURER = ENTITY_TYPES.register("hollowed_devourer",
            () -> EntityType.Builder.<HollowedDevourer>of(HollowedDevourer::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ElementalArcana.MODID + ":hollowed_devourer"));
    public static final DeferredHolder<EntityType<?>, EntityType<HollowHerald>> HERALD = ENTITY_TYPES.register("hollow_herald",
            () -> EntityType.Builder.<HollowHerald>of(HollowHerald::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10)
                    .build(ElementalArcana.MODID + ":hollow_herald"));
    public static final DeferredHolder<EntityType<?>, EntityType<HungerBolt>> HUNGER_BOLT = ENTITY_TYPES.register("hunger_bolt",
            () -> EntityType.Builder.<HungerBolt>of(HungerBolt::new, MobCategory.MISC).sized(0.3125f, 0.3125f).clientTrackingRange(4)
                    .updateInterval(2).build(ElementalArcana.MODID + ":hunger_bolt"));

    public static final DeferredBlock<HungerObeliskBlock> OBELISK = BLOCKS.register("hunger_obelisk", () -> new HungerObeliskBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(50f, 1200f).requiresCorrectToolForDrops()
                    .lightLevel(state -> 9).sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HungerObeliskBlockEntity>> OBELISK_ENTITY =
            BLOCK_ENTITIES.register("hunger_obelisk", () -> BlockEntityType.Builder.of(HungerObeliskBlockEntity::new, OBELISK.get()).build(null));

    public static final DeferredItem<Item> HOLLOW_SHARD = ITEMS.register("hollow_shard", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** Carried, it halves what the Hollowed eat (see Hungerward); its tooltip says so. */
    public static final DeferredItem<WildCharmItem> HUNGERWARD = ITEMS.register("hungerward_charm",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    /** What a Hunger Bolt is drawn as (no use of its own; not in the creative tab). */
    public static final DeferredItem<Item> HUNGER_ORB = ITEMS.register("hunger_orb", () -> new Item(new Item.Properties()));
    public static final DeferredItem<BlockItem> OBELISK_ITEM = ITEMS.register("hunger_obelisk",
            () -> new BlockItem(OBELISK.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> ACOLYTE_EGG = ITEMS.register("hollowed_acolyte_spawn_egg",
            () -> new DeferredSpawnEggItem(ACOLYTE, 0x1A1420, 0x8A4CD8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> DEVOURER_EGG = ITEMS.register("hollowed_devourer_spawn_egg",
            () -> new DeferredSpawnEggItem(DEVOURER, 0x2A2430, 0x5A2A8A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> HERALD_EGG = ITEMS.register("hollow_herald_spawn_egg",
            () -> new DeferredSpawnEggItem(HERALD, 0x0E0A14, 0xC890FF, new Item.Properties()));

    public static final DeferredHolder<StructureType<?>, StructureType<HollowedCampStructure>> CAMP =
            STRUCTURE_TYPES.register("hollowed_camp", () -> () -> HollowedCampStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CAMP_PIECE =
            STRUCTURE_PIECES.register("hollowed_camp", () -> (StructurePieceType.ContextlessType) HollowedCampPiece::new);

    private ModHollowed() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModHollowed::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ACOLYTE.get(), HollowedAcolyte.createAttributes().build());
        event.put(DEVOURER.get(), HollowedDevourer.createAttributes().build());
        event.put(HERALD.get(), HollowHerald.createAttributes().build());
    }

    /** Hunger's harm, done by {@code direct} for {@code owner}. */
    public static DamageSource hunger(ServerLevel level, Entity direct, @Nullable Entity owner) {
        return level.damageSources().source(HOLLOWED_DAMAGE, direct, owner);
    }

    /** For the creative tab. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(HOLLOW_SHARD, HUNGERWARD, OBELISK_ITEM, ACOLYTE_EGG, DEVOURER_EGG, HERALD_EGG);
    }
}
