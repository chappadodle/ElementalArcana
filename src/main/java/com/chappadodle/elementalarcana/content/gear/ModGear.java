package com.chappadodle.elementalarcana.content.gear;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Magic gear (docs/superpowers/specs/2026-10-02-gear-design.md): elemental foci (an Apprentice
 * Wand and an Adept Staff, attuned to one element through the {@link #ELEMENT} component) and two
 * robe sets. Their stat bonuses feed the same stats as stat points and tree nodes (see GearStats).
 */
public final class ModGear {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ElementalArcana.MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, ElementalArcana.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);

    private static final Codec<Element> ELEMENT_CODEC = Codec.STRING.comapFlatMap(
            name -> {
                for (Element element : Element.values()) {
                    if (element.name().equalsIgnoreCase(name)) {
                        return DataResult.success(element);
                    }
                }
                return DataResult.error(() -> "Unknown element: " + name);
            },
            element -> element.name().toLowerCase(Locale.ROOT));
    private static final StreamCodec<FriendlyByteBuf, Element> ELEMENT_STREAM_CODEC = NeoForgeStreamCodecs.enumCodec(Element.class);

    /** The element a focus is attuned to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Element>> ELEMENT =
            COMPONENTS.registerComponentType("element", builder -> builder.persistent(ELEMENT_CODEC).networkSynchronized(ELEMENT_STREAM_CODEC));

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> APPRENTICE_CLOTH = ARMOR_MATERIALS.register("apprentice", () -> new ArmorMaterial(
            defense(1, 2, 3, 1), 15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ItemTags.WOOL),
            List.of(new ArmorMaterial.Layer(ElementalArcana.id("apprentice"))), 0f, 0f));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ADEPT_CLOTH = ARMOR_MATERIALS.register("adept", () -> new ArmorMaterial(
            defense(2, 4, 5, 1), 20, SoundEvents.ARMOR_EQUIP_CHAIN, () -> Ingredient.of(Items.AMETHYST_SHARD),
            List.of(new ArmorMaterial.Layer(ElementalArcana.id("adept"))), 0.5f, 0f));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MASTER_CLOTH = ARMOR_MATERIALS.register("master", () -> new ArmorMaterial(
            defense(2, 5, 6, 2), 25, SoundEvents.ARMOR_EQUIP_GOLD, () -> Ingredient.of(Items.DIAMOND),
            List.of(new ArmorMaterial.Layer(ElementalArcana.id("master"))), 1f, 0f));

    public static final DeferredItem<FocusItem> APPRENTICE_WAND = ITEMS.register("apprentice_wand",
            () -> new FocusItem(1, 0, Map.of("potency", 2), 3, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<FocusItem> ADEPT_STAFF = ITEMS.register("adept_staff",
            () -> new FocusItem(2, 15, Map.of("potency", 4, "focus", 2), 6, new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    /** The master tier (level 35): made with a Magister's Guardian Core (see the mage towers spec). */
    public static final DeferredItem<FocusItem> MASTER_STAFF = ITEMS.register("master_staff",
            () -> new FocusItem(3, 35, Map.of("potency", 7, "focus", 4), 10, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<RobeItem> APPRENTICE_HOOD = robe("apprentice_hood", APPRENTICE_CLOTH, ArmorItem.Type.HELMET, 0, Map.of("insight", 1, "reservoir", 1));
    public static final DeferredItem<RobeItem> APPRENTICE_ROBE = robe("apprentice_robe", APPRENTICE_CLOTH, ArmorItem.Type.CHESTPLATE, 0, Map.of("reservoir", 3, "ward", 1));
    public static final DeferredItem<RobeItem> APPRENTICE_TROUSERS = robe("apprentice_trousers", APPRENTICE_CLOTH, ArmorItem.Type.LEGGINGS, 0, Map.of("focus", 2));
    public static final DeferredItem<RobeItem> APPRENTICE_BOOTS = robe("apprentice_boots", APPRENTICE_CLOTH, ArmorItem.Type.BOOTS, 0, Map.of("vitality", 1, "reservoir", 1));
    public static final DeferredItem<RobeItem> ADEPT_HOOD = robe("adept_hood", ADEPT_CLOTH, ArmorItem.Type.HELMET, 15, Map.of("insight", 2, "reservoir", 2));
    public static final DeferredItem<RobeItem> ADEPT_ROBE = robe("adept_robe", ADEPT_CLOTH, ArmorItem.Type.CHESTPLATE, 15, Map.of("reservoir", 5, "ward", 3));
    public static final DeferredItem<RobeItem> ADEPT_TROUSERS = robe("adept_trousers", ADEPT_CLOTH, ArmorItem.Type.LEGGINGS, 15, Map.of("focus", 4, "ward", 1));
    public static final DeferredItem<RobeItem> ADEPT_BOOTS = robe("adept_boots", ADEPT_CLOTH, ArmorItem.Type.BOOTS, 15, Map.of("vitality", 2, "reservoir", 2));
    public static final DeferredItem<RobeItem> MASTER_HOOD = robe("master_hood", MASTER_CLOTH, ArmorItem.Type.HELMET, 35, Map.of("insight", 3, "reservoir", 3));
    public static final DeferredItem<RobeItem> MASTER_ROBE = robe("master_robe", MASTER_CLOTH, ArmorItem.Type.CHESTPLATE, 35, Map.of("reservoir", 8, "ward", 5));
    public static final DeferredItem<RobeItem> MASTER_TROUSERS = robe("master_trousers", MASTER_CLOTH, ArmorItem.Type.LEGGINGS, 35, Map.of("focus", 6, "ward", 2));
    public static final DeferredItem<RobeItem> MASTER_BOOTS = robe("master_boots", MASTER_CLOTH, ArmorItem.Type.BOOTS, 35, Map.of("vitality", 3, "reservoir", 3));

    private ModGear() {
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        return Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            map.put(ArmorItem.Type.BOOTS, boots);
            map.put(ArmorItem.Type.LEGGINGS, leggings);
            map.put(ArmorItem.Type.CHESTPLATE, chestplate);
            map.put(ArmorItem.Type.HELMET, helmet);
            map.put(ArmorItem.Type.BODY, chestplate);
        });
    }

    private static DeferredItem<RobeItem> robe(String name, DeferredHolder<ArmorMaterial, ArmorMaterial> material, ArmorItem.Type type,
                                               int requiredLevel, Map<String, Integer> stats) {
        boolean master = requiredLevel >= 35;
        return ITEMS.register(name, () -> new RobeItem(material, type, requiredLevel, stats,
                new Item.Properties().durability(type.getDurability(master ? 28 : requiredLevel > 0 ? 20 : 12))
                        .rarity(master ? Rarity.EPIC : requiredLevel > 0 ? Rarity.RARE : Rarity.UNCOMMON)));
    }

    /** The foci, lowest tier first. */
    public static List<DeferredItem<FocusItem>> foci() {
        return List.of(APPRENTICE_WAND, ADEPT_STAFF, MASTER_STAFF);
    }

    /** The robes, in creative-tab order. */
    public static List<DeferredItem<RobeItem>> robes() {
        return List.of(APPRENTICE_HOOD, APPRENTICE_ROBE, APPRENTICE_TROUSERS, APPRENTICE_BOOTS,
                ADEPT_HOOD, ADEPT_ROBE, ADEPT_TROUSERS, ADEPT_BOOTS, MASTER_HOOD, MASTER_ROBE, MASTER_TROUSERS, MASTER_BOOTS);
    }

    public static void register(IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
        ARMOR_MATERIALS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
