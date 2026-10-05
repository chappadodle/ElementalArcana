package com.chappadodle.elementalarcana.content.cantrip;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Spell;
import com.chappadodle.elementalarcana.api.SpellRegistries;
import com.chappadodle.elementalarcana.api.SpellSchool;
import com.chappadodle.elementalarcana.content.ModSchools;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Cantrips (docs/superpowers/specs/2026-10-04-cantrips-design.md): the Cantrip Scroll and the
 * {@code elementalarcana:cantrip} component naming its cantrip, the Mage Light, and the loot
 * modifier that hides scrolls in the world's old places. The cantrips themselves are spells of the
 * Arcane school, registered in ModSpells.
 */
public final class ModCantrips {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElementalArcana.MODID);
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ElementalArcana.MODID);

    /** The cantrip a scroll teaches. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> CANTRIP =
            COMPONENTS.registerComponentType("cantrip", builder -> builder.persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    public static final DeferredItem<CantripScrollItem> SCROLL = ITEMS.register("cantrip_scroll",
            () -> new CantripScrollItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    /** A Mage Light's orb: light where it's set, gone at a touch. */
    public static final DeferredBlock<MageLightBlock> MAGE_LIGHT = BLOCKS.register("mage_light", () -> new MageLightBlock(
            BlockBehaviour.Properties.of().replaceable().noCollission().instabreak().lightLevel(state -> 15).noLootTable()
                    .sound(SoundType.AMETHYST).noOcclusion().pushReaction(PushReaction.DESTROY)
                    .isValidSpawn((state, level, pos, type) -> false)));

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<CantripScrollsModifier>> SCROLLS_IN_LOOT =
            LOOT_MODIFIERS.register("cantrip_scrolls", () -> CantripScrollsModifier.CODEC);

    private ModCantrips() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        COMPONENTS.register(modEventBus);
        LOOT_MODIFIERS.register(modEventBus);
    }

    /** Whether {@code school} is the Arcane school, whose spells are the cantrips. */
    public static boolean isArcane(SpellSchool school) {
        return school == ModSchools.ARCANE.get();
    }

    public static boolean isCantrip(Spell spell) {
        return isArcane(spell.school());
    }

    /** Every cantrip, in the wheel's order. */
    public static List<Spell> cantrips() {
        List<Spell> cantrips = new ArrayList<>();
        for (Spell spell : SpellRegistries.SPELLS) {
            if (isCantrip(spell)) {
                cantrips.add(spell);
            }
        }
        return cantrips;
    }

    /** A scroll teaching {@code cantrip}. */
    public static ItemStack scroll(Spell cantrip) {
        ItemStack stack = new ItemStack(SCROLL.get());
        stack.set(CANTRIP.get(), cantrip.id());
        return stack;
    }

    /** The cantrip {@code stack} teaches, or null if it names none (or one that isn't a cantrip). */
    @Nullable
    public static Spell cantripOf(ItemStack stack) {
        ResourceLocation id = stack.get(CANTRIP.get());
        Spell spell = id == null ? null : SpellRegistries.SPELLS.get(id);
        return spell != null && isCantrip(spell) ? spell : null;
    }
}
