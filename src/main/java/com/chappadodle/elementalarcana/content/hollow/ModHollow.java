package com.chappadodle.elementalarcana.content.hollow;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Hollow (docs/superpowers/specs/2026-10-03-the-hollow-design.md): its dimension (data:
 * dimension/the_hollow), the Hollow itself, the Prime Key that opens the way and the Heart of the
 * Prime it leaves.
 */
public final class ModHollow {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);

    public static final ResourceKey<Level> THE_HOLLOW = ResourceKey.create(Registries.DIMENSION, ElementalArcana.id("the_hollow"));
    /** What its hunger does to someone with no mana left. */
    public static final ResourceKey<DamageType> HUNGER = ResourceKey.create(Registries.DAMAGE_TYPE, ElementalArcana.id("hollow_hunger"));

    public static final DeferredItem<PrimeKeyItem> PRIME_KEY = ITEMS.register("prime_key",
            () -> new PrimeKeyItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final DeferredItem<PrimeHeartItem> PRIME_HEART = ITEMS.register("prime_heart",
            () -> new PrimeHeartItem(new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant()));

    public static final DeferredHolder<EntityType<?>, EntityType<HollowEntity>> HOLLOW = ENTITY_TYPES.register("the_hollow",
            () -> EntityType.Builder.<HollowEntity>of(HollowEntity::new, MobCategory.MONSTER)
                    .sized(2.4f, 4.5f).eyeHeight(3.2f).fireImmune().clientTrackingRange(12)
                    .build(ElementalArcana.MODID + ":the_hollow"));
    public static final DeferredItem<DeferredSpawnEggItem> HOLLOW_EGG = ITEMS.register("the_hollow_spawn_egg",
            () -> new DeferredSpawnEggItem(HOLLOW, 0x14101C, 0x9A4AE0, new Item.Properties()));

    private ModHollow() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(ModHollow::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HOLLOW.get(), HollowEntity.createAttributes().build());
    }
}
