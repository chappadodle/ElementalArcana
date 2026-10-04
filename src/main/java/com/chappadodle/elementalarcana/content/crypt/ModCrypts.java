package com.chappadodle.elementalarcana.content.crypt;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Arcane Crypts (docs/superpowers/specs/2026-10-04-arcane-crypts-design.md): the structure and its
 * pieces, the gate's blocks (runestones, keystone, seal), glyphs, coffins, the grave flame and the
 * Revenant.
 */
public final class ModCrypts {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    public static final DeferredBlock<RunestoneBlock> RUNESTONE = BLOCKS.register("runestone", () -> new RunestoneBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(-1f, 3600000f).noLootTable()
                    .lightLevel(state -> state.getValue(RunestoneBlock.LIT) ? 10 : 1).sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredBlock<RuneLockBlock> RUNE_LOCK = BLOCKS.register("rune_lock", () -> new RuneLockBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(-1f, 3600000f).noLootTable()
                    .lightLevel(state -> state.getValue(RuneLockBlock.OPEN) ? 12 : 1 + state.getValue(RuneLockBlock.PROGRESS) * 3)
                    .sound(SoundType.DEEPSLATE_BRICKS)));
    public static final DeferredBlock<RunicSealBlock> RUNIC_SEAL = BLOCKS.register("runic_seal", () -> new RunicSealBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).noLootTable().lightLevel(state -> 7)
                    .noOcclusion().isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                    .isValidSpawn((state, level, pos, type) -> false).pushReaction(PushReaction.BLOCK).sound(SoundType.AMETHYST)));
    public static final DeferredBlock<GlyphBlock> GLYPH = BLOCKS.register("glyph", () -> new GlyphBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(1.5f, 6f).noLootTable().noCollission()
                    .lightLevel(state -> switch (state.getValue(GlyphBlock.STAGE)) {
                        case ARMED -> 2;
                        case FLARING -> 12;
                        case RESTING -> 0;
                    }).pushReaction(PushReaction.DESTROY).sound(SoundType.DEEPSLATE_TILES)));
    public static final DeferredBlock<CoffinBlock> COFFIN = BLOCKS.register("coffin", () -> new CoffinBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(3f, 6f).noLootTable().noOcclusion()
                    .pushReaction(PushReaction.BLOCK).sound(SoundType.POLISHED_DEEPSLATE)));
    public static final DeferredBlock<GraveFlameBlock> GRAVE_FLAME = BLOCKS.register("grave_flame", () -> new GraveFlameBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE).strength(-1f, 3600000f).noLootTable().noOcclusion()
                    .lightLevel(state -> state.getValue(GraveFlameBlock.LIT) ? 15 : 2).sound(SoundType.POLISHED_DEEPSLATE)));

    public static final DeferredItem<BlockItem> RUNESTONE_ITEM = ITEMS.register("runestone", () -> new BlockItem(RUNESTONE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RUNE_LOCK_ITEM = ITEMS.register("rune_lock", () -> new BlockItem(RUNE_LOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RUNIC_SEAL_ITEM = ITEMS.register("runic_seal", () -> new BlockItem(RUNIC_SEAL.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GLYPH_ITEM = ITEMS.register("glyph", () -> new BlockItem(GLYPH.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> COFFIN_ITEM = ITEMS.register("coffin", () -> new DoubleHighBlockItem(COFFIN.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GRAVE_FLAME_ITEM = ITEMS.register("grave_flame",
            () -> new BlockItem(GRAVE_FLAME.get(), new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoffinBlockEntity>> COFFIN_ENTITY =
            BLOCK_ENTITIES.register("coffin", () -> BlockEntityType.Builder.of(CoffinBlockEntity::new, COFFIN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GraveFlameBlockEntity>> GRAVE_FLAME_ENTITY =
            BLOCK_ENTITIES.register("grave_flame", () -> BlockEntityType.Builder.of(GraveFlameBlockEntity::new, GRAVE_FLAME.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<RevenantEntity>> REVENANT = ENTITY_TYPES.register("revenant",
            () -> EntityType.Builder.<RevenantEntity>of(RevenantEntity::new, MobCategory.MONSTER)
                    .sized(0.6f, 1.99f).eyeHeight(1.74f).clientTrackingRange(10).build(ElementalArcana.MODID + ":revenant"));
    public static final DeferredItem<DeferredSpawnEggItem> REVENANT_EGG = ITEMS.register("revenant_spawn_egg",
            () -> new DeferredSpawnEggItem(REVENANT, 0x2A2633, 0x9E8CFF, new Item.Properties()));

    public static final DeferredHolder<StructureType<?>, StructureType<CryptStructure>> CRYPT =
            STRUCTURE_TYPES.register("crypt", () -> () -> CryptStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRYPT_ENTRANCE =
            STRUCTURE_PIECES.register("crypt_entrance", () -> (StructurePieceType.ContextlessType) CryptEntrancePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> CRYPT_ROOM =
            STRUCTURE_PIECES.register("crypt_room", () -> (StructurePieceType.ContextlessType) CryptRoomPiece::new);

    private ModCrypts() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        modEventBus.addListener(ModCrypts::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(REVENANT.get(), RevenantEntity.createAttributes().build());
    }

    /** The crypt's blocks for the creative tab. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(RUNESTONE_ITEM, RUNE_LOCK_ITEM, RUNIC_SEAL_ITEM, GLYPH_ITEM, COFFIN_ITEM, GRAVE_FLAME_ITEM);
    }
}
