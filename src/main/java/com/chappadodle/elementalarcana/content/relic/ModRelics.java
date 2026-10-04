package com.chappadodle.elementalarcana.content.relic;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Relic;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

/** Relics (docs/superpowers/specs/2026-10-04-relics-design.md): the nine relic items and their data. */
public final class ModRelics {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElementalArcana.MODID);

    /** Who bound a relic, and when. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RelicBond>> BOND =
            COMPONENTS.registerComponentType("relic_bond", builder -> builder.persistent(RelicBond.CODEC).networkSynchronized(RelicBond.STREAM_CODEC));
    /** When a relic with a cooldown (the Phylactery) is ready again, in game time. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> READY_AT =
            COMPONENTS.registerComponentType("relic_ready_at", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    private static final Map<Relic, DeferredItem<RelicItem>> RELICS = new EnumMap<>(Relic.class);

    static {
        for (Relic relic : Relic.values()) {
            RELICS.put(relic, ITEMS.register(relic.id(), () -> new RelicItem(relic, new Item.Properties().stacksTo(1)
                    .rarity(relic == Relic.REVENANTS_PHYLACTERY ? Rarity.EPIC : Rarity.RARE))));
        }
    }

    private ModRelics() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        COMPONENTS.register(modEventBus);
    }

    public static RelicItem item(Relic relic) {
        return RELICS.get(relic).get();
    }
}
