package com.chappadodle.elementalarcana.content.wanderer;

import com.chappadodle.elementalarcana.ElementalArcana;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The Wandering Mage (docs/superpowers/specs/2026-10-06-wandering-mage-design.md): its entity type and egg. */
public final class ModWanderer {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<WanderingMageEntity>> WANDERING_MAGE = ENTITY_TYPES.register("wandering_mage",
            () -> EntityType.Builder.<WanderingMageEntity>of(WanderingMageEntity::new, MobCategory.CREATURE).sized(0.6f, 1.95f)
                    .clientTrackingRange(10).build(ElementalArcana.MODID + ":wandering_mage"));
    public static final DeferredItem<DeferredSpawnEggItem> WANDERING_MAGE_EGG = ITEMS.register("wandering_mage_spawn_egg",
            () -> new DeferredSpawnEggItem(WANDERING_MAGE, 0x24346A, 0xCED6E4, new Item.Properties()));

    private ModWanderer() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(ModWanderer::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(WANDERING_MAGE.get(), Mob.createMobAttributes().build());
    }
}
