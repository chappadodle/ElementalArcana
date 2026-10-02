package com.chappadodle.elementalarcana.content.people;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Arcanist (docs/superpowers/specs/2026-10-02-arcanist-design.md): its workstation, the Arcane
 * Lectern, the job site and profession, and the two items only Arcanists sell.
 */
public final class ModPeople {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<VillagerProfession> PROFESSIONS = DeferredRegister.create(Registries.VILLAGER_PROFESSION, ElementalArcana.MODID);

    public static final DeferredBlock<ArcaneLecternBlock> ARCANE_LECTERN = BLOCKS.register("arcane_lectern", () -> new ArcaneLecternBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(2.5f).sound(SoundType.WOOD)
                    .lightLevel(state -> 7).noOcclusion()));
    public static final DeferredItem<BlockItem> ARCANE_LECTERN_ITEM = ITEMS.register("arcane_lectern",
            () -> new BlockItem(ARCANE_LECTERN.get(), new Item.Properties()));
    public static final DeferredItem<ScrollOfUnbindingItem> SCROLL_OF_UNBINDING = ITEMS.register("scroll_of_unbinding",
            () -> new ScrollOfUnbindingItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<TomeOfInsightItem> TOME_OF_INSIGHT = ITEMS.register("tome_of_insight",
            () -> new TomeOfInsightItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final ResourceKey<PoiType> ARCANIST_POI = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, ElementalArcana.id("arcanist"));
    public static final DeferredHolder<PoiType, PoiType> ARCANIST_JOB_SITE = POI_TYPES.register("arcanist",
            () -> new PoiType(ImmutableSet.copyOf(ARCANE_LECTERN.get().getStateDefinition().getPossibleStates()), 1, 1));
    public static final DeferredHolder<VillagerProfession, VillagerProfession> ARCANIST = PROFESSIONS.register("arcanist",
            () -> new VillagerProfession("arcanist", poi -> poi.is(ARCANIST_POI), poi -> poi.is(ARCANIST_POI),
                    ImmutableSet.of(), ImmutableSet.of(), SoundEvents.AMETHYST_BLOCK_RESONATE));

    private ModPeople() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        POI_TYPES.register(modEventBus);
        PROFESSIONS.register(modEventBus);
    }
}
