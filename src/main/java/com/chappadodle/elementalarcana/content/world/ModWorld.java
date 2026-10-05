package com.chappadodle.elementalarcana.content.world;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The world's magic places (docs/superpowers/specs/2026-10-02-shrines-design.md): the shrine
 * structure, its generation piece, and the Shrine Core block at its heart; and the old ruins
 * (docs/superpowers/specs/2026-10-04-ruins-design.md).
 */
public final class ModWorld {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, ElementalArcana.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, ElementalArcana.MODID);

    /** Unbreakable, glowing: the shrine's power stays where it was found. */
    public static final DeferredBlock<ShrineCoreBlock> SHRINE_CORE = BLOCKS.register("shrine_core", () -> new ShrineCoreBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1f, 3600000f).noLootTable()
                    .lightLevel(state -> 12).sound(SoundType.AMETHYST).noOcclusion()));
    public static final DeferredItem<BlockItem> SHRINE_CORE_ITEM = ITEMS.register("shrine_core",
            () -> new BlockItem(SHRINE_CORE.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShrineCoreBlockEntity>> SHRINE_CORE_ENTITY =
            BLOCK_ENTITIES.register("shrine_core", () -> BlockEntityType.Builder.of(ShrineCoreBlockEntity::new, SHRINE_CORE.get()).build(null));

    public static final DeferredHolder<StructureType<?>, StructureType<ShrineStructure>> SHRINE =
            STRUCTURE_TYPES.register("shrine", () -> () -> ShrineStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SHRINE_PIECE =
            STRUCTURE_PIECES.register("shrine", () -> (StructurePieceType.ContextlessType) ShrinePiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<RuinStructure>> RUIN =
            STRUCTURE_TYPES.register("ruin", () -> () -> RuinStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> RUIN_PIECE =
            STRUCTURE_PIECES.register("ruin", () -> (StructurePieceType.ContextlessType) RuinPiece::new);

    // Sky Isles (docs/superpowers/specs/2026-10-05-sky-isles-design.md).
    public static final DeferredHolder<StructureType<?>, StructureType<SkyIsleStructure>> SKY_ISLE =
            STRUCTURE_TYPES.register("sky_isle", () -> () -> SkyIsleStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SKY_ISLE_PIECE =
            STRUCTURE_PIECES.register("sky_isle", () -> (StructurePieceType.ContextlessType) SkyIslePiece::new);

    private ModWorld() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        LeyLines.ATTACHMENTS.register(modEventBus);
    }
}
