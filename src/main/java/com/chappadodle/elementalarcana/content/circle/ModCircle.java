package com.chappadodle.elementalarcana.content.circle;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
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
 * The Circle (docs/superpowers/specs/2026-10-06-the-circle-enclave-design.md): the Enclave and its
 * piece, its heart, the Circle Mages and the Archmagister.
 */
public final class ModCircle {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    /** Unbreakable, like a tower's heart: the Enclave's heart stays where it was built. */
    public static final DeferredBlock<CircleHeartBlock> CIRCLE_HEART = BLOCKS.register("circle_heart", () -> new CircleHeartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(-1f, 3600000f).noLootTable().sound(SoundType.STONE)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CircleHeartBlockEntity>> CIRCLE_HEART_ENTITY =
            BLOCK_ENTITIES.register("circle_heart", () -> BlockEntityType.Builder.of(CircleHeartBlockEntity::new, CIRCLE_HEART.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<CircleMageEntity>> CIRCLE_MAGE = ENTITY_TYPES.register("circle_mage",
            () -> EntityType.Builder.<CircleMageEntity>of((type, level) -> new CircleMageEntity(type, level, false), MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).build(ElementalArcana.MODID + ":circle_mage"));
    public static final DeferredHolder<EntityType<?>, EntityType<CircleMageEntity>> ARCHMAGISTER = ENTITY_TYPES.register("archmagister",
            () -> EntityType.Builder.<CircleMageEntity>of((type, level) -> new CircleMageEntity(type, level, true), MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).build(ElementalArcana.MODID + ":archmagister"));
    public static final DeferredItem<DeferredSpawnEggItem> CIRCLE_MAGE_EGG = ITEMS.register("circle_mage_spawn_egg",
            () -> new DeferredSpawnEggItem(CIRCLE_MAGE, 0xE8E8EE, 0x6A8CC8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ARCHMAGISTER_EGG = ITEMS.register("archmagister_spawn_egg",
            () -> new DeferredSpawnEggItem(ARCHMAGISTER, 0xF2F2F6, 0xE2BA4A, new Item.Properties()));

    public static final DeferredHolder<StructureType<?>, StructureType<EnclaveStructure>> ENCLAVE =
            STRUCTURE_TYPES.register("enclave", () -> () -> EnclaveStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ENCLAVE_PIECE =
            STRUCTURE_PIECES.register("enclave", () -> (StructurePieceType.ContextlessType) EnclavePiece::new);

    private ModCircle() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModCircle::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CIRCLE_MAGE.get(), CircleMageEntity.createAttributes(false).build());
        event.put(ARCHMAGISTER.get(), CircleMageEntity.createAttributes(true).build());
    }

    /** For the creative tab. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(CIRCLE_MAGE_EGG, ARCHMAGISTER_EGG);
    }
}
