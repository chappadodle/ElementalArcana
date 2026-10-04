package com.chappadodle.elementalarcana.content.infusion;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.content.gear.ModGear;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Arcane Infusion (docs/superpowers/specs/2026-10-04-arcane-infusion-design.md): the Infusion
 * Altar, and the {@code elementalarcana:infusion} component an infused weapon or armour piece carries.
 */
public final class ModInfusion {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElementalArcana.MODID);

    /** What the altar takes: weapons, bows and crossbows, tridents, maces and armour (a data tag). */
    public static final TagKey<Item> INFUSABLE = TagKey.create(Registries.ITEM, ElementalArcana.id("infusable"));

    /** The element an item is infused with. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Element>> INFUSION =
            COMPONENTS.registerComponentType("infusion", builder -> builder.persistent(ModGear.ELEMENT_CODEC)
                    .networkSynchronized(ModGear.ELEMENT_STREAM_CODEC));

    public static final DeferredBlock<InfusionAltarBlock> ALTAR = BLOCKS.register("infusion_altar", () -> new InfusionAltarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(3f, 6f).sound(SoundType.DEEPSLATE_BRICKS)
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> 5)));
    public static final DeferredItem<BlockItem> ALTAR_ITEM = ITEMS.registerSimpleBlockItem(ALTAR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InfusionAltarBlockEntity>> ALTAR_ENTITY =
            BLOCK_ENTITIES.register("infusion_altar", () -> BlockEntityType.Builder.of(InfusionAltarBlockEntity::new, ALTAR.get()).build(null));

    private ModInfusion() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        COMPONENTS.register(modEventBus);
    }

    /** The element {@code stack} is infused with, or null. */
    @Nullable
    public static Element infusionOf(@Nullable ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.get(INFUSION.get());
    }
}
