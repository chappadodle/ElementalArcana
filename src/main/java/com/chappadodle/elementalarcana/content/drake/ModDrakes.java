package com.chappadodle.elementalarcana.content.drake;

import net.minecraft.world.item.BlockItem;
import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.DrakeRules;
import com.chappadodle.elementalarcana.api.Element;
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
import net.minecraft.world.level.material.PushReaction;
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
 * Elemental Drakes (docs/superpowers/specs/2026-10-04-drakes-design.md): a drake of each of five
 * elements, their scales and charms, and their nests.
 */
public final class ModDrakes {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    private static final Map<Element, DeferredHolder<EntityType<?>, EntityType<DrakeEntity>>> DRAKES = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredItem<DeferredSpawnEggItem>> EGGS = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredItem<Item>> SCALES = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredItem<DrakescaleCharmItem>> CHARMS = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredHolder<EntityType<?>, EntityType<TamedDrakeEntity>>> TAMED = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredBlock<DrakeEggBlock>> EGG_BLOCKS = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredItem<BlockItem>> EGG_ITEMS = new EnumMap<>(Element.class);

    /** Put on a grown drake you raised, to ride it. */
    public static final DeferredItem<Item> SADDLE = ITEMS.register("drake_saddle", () -> new Item(new Item.Properties().stacksTo(1)));

    /** Unseen in a nest: it calls the nest's drake when someone first comes near, then is gone. */
    public static final DeferredBlock<NestHeartBlock> NEST_HEART = BLOCKS.register("nest_heart", () -> new NestHeartBlock(
            BlockBehaviour.Properties.of().strength(-1f, 3600000f).noLootTable().noCollission().noOcclusion()
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.STONE)));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NestHeartBlockEntity>> NEST_HEART_ENTITY =
            BLOCK_ENTITIES.register("nest_heart", () -> BlockEntityType.Builder.of(NestHeartBlockEntity::new, NEST_HEART.get()).build(null));

    public static final DeferredHolder<StructureType<?>, StructureType<NestStructure>> NEST =
            STRUCTURE_TYPES.register("drake_nest", () -> () -> NestStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> NEST_PIECE =
            STRUCTURE_PIECES.register("drake_nest", () -> (StructurePieceType.ContextlessType) NestPiece::new);

    static {
        for (Element element : DrakeRules.ELEMENTS) {
            String name = element.name().toLowerCase(Locale.ROOT);
            DeferredHolder<EntityType<?>, EntityType<DrakeEntity>> type = ENTITY_TYPES.register(name + "_drake", () -> {
                EntityType.Builder<DrakeEntity> builder = EntityType.Builder.<DrakeEntity>of((t, level) -> new DrakeEntity(t, level, element),
                        MobCategory.MONSTER).sized(3.0f, 2.0f).eyeHeight(1.6f).clientTrackingRange(10).updateInterval(2);
                if (element == Element.FIRE) {
                    builder.fireImmune();
                }
                return builder.build(ElementalArcana.MODID + ":" + name + "_drake");
            });
            DRAKES.put(element, type);
            EGGS.put(element, ITEMS.register(name + "_drake_spawn_egg",
                    () -> new DeferredSpawnEggItem(type, shellColor(element), element.color(), new Item.Properties())));
            SCALES.put(element, ITEMS.register(name + "_drake_scale", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))));
            CHARMS.put(element, ITEMS.register(name + "_drakescale_charm",
                    () -> new DrakescaleCharmItem(element, new Item.Properties().stacksTo(1).rarity(Rarity.RARE))));
            TAMED.put(element, ENTITY_TYPES.register("tamed_" + name + "_drake", () -> {
                EntityType.Builder<TamedDrakeEntity> builder = EntityType.Builder.<TamedDrakeEntity>of((t, level) -> new TamedDrakeEntity(t, level, element),
                        MobCategory.CREATURE).sized(3.0f, 2.0f).eyeHeight(1.6f).clientTrackingRange(10).updateInterval(2);
                if (element == Element.FIRE) {
                    builder.fireImmune();
                }
                return builder.build(ElementalArcana.MODID + ":tamed_" + name + "_drake");
            }));
            DeferredBlock<DrakeEggBlock> egg = BLOCKS.register(name + "_drake_egg", () -> new DrakeEggBlock(element,
                    BlockBehaviour.Properties.of().strength(2f, 6f).sound(SoundType.METAL).noOcclusion().randomTicks()
                            .lightLevel(state -> 3)));
            EGG_BLOCKS.put(element, egg);
            EGG_ITEMS.put(element, ITEMS.register(name + "_drake_egg", () -> new BlockItem(egg.get(), new Item.Properties().stacksTo(1)
                    .rarity(Rarity.EPIC))));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DrakeEggBlockEntity>> EGG_ENTITY =
            BLOCK_ENTITIES.register("drake_egg", () -> BlockEntityType.Builder.of(DrakeEggBlockEntity::new,
                    EGG_BLOCKS.values().stream().map(DeferredBlock::get).toArray(net.minecraft.world.level.block.Block[]::new)).build(null));

    private ModDrakes() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModDrakes::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        DRAKES.values().forEach(type -> event.put(type.get(), DrakeEntity.createAttributes().build()));
        TAMED.values().forEach(type -> event.put(type.get(), TamedDrakeEntity.createAttributes().build()));
    }

    public static EntityType<DrakeEntity> drake(Element element) {
        return DRAKES.get(element).get();
    }

    public static Item egg(Element element) {
        return EGGS.get(element).get();
    }

    public static Item scale(Element element) {
        return SCALES.get(element).get();
    }

    public static Item charm(Element element) {
        return CHARMS.get(element).get();
    }

    public static EntityType<TamedDrakeEntity> tamedDrake(Element element) {
        return TAMED.get(element).get();
    }

    public static Item eggItem(Element element) {
        return EGG_ITEMS.get(element).get();
    }

    /** A drake egg's shell: its scales' darkest. */
    private static int shellColor(Element element) {
        return switch (element) {
            case FIRE -> 0x781E14;
            case ICE -> 0x5C82A8;
            case LIGHTNING -> 0x32323F;
            case WATER -> 0x1E6482;
            default -> 0x4E8C78;
        };
    }
}
