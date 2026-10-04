package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Creatures of the Wild (docs/superpowers/specs/2026-10-04-wild-creatures-design.md): the
 * Thornwood Treant, the Frost Wraith and the Ember Salamander, what they leave behind, and Rooted.
 */
public final class ModWild {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ElementalArcana.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<TreantEntity>> TREANT = ENTITY_TYPES.register("thornwood_treant",
            () -> EntityType.Builder.of(TreantEntity::new, MobCategory.MONSTER).sized(1.4f, 3.0f).eyeHeight(2.4f)
                    .clientTrackingRange(10).build(ElementalArcana.MODID + ":thornwood_treant"));
    public static final DeferredHolder<EntityType<?>, EntityType<FrostWraithEntity>> FROST_WRAITH = ENTITY_TYPES.register("frost_wraith",
            () -> EntityType.Builder.of(FrostWraithEntity::new, MobCategory.MONSTER).sized(0.7f, 2.2f).eyeHeight(1.95f)
                    .clientTrackingRange(8).build(ElementalArcana.MODID + ":frost_wraith"));
    public static final DeferredHolder<EntityType<?>, EntityType<SalamanderEntity>> SALAMANDER = ENTITY_TYPES.register("ember_salamander",
            () -> EntityType.Builder.of(SalamanderEntity::new, MobCategory.MONSTER).sized(1.0f, 0.6f).eyeHeight(0.45f)
                    .fireImmune().clientTrackingRange(8).build(ElementalArcana.MODID + ":ember_salamander"));

    public static final DeferredItem<DeferredSpawnEggItem> TREANT_EGG = ITEMS.register("thornwood_treant_spawn_egg",
            () -> new DeferredSpawnEggItem(TREANT, 0x5A3F24, 0x4E8A2E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FROST_WRAITH_EGG = ITEMS.register("frost_wraith_spawn_egg",
            () -> new DeferredSpawnEggItem(FROST_WRAITH, 0xC8E6F5, 0x4F86AD, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SALAMANDER_EGG = ITEMS.register("ember_salamander_spawn_egg",
            () -> new DeferredSpawnEggItem(SALAMANDER, 0x7A2E12, 0xFFA030, new Item.Properties()));

    /** What the three leave behind now and then: materials for the wild's own gear. */
    public static final DeferredItem<Item> HEARTWOOD = ITEMS.register("heartwood", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> WRAITH_SILK = ITEMS.register("wraith_silk", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SALAMANDER_SCALE = ITEMS.register("salamander_scale",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** Held fast by roots: no walking, no jumping. */
    public static final DeferredHolder<MobEffect, RootedEffect> ROOTED = EFFECTS.register("rooted", RootedEffect::new);

    private ModWild() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        EFFECTS.register(modEventBus);
        modEventBus.addListener(ModWild::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TREANT.get(), TreantEntity.createAttributes().build());
        event.put(FROST_WRAITH.get(), FrostWraithEntity.createAttributes().build());
        event.put(SALAMANDER.get(), SalamanderEntity.createAttributes().build());
    }

    /** For the creative tab: the eggs, then the materials. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(TREANT_EGG, FROST_WRAITH_EGG, SALAMANDER_EGG, HEARTWOOD, WRAITH_SILK, SALAMANDER_SCALE);
    }
}
