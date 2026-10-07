package com.chappadodle.elementalarcana.content.wild;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

import java.util.List;

/**
 * Creatures of the Wild (docs/superpowers/specs/2026-10-04-wild-creatures-design.md): the
 * Thornwood Treant, the Frost Wraith and the Ember Salamander, what they leave behind, and Rooted.
 */
public final class ModWild {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
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

    // Creatures of the Wild II (docs/superpowers/specs/2026-10-05-wild-creatures-2-design.md).
    public static final DeferredHolder<EntityType<?>, EntityType<HarpyEntity>> HARPY = ENTITY_TYPES.register("gale_harpy",
            () -> EntityType.Builder.of(HarpyEntity::new, MobCategory.MONSTER).sized(0.8f, 1.6f).eyeHeight(1.35f)
                    .clientTrackingRange(10).build(ElementalArcana.MODID + ":gale_harpy"));
    public static final DeferredHolder<EntityType<?>, EntityType<CrystalCrawlerEntity>> CRAWLER = ENTITY_TYPES.register("crystal_crawler",
            () -> EntityType.Builder.of(CrystalCrawlerEntity::new, MobCategory.MONSTER).sized(1.4f, 0.9f).eyeHeight(0.6f)
                    .clientTrackingRange(8).build(ElementalArcana.MODID + ":crystal_crawler"));
    public static final DeferredHolder<EntityType<?>, EntityType<BogLurkerEntity>> LURKER = ENTITY_TYPES.register("bog_lurker",
            () -> EntityType.Builder.of(BogLurkerEntity::new, MobCategory.MONSTER).sized(1.4f, 0.7f).eyeHeight(0.6f)
                    .clientTrackingRange(8).build(ElementalArcana.MODID + ":bog_lurker"));

    // Creatures of the Nether (docs/superpowers/specs/2026-10-07-nether-creatures-design.md).
    public static final DeferredHolder<EntityType<?>, EntityType<AshWraithEntity>> ASH_WRAITH = ENTITY_TYPES.register("ash_wraith",
            () -> EntityType.Builder.of(AshWraithEntity::new, MobCategory.MONSTER).sized(0.7f, 2.2f).eyeHeight(1.95f).fireImmune()
                    .clientTrackingRange(8).build(ElementalArcana.MODID + ":ash_wraith"));
    public static final DeferredHolder<EntityType<?>, EntityType<CinderHoundEntity>> CINDER_HOUND = ENTITY_TYPES.register("cinder_hound",
            () -> EntityType.Builder.of(CinderHoundEntity::new, MobCategory.MONSTER).sized(0.7f, 1.0f).eyeHeight(0.8f).fireImmune()
                    .clientTrackingRange(8).build(ElementalArcana.MODID + ":cinder_hound"));

    public static final DeferredItem<DeferredSpawnEggItem> TREANT_EGG = ITEMS.register("thornwood_treant_spawn_egg",
            () -> new DeferredSpawnEggItem(TREANT, 0x5A3F24, 0x4E8A2E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> FROST_WRAITH_EGG = ITEMS.register("frost_wraith_spawn_egg",
            () -> new DeferredSpawnEggItem(FROST_WRAITH, 0xC8E6F5, 0x4F86AD, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> SALAMANDER_EGG = ITEMS.register("ember_salamander_spawn_egg",
            () -> new DeferredSpawnEggItem(SALAMANDER, 0x7A2E12, 0xFFA030, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> HARPY_EGG = ITEMS.register("gale_harpy_spawn_egg",
            () -> new DeferredSpawnEggItem(HARPY, 0x6F7F98, 0xE8D27A, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CRAWLER_EGG = ITEMS.register("crystal_crawler_spawn_egg",
            () -> new DeferredSpawnEggItem(CRAWLER, 0x3A3046, 0xC48CFF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LURKER_EGG = ITEMS.register("bog_lurker_spawn_egg",
            () -> new DeferredSpawnEggItem(LURKER, 0x4A5A30, 0xC8B060, new Item.Properties()));

    /** What the three leave behind now and then: materials for the wild's own gear. */
    public static final DeferredItem<Item> HEARTWOOD = ITEMS.register("heartwood", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> WRAITH_SILK = ITEMS.register("wraith_silk", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> SALAMANDER_SCALE = ITEMS.register("salamander_scale",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> HARPY_PLUME = ITEMS.register("harpy_plume", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> PRISM_CORE = ITEMS.register("prism_core", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> BOG_PEARL = ITEMS.register("bog_pearl", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<DeferredSpawnEggItem> ASH_WRAITH_EGG = ITEMS.register("ash_wraith_spawn_egg",
            () -> new DeferredSpawnEggItem(ASH_WRAITH, 0x5E5C64, 0x50E2FF, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> CINDER_HOUND_EGG = ITEMS.register("cinder_hound_spawn_egg",
            () -> new DeferredSpawnEggItem(CINDER_HOUND, 0x322C2E, 0xFF7A20, new Item.Properties()));
    public static final DeferredItem<Item> SOUL_ASH = ITEMS.register("soul_ash", () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<Item> CINDER_FANG = ITEMS.register("cinder_fang",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()));

    /** Trophies of the Wild (docs/superpowers/specs/2026-10-04-wild-trophies-design.md): each creature's trick, carried. */
    public static final DeferredItem<WildCharmItem> HEARTWOOD_TALISMAN = ITEMS.register("heartwood_talisman",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<WildCharmItem> WRAITHSILK_VEIL = ITEMS.register("wraithsilk_veil",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<WildCharmItem> SALAMANDER_CHARM = ITEMS.register("salamander_charm",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    // Trophies II (docs/superpowers/specs/2026-10-05-wild-trophies-2-design.md).
    public static final DeferredItem<WildCharmItem> PLUME_CHARM = ITEMS.register("plume_of_the_gale",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<WildCharmItem> PRISM_CHARM = ITEMS.register("crawlers_prism",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<WildCharmItem> PEARL_CHARM = ITEMS.register("bog_pearl_charm",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // The Creatures of the Nether's trophies (NetherCharms).
    public static final DeferredItem<WildCharmItem> ASHEN_SHROUD = ITEMS.register("ashen_shroud",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<WildCharmItem> HOUNDSTOOTH_CHARM = ITEMS.register("houndstooth_charm",
            () -> new WildCharmItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant()));

    /** Lava cooled under a Salamander Charm's bearer; it melts back on its own. */
    public static final DeferredBlock<LavaCrustBlock> LAVA_CRUST = BLOCKS.register("lava_crust", () -> new LavaCrustBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.NETHER).strength(0.5f).sound(SoundType.BASALT)
                    .lightLevel(state -> 3 + 3 * state.getValue(LavaCrustBlock.AGE)).noLootTable()
                    .isValidSpawn((state, level, pos, type) -> false).pushReaction(PushReaction.BLOCK)));

    /** Held fast by roots: no walking, no jumping. */
    public static final DeferredHolder<MobEffect, RootedEffect> ROOTED = EFFECTS.register("rooted", RootedEffect::new);

    private ModWild() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        EFFECTS.register(modEventBus);
        modEventBus.addListener(ModWild::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TREANT.get(), TreantEntity.createAttributes().build());
        event.put(FROST_WRAITH.get(), FrostWraithEntity.createAttributes().build());
        event.put(SALAMANDER.get(), SalamanderEntity.createAttributes().build());
        event.put(HARPY.get(), HarpyEntity.createAttributes().build());
        event.put(CRAWLER.get(), CrystalCrawlerEntity.createAttributes().build());
        event.put(LURKER.get(), BogLurkerEntity.createAttributes().build());
        event.put(ASH_WRAITH.get(), AshWraithEntity.createAttributes().build());
        event.put(CINDER_HOUND.get(), CinderHoundEntity.createAttributes().build());
    }

    /** For the creative tab: the eggs, the materials, then the charms they make. */
    public static List<DeferredItem<? extends Item>> items() {
        return List.of(TREANT_EGG, FROST_WRAITH_EGG, SALAMANDER_EGG, HARPY_EGG, CRAWLER_EGG, LURKER_EGG, HEARTWOOD, WRAITH_SILK,
                SALAMANDER_SCALE, HARPY_PLUME, PRISM_CORE, BOG_PEARL, HEARTWOOD_TALISMAN, WRAITHSILK_VEIL, SALAMANDER_CHARM,
                PLUME_CHARM, PRISM_CHARM, PEARL_CHARM, ASH_WRAITH_EGG, CINDER_HOUND_EGG, SOUL_ASH, CINDER_FANG, ASHEN_SHROUD, HOUNDSTOOTH_CHARM);
    }
}
