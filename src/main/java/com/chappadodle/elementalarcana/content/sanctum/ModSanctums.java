package com.chappadodle.elementalarcana.content.sanctum;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.SovereignRules;
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

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The Sovereigns' sanctums (docs/superpowers/specs/2026-10-03-sanctums-and-sovereigns-design.md):
 * the structure and its piece, the Seal, the four Sovereigns and their Hearts.
 */
public final class ModSanctums {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    /** Unbreakable: the seal stays where it was found, sealed, awake or restored. */
    public static final DeferredBlock<SanctumSealBlock> SANCTUM_SEAL = BLOCKS.register("sanctum_seal", () -> new SanctumSealBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable()
                    .lightLevel(state -> state.getValue(SanctumSealBlock.STATE) == SealState.SEALED ? 7 : 15).sound(SoundType.AMETHYST)));
    public static final DeferredItem<BlockItem> SANCTUM_SEAL_ITEM = ITEMS.register("sanctum_seal",
            () -> new BlockItem(SANCTUM_SEAL.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SanctumSealBlockEntity>> SANCTUM_SEAL_ENTITY =
            BLOCK_ENTITIES.register("sanctum_seal", () -> BlockEntityType.Builder.of(SanctumSealBlockEntity::new, SANCTUM_SEAL.get()).build(null));

    public static final DeferredItem<SovereignHeartItem> SOVEREIGN_HEART = ITEMS.register("sovereign_heart",
            () -> new SovereignHeartItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    private static final Map<Element, DeferredHolder<EntityType<?>, EntityType<SovereignEntity>>> SOVEREIGNS = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredItem<DeferredSpawnEggItem>> EGGS = new EnumMap<>(Element.class);

    static {
        for (Element element : SovereignRules.ELEMENTS) {
            String name = element.name().toLowerCase(Locale.ROOT) + "_sovereign";
            DeferredHolder<EntityType<?>, EntityType<SovereignEntity>> type = ENTITY_TYPES.register(name, () -> {
                EntityType.Builder<SovereignEntity> builder = EntityType.Builder.<SovereignEntity>of(
                                (entityType, level) -> new SovereignEntity(entityType, level, element), MobCategory.MONSTER)
                        .sized(1.6f, 3.0f).eyeHeight(2.1f).clientTrackingRange(10);
                if (element == Element.FIRE) {
                    builder.fireImmune();
                }
                return builder.build(ElementalArcana.MODID + ":" + name);
            });
            SOVEREIGNS.put(element, type);
            int[] colors = eggColors(element);
            EGGS.put(element, ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, colors[0], colors[1], new Item.Properties())));
        }
    }

    public static final DeferredHolder<StructureType<?>, StructureType<SanctumStructure>> SANCTUM =
            STRUCTURE_TYPES.register("sanctum", () -> () -> SanctumStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SANCTUM_PIECE =
            STRUCTURE_PIECES.register("sanctum", () -> (StructurePieceType.ContextlessType) SanctumPiece::new);

    private ModSanctums() {
    }

    private static int[] eggColors(Element element) {
        return switch (element) {
            case WATER -> new int[]{0x0C2240, 0x3F9CFF};
            case WIND -> new int[]{0xE6F2EC, 0x5FBF98};
            case EARTH -> new int[]{0x33261C, 0xB5895A};
            default -> new int[]{0x2A1410, 0xFF7A1F};
        };
    }

    public static EntityType<SovereignEntity> sovereign(Element element) {
        return SOVEREIGNS.get(element).get();
    }

    public static Item sovereignEgg(Element element) {
        return EGGS.get(element).get();
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModSanctums::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        SOVEREIGNS.values().forEach(type -> event.put(type.get(), SovereignEntity.createAttributes().build()));
    }
}
