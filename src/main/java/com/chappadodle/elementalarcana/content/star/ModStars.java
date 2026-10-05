package com.chappadodle.elementalarcana.content.star;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Starfall (docs/superpowers/specs/2026-10-05-starfall-design.md): the Fallen Star and the Starstone
 * it cools into, Star Fragments, the Starlit Lantern and the Wishing Star.
 */
public final class ModStars {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);

    public static final DeferredBlock<FallenStarBlock> FALLEN_STAR = BLOCKS.register("fallen_star", () -> new FallenStarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3f, 6f).requiresCorrectToolForDrops().lightLevel(state -> 15)
                    .sound(SoundType.AMETHYST).noOcclusion()));
    public static final DeferredBlock<Block> STARSTONE = BLOCKS.register("starstone", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(2.5f, 6f).requiresCorrectToolForDrops()
                    .lightLevel(state -> 4).sound(SoundType.DEEPSLATE)));
    public static final DeferredBlock<StarlitLanternBlock> STARLIT_LANTERN = BLOCKS.register("starlit_lantern", () -> new StarlitLanternBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2f, 6f).requiresCorrectToolForDrops().lightLevel(state -> 15)
                    .sound(SoundType.LANTERN).noOcclusion()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StarlitLanternBlockEntity>> LANTERN_ENTITY =
            BLOCK_ENTITIES.register("starlit_lantern", () -> BlockEntityType.Builder.of(StarlitLanternBlockEntity::new, STARLIT_LANTERN.get()).build(null));

    public static final DeferredItem<BlockItem> FALLEN_STAR_ITEM = ITEMS.register("fallen_star",
            () -> new BlockItem(FALLEN_STAR.get(), new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<BlockItem> STARSTONE_ITEM = ITEMS.registerSimpleBlockItem(STARSTONE);
    public static final DeferredItem<BlockItem> STARLIT_LANTERN_ITEM = ITEMS.register("starlit_lantern",
            () -> new BlockItem(STARLIT_LANTERN.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> STAR_FRAGMENT = ITEMS.register("star_fragment", () -> new Item(new Item.Properties().rarity(Rarity.RARE)));
    public static final DeferredItem<WishingStarItem> WISHING_STAR = ITEMS.register("wishing_star",
            () -> new WishingStarItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC)));

    private ModStars() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }

    /** For the creative tab. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(STAR_FRAGMENT, WISHING_STAR, STARLIT_LANTERN_ITEM, FALLEN_STAR_ITEM, STARSTONE_ITEM);
    }
}
