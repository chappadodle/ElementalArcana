package com.chappadodle.elementalarcana.content.tower;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
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

/**
 * Mage towers (docs/superpowers/specs/2026-10-02-mage-towers-design.md): the structure and its
 * piece, the Tower Heart, the tower's people (Acolyte and Magister) and the Guardian Core.
 */
public final class ModTowers {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    /** Unbreakable: the tower's heart stays where it was found, lit or dark. */
    public static final DeferredBlock<TowerHeartBlock> TOWER_HEART = BLOCKS.register("tower_heart", () -> new TowerHeartBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable()
                    .lightLevel(state -> state.getValue(TowerHeartBlock.LIT) ? 15 : 6).sound(SoundType.AMETHYST).noOcclusion()));
    public static final DeferredItem<BlockItem> TOWER_HEART_ITEM = ITEMS.register("tower_heart",
            () -> new BlockItem(TOWER_HEART.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TowerHeartBlockEntity>> TOWER_HEART_ENTITY =
            BLOCK_ENTITIES.register("tower_heart", () -> BlockEntityType.Builder.of(TowerHeartBlockEntity::new, TOWER_HEART.get()).build(null));

    public static final DeferredItem<GuardianCoreItem> GUARDIAN_CORE = ITEMS.register("guardian_core",
            () -> new GuardianCoreItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    public static final DeferredHolder<EntityType<?>, EntityType<TowerMageEntity>> ACOLYTE = ENTITY_TYPES.register("tower_acolyte",
            () -> EntityType.Builder.<TowerMageEntity>of((type, level) -> new TowerMageEntity(type, level, false), MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build(ElementalArcana.MODID + ":tower_acolyte"));
    public static final DeferredHolder<EntityType<?>, EntityType<TowerMageEntity>> MAGISTER = ENTITY_TYPES.register("tower_magister",
            () -> EntityType.Builder.<TowerMageEntity>of((type, level) -> new TowerMageEntity(type, level, true), MobCategory.MONSTER)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).fireImmune().build(ElementalArcana.MODID + ":tower_magister"));
    public static final DeferredItem<DeferredSpawnEggItem> ACOLYTE_EGG = ITEMS.register("tower_acolyte_spawn_egg",
            () -> new DeferredSpawnEggItem(ACOLYTE, 0x4A2E6E, 0xC9A23E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> MAGISTER_EGG = ITEMS.register("tower_magister_spawn_egg",
            () -> new DeferredSpawnEggItem(MAGISTER, 0x1E1033, 0xF2D774, new Item.Properties()));

    public static final DeferredHolder<StructureType<?>, StructureType<MageTowerStructure>> MAGE_TOWER =
            STRUCTURE_TYPES.register("mage_tower", () -> () -> MageTowerStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> MAGE_TOWER_PIECE =
            STRUCTURE_PIECES.register("mage_tower", () -> (StructurePieceType.ContextlessType) MageTowerPiece::new);

    private ModTowers() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModTowers::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ACOLYTE.get(), TowerMageEntity.createAcolyteAttributes().build());
        event.put(MAGISTER.get(), TowerMageEntity.createMagisterAttributes().build());
    }
}
