package com.chappadodle.elementalarcana.content.wonder;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * The Wonders of the Wild (docs/superpowers/specs/2026-10-06-wonders-of-the-wild-design.md):
 * glowmoths and skyrays, the Moth Jar and the Bottled Glowmoth that sets it.
 */
public final class ModWonders {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GlowmothEntity>> GLOWMOTH = ENTITY_TYPES.register("glowmoth",
            () -> EntityType.Builder.<GlowmothEntity>of(GlowmothEntity::new, MobCategory.AMBIENT).sized(0.3f, 0.3f).clientTrackingRange(6)
                    .build(ElementalArcana.MODID + ":glowmoth"));
    public static final DeferredHolder<EntityType<?>, EntityType<SkyrayEntity>> SKYRAY = ENTITY_TYPES.register("skyray",
            () -> EntityType.Builder.<SkyrayEntity>of(SkyrayEntity::new, MobCategory.AMBIENT).sized(3f, 0.6f).clientTrackingRange(12)
                    .build(ElementalArcana.MODID + ":skyray"));

    public static final DeferredBlock<MothJarBlock> MOTH_JAR = BLOCKS.register("moth_jar", () -> new MothJarBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3f).sound(SoundType.GLASS).lightLevel(state -> 12)
                    .noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredItem<BottledGlowmothItem> BOTTLED_GLOWMOTH = ITEMS.register("bottled_glowmoth",
            () -> new BottledGlowmothItem(MOTH_JAR.get(), new Item.Properties().stacksTo(16)));
    public static final DeferredItem<DeferredSpawnEggItem> GLOWMOTH_EGG = ITEMS.register("glowmoth_spawn_egg",
            () -> new DeferredSpawnEggItem(GLOWMOTH, 0x2A2440, 0xFFF1B8, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SKYRAY_EGG = ITEMS.register("skyray_spawn_egg",
            () -> new DeferredSpawnEggItem(SKYRAY, 0x1E3A6E, 0x9EE6FF, new Item.Properties()));

    private ModWonders() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModWonders::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(GLOWMOTH.get(), GlowmothEntity.createAttributes().build());
        event.put(SKYRAY.get(), SkyrayEntity.createAttributes().build());
    }

    /** For the creative tab: a bottled glowmoth of each element, then the eggs. */
    public static List<ItemStack> items() {
        List<ItemStack> items = new ArrayList<>();
        for (Element element : Element.values()) {
            items.add(BottledGlowmothItem.of(element));
        }
        items.add(new ItemStack(GLOWMOTH_EGG.get()));
        items.add(new ItemStack(SKYRAY_EGG.get()));
        return items;
    }
}
