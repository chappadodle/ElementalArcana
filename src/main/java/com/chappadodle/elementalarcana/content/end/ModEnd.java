package com.chappadodle.elementalarcana.content.end;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.content.wild.WildCharmItem;
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

import java.util.List;

/**
 * The Far Isles (docs/superpowers/specs/2026-10-08-the-far-isles-design.md): the Starfallen
 * Observatory and its piece, its Star Lenses and Astral Orrery, the Stargazers, Stardust, the
 * Voidwalker's Charm and the Astral Chart.
 */
public final class ModEnd {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    /** A lens on a pillar's foot, showing one of four constellations; use it to turn it. Unbreakable, part of its observatory. */
    public static final DeferredBlock<StarLensBlock> STAR_LENS = BLOCKS.register("star_lens", () -> new StarLensBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable()
                    .sound(SoundType.AMETHYST).lightLevel(state -> 7)));
    /** The observatory's instrument: it holds the chart and opens the vault. Unbreakable. */
    public static final DeferredBlock<AstralOrreryBlock> ASTRAL_ORRERY = BLOCKS.register("astral_orrery", () -> new AstralOrreryBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1f, 3600000f).noLootTable()
                    .sound(SoundType.COPPER).lightLevel(state -> state.getValue(AstralOrreryBlock.AWAKE) ? 15 : 6).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AstralOrreryBlockEntity>> ASTRAL_ORRERY_ENTITY =
            BLOCK_ENTITIES.register("astral_orrery", () -> BlockEntityType.Builder.of(AstralOrreryBlockEntity::new, ASTRAL_ORRERY.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<StargazerEntity>> STARGAZER = ENTITY_TYPES.register("stargazer",
            () -> EntityType.Builder.of(StargazerEntity::new, MobCategory.MONSTER).sized(0.7f, 2.9f).eyeHeight(2.55f)
                    .clientTrackingRange(10).build(ElementalArcana.MODID + ":stargazer"));
    public static final DeferredItem<DeferredSpawnEggItem> STARGAZER_EGG = ITEMS.register("stargazer_spawn_egg",
            () -> new DeferredSpawnEggItem(STARGAZER, 0x1E1430, 0xE8E0FF, new Item.Properties()));

    public static final DeferredItem<Item> STARDUST = ITEMS.register("stardust", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** Carried (or in the Charm Pouch): a fall into the void carries you back to solid ground, once every five minutes (Voidwalking). */
    public static final DeferredItem<WildCharmItem> VOIDWALKER_CHARM = ITEMS.register("voidwalker_charm",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<AstralChartItem> ASTRAL_CHART = ITEMS.register("astral_chart",
            () -> new AstralChartItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> STAR_LENS_ITEM = ITEMS.register("star_lens",
            () -> new BlockItem(STAR_LENS.get(), new Item.Properties().rarity(Rarity.EPIC)));
    public static final DeferredItem<BlockItem> ASTRAL_ORRERY_ITEM = ITEMS.register("astral_orrery",
            () -> new BlockItem(ASTRAL_ORRERY.get(), new Item.Properties().rarity(Rarity.EPIC)));

    public static final DeferredHolder<StructureType<?>, StructureType<ObservatoryStructure>> OBSERVATORY =
            STRUCTURE_TYPES.register("observatory", () -> () -> ObservatoryStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> OBSERVATORY_PIECE =
            STRUCTURE_PIECES.register("observatory", () -> (StructurePieceType.ContextlessType) ObservatoryPiece::new);

    private ModEnd() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModEnd::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STARGAZER.get(), StargazerEntity.createAttributes().build());
    }

    /** For the creative tab. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(STARGAZER_EGG, STARDUST, VOIDWALKER_CHARM, ASTRAL_CHART, STAR_LENS_ITEM, ASTRAL_ORRERY_ITEM);
    }
}
