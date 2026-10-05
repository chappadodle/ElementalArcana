package com.chappadodle.elementalarcana.content.seeker;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.SeekerRules;
import com.mojang.serialization.Codec;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Seeker's Compass (docs/superpowers/specs/2026-10-05-seekers-compass-design.md): a compass that
 * finds the nearest shrine, ruin, crypt, mage tower, sanctum, drake nest or sky isle.
 */
public final class ModSeeker {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElementalArcana.MODID);

    /** What a Seeker's Compass seeks (one of SeekerRules.KINDS; the first if it has none). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>> SEEKING =
            COMPONENTS.registerComponentType("seeking", builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));
    /** Where its needle points: the place it found, until you get there. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> SOUGHT =
            COMPONENTS.registerComponentType("sought", builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    public static final DeferredItem<SeekersCompassItem> SEEKERS_COMPASS = ITEMS.register("seekers_compass",
            () -> new SeekersCompassItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    private ModSeeker() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        COMPONENTS.register(modEventBus);
    }

    /** What {@code stack} seeks. */
    public static String seeking(ItemStack stack) {
        return SeekerRules.known(stack.getOrDefault(SEEKING.get(), SeekerRules.KINDS.get(0)));
    }

    /** The structures of a kind (the tag elementalarcana:seekable/<kind>). */
    public static TagKey<Structure> structures(String kind) {
        return TagKey.create(Registries.STRUCTURE, ElementalArcana.id("seekable/" + kind));
    }

    /** "Crypts", for "Seeking: Crypts". */
    public static Component kindName(String kind) {
        return Component.translatable("seeker.elementalarcana.kind." + kind);
    }

    /** "crypt", for "a crypt" and "the crypt". */
    public static Component oneName(String kind) {
        return Component.translatable("seeker.elementalarcana.one." + kind);
    }
}
