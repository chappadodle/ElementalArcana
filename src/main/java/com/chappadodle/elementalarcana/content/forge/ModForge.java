package com.chappadodle.elementalarcana.content.forge;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.WildCharmItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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

import java.util.List;

/**
 * The Ember Reaches, part 1 (docs/superpowers/specs/2026-10-06-cinder-forges-design.md): the Cinder
 * Forge and its piece, the Forge Heart, the Forgewarden, the Ember Core it leaves and the Forgefire
 * Charm made from one.
 */
public final class ModForge {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    /** Unbreakable, like a tower's heart: the forge's heart stays where it was built. */
    public static final DeferredBlock<ForgeHeartBlock> FORGE_HEART = BLOCKS.register("forge_heart", () -> new ForgeHeartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(-1f, 3600000f).noLootTable().sound(SoundType.POLISHED_DEEPSLATE)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ForgeHeartBlockEntity>> FORGE_HEART_ENTITY =
            BLOCK_ENTITIES.register("forge_heart", () -> BlockEntityType.Builder.of(ForgeHeartBlockEntity::new, FORGE_HEART.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<ForgewardenEntity>> FORGEWARDEN = ENTITY_TYPES.register("forgewarden",
            () -> EntityType.Builder.<ForgewardenEntity>of(ForgewardenEntity::new, MobCategory.MONSTER)
                    .sized(2.4f, 4.0f).fireImmune().clientTrackingRange(10).build(ElementalArcana.MODID + ":forgewarden"));
    public static final DeferredItem<DeferredSpawnEggItem> FORGEWARDEN_EGG = ITEMS.register("forgewarden_spawn_egg",
            () -> new DeferredSpawnEggItem(FORGEWARDEN, 0x2A2329, 0xFF7A1F, new Item.Properties()));

    /** The forge's fire, still pulsing: the Forgefire Charm is made from it, and it burns in a furnace as long as a bucket of lava. */
    public static final DeferredItem<Item> EMBER_CORE = ITEMS.register("ember_core",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()));
    /** Carried (or in the Charm Pouch): fire and lava harm a third less, and burning goes out twice as fast (ForgeEvents). */
    public static final DeferredItem<WildCharmItem> FORGEFIRE_CHARM = ITEMS.register("forgefire_charm",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    public static final DeferredHolder<StructureType<?>, StructureType<CinderForgeStructure>> CINDER_FORGE =
            STRUCTURE_TYPES.register("cinder_forge", () -> () -> CinderForgeStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CINDER_FORGE_PIECE =
            STRUCTURE_PIECES.register("cinder_forge", () -> (StructurePieceType.ContextlessType) CinderForgePiece::new);

    private ModForge() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModForge::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(FORGEWARDEN.get(), ForgewardenEntity.createAttributes().build());
    }

    /** For the creative tab. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(EMBER_CORE, FORGEFIRE_CHARM, FORGEWARDEN_EGG);
    }
}
