package com.chappadodle.elementalarcana.content.creature;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The mod's creatures (docs/superpowers/specs/2026-10-02-wisps-design.md): a wisp of every element and
 * their spawn eggs; a familiar of every element (a bound wisp, see the Familiars spec) and the
 * Binding Charm that makes one; the Elemental Golems (see the Golems spec) and their eggs.
 */
public final class ModCreatures {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final Map<Element, DeferredHolder<EntityType<?>, EntityType<WispEntity>>> WISPS = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredItem<DeferredSpawnEggItem>> WISP_EGGS = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredHolder<EntityType<?>, EntityType<FamiliarEntity>>> FAMILIARS = new EnumMap<>(Element.class);
    /** The Elemental Golems, one per base element (see the Golems spec). */
    public static final List<Element> GOLEM_ELEMENTS = List.of(Element.FIRE, Element.WATER, Element.WIND, Element.EARTH);
    private static final Map<Element, DeferredHolder<EntityType<?>, EntityType<GolemEntity>>> GOLEMS = new EnumMap<>(Element.class);
    private static final Map<Element, DeferredItem<DeferredSpawnEggItem>> GOLEM_EGGS = new EnumMap<>(Element.class);
    /** Binds a worn-down wild wisp as its user's familiar. */
    public static final DeferredItem<BindingCharmItem> BINDING_CHARM = ITEMS.register("binding_charm",
            () -> new BindingCharmItem(new Item.Properties().stacksTo(16)));

    static {
        for (Element element : Element.values()) {
            String name = element.name().toLowerCase(Locale.ROOT) + "_wisp";
            DeferredHolder<EntityType<?>, EntityType<WispEntity>> type = ENTITY_TYPES.register(name, () -> {
                EntityType.Builder<WispEntity> builder = EntityType.Builder
                        .<WispEntity>of((entityType, level) -> new WispEntity(entityType, level, element), MobCategory.MONSTER)
                        .sized(0.6f, 0.6f)
                        .eyeHeight(0.3f)
                        .clientTrackingRange(8);
                if (element == Element.FIRE) {
                    builder.fireImmune();
                }
                return builder.build(ElementalArcana.MODID + ":" + name);
            });
            WISPS.put(element, type);
            String familiar = element.name().toLowerCase(Locale.ROOT) + "_familiar";
            FAMILIARS.put(element, ENTITY_TYPES.register(familiar, () -> {
                EntityType.Builder<FamiliarEntity> builder = EntityType.Builder
                        .<FamiliarEntity>of((entityType, level) -> new FamiliarEntity(entityType, level, element), MobCategory.CREATURE)
                        .sized(0.6f, 0.6f)
                        .eyeHeight(0.3f)
                        .clientTrackingRange(10);
                if (element == Element.FIRE) {
                    builder.fireImmune();
                }
                return builder.build(ElementalArcana.MODID + ":" + familiar);
            }));
            WISP_EGGS.put(element, ITEMS.register(name + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(type, eggColor(element), eggSpots(element), new Item.Properties())));
        }
    }

    static {
        for (Element element : GOLEM_ELEMENTS) {
            String name = element.name().toLowerCase(Locale.ROOT) + "_golem";
            DeferredHolder<EntityType<?>, EntityType<GolemEntity>> type = ENTITY_TYPES.register(name, () -> {
                EntityType.Builder<GolemEntity> builder = EntityType.Builder
                        .<GolemEntity>of((entityType, level) -> new GolemEntity(entityType, level, element), MobCategory.MONSTER)
                        .sized(1.4f, 2.3f)
                        .eyeHeight(2.0f)
                        .clientTrackingRange(10);
                if (element == Element.FIRE) {
                    builder.fireImmune();
                }
                return builder.build(ElementalArcana.MODID + ":" + name);
            });
            GOLEMS.put(element, type);
            GOLEM_EGGS.put(element, ITEMS.register(name + "_spawn_egg",
                    () -> new DeferredSpawnEggItem(type, golemColor(element), golemGlow(element), new Item.Properties())));
        }
    }

    private ModCreatures() {
    }

    public static EntityType<GolemEntity> golem(Element element) {
        return GOLEMS.get(element).get();
    }

    public static Item golemEgg(Element element) {
        return GOLEM_EGGS.get(element).get();
    }

    /** A golem egg's shell: its stone. */
    private static int golemColor(Element element) {
        return switch (element) {
            case FIRE -> 0x3A2C30;
            case WATER -> 0x5CA298;
            case WIND -> 0xD8D8CE;
            default -> 0x6C6862;
        };
    }

    /** A golem egg's spots: its glow. */
    private static int golemGlow(Element element) {
        return switch (element) {
            case FIRE -> 0xFF8A28;
            case WATER -> 0x6EE2FF;
            case WIND -> 0x96F6CE;
            default -> 0xFFB24C;
        };
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModCreatures::registerAttributes);
    }

    public static EntityType<WispEntity> wisp(Element element) {
        return WISPS.get(element).get();
    }

    public static EntityType<FamiliarEntity> familiar(Element element) {
        return FAMILIARS.get(element).get();
    }

    public static Item wispEgg(Element element) {
        return WISP_EGGS.get(element).get();
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        WISPS.values().forEach(type -> event.put(type.get(), WispEntity.createAttributes().build()));
        FAMILIARS.values().forEach(type -> event.put(type.get(), FamiliarEntity.createAttributes().build()));
        GOLEMS.values().forEach(type -> event.put(type.get(), GolemEntity.createAttributes().build()));
    }

    /** The egg's shell: the wisp's shell colour. */
    private static int eggColor(Element element) {
        return switch (element) {
            case FIRE -> 0xFF7020;
            case WATER -> 0x3084FF;
            case ICE -> 0x8CCDFA;
            case WIND -> 0x78DEB2;
            case EARTH -> 0x786450;
            case CRYSTAL -> 0x9A5CC8;
            case LIGHTNING -> 0xE6C21E;
            case RADIANCE -> 0xF2E2A8;
        };
    }

    /** The egg's spots: the wisp's core. */
    private static int eggSpots(Element element) {
        return switch (element) {
            case FIRE -> 0xFFE08A;
            case WATER -> 0xD0F0FF;
            case ICE -> 0xFFFFFF;
            case WIND -> 0xF0FFF8;
            case EARTH -> 0xFFC460;
            case CRYSTAL -> 0xF0D8FF;
            case LIGHTNING -> 0xFFFFFF;
            case RADIANCE -> 0xFFFFFF;
        };
    }
}
