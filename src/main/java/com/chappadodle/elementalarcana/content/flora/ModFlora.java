package com.chappadodle.elementalarcana.content.flora;

import com.chappadodle.elementalarcana.ElementalArcana;
import com.chappadodle.elementalarcana.api.Element;
import com.chappadodle.elementalarcana.api.Herb;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arcane Flora (docs/superpowers/specs/2026-10-06-arcane-flora-design.md): the eight herbs of the
 * elements, potted, and the tonics brewed from them, each giving Kinship with its herb's element
 * (see Kinships). Where they grow is data, from tools/gen_flora.py.
 */
@EventBusSubscriber(modid = ElementalArcana.MODID)
public final class ModFlora {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ElementalArcana.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ElementalArcana.MODID);
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ElementalArcana.MODID);
    private static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(Registries.POTION, ElementalArcana.MODID);
    private static final int NORMAL = 20 * 180;
    private static final int LONG = 20 * 480;
    private static final int STRONG = 20 * 90;

    private static final Map<Element, DeferredHolder<MobEffect, MobEffect>> KINSHIPS = new EnumMap<>(Element.class);
    private static final Map<Herb, DeferredBlock<HerbBlock>> HERBS = new EnumMap<>(Herb.class);
    private static final Map<Herb, DeferredItem<? extends BlockItem>> HERB_ITEMS = new EnumMap<>(Herb.class);
    private static final Map<Herb, DeferredBlock<FlowerPotBlock>> POTTED = new EnumMap<>(Herb.class);
    /** Each herb's tonic: plain, long (redstone) and strong (glowstone). */
    private static final Map<Herb, List<DeferredHolder<Potion, Potion>>> TONICS = new EnumMap<>(Herb.class);

    static {
        for (Element element : Element.values()) {
            KINSHIPS.put(element, EFFECTS.register(element.name().toLowerCase(Locale.ROOT) + "_kinship",
                    () -> new MobEffect(MobEffectCategory.BENEFICIAL, element.color()) {
                    }));
        }
        for (Herb herb : Herb.values()) {
            DeferredBlock<HerbBlock> block = BLOCKS.register(herb.id(), () -> switch (herb) {
                case MOONLILY -> new MoonlilyBlock(herb, properties(herb));
                case SUNPETAL -> new BloomingHerbBlock(herb, properties(herb));
                default -> new HerbBlock(herb, properties(herb));
            });
            HERBS.put(herb, block);
            HERB_ITEMS.put(herb, herb == Herb.MOONLILY
                    ? ITEMS.register(herb.id(), () -> new PlaceOnWaterBlockItem(block.get(), new Item.Properties()))
                    : ITEMS.registerSimpleBlockItem(block));
            if (herb != Herb.MOONLILY) {
                POTTED.put(herb, BLOCKS.register("potted_" + herb.id(), () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, block,
                        BlockBehaviour.Properties.ofFullCopy(Blocks.FLOWER_POT).lightLevel(state -> herb.light(true)))));
            }
            Holder<MobEffect> kinship = KINSHIPS.get(herb.element());
            String name = ElementalArcana.MODID + "." + herb.tonicId();
            TONICS.put(herb, List.of(
                    POTIONS.register(herb.tonicId(), () -> new Potion(name, new MobEffectInstance(kinship, NORMAL, 0))),
                    POTIONS.register("long_" + herb.tonicId(), () -> new Potion(name, new MobEffectInstance(kinship, LONG, 0))),
                    POTIONS.register("strong_" + herb.tonicId(), () -> new Potion(name, new MobEffectInstance(kinship, STRONG, 1)))));
        }
    }

    private ModFlora() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        EFFECTS.register(modEventBus);
        POTIONS.register(modEventBus);
        modEventBus.addListener(ModFlora::onCommonSetup);
    }

    /** A herb: breaks at a touch, glows as its herb does; all but the Moonlily (a lily pad) are walked through. */
    private static BlockBehaviour.Properties properties(Herb herb) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).instabreak()
                .pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> herb.light(!state.hasProperty(BlockStateProperties.OPEN) || state.getValue(BlockStateProperties.OPEN)))
                .sound(switch (herb) {
                    case MOONLILY -> SoundType.LILY_PAD;
                    case PRISMLEAF -> SoundType.SMALL_AMETHYST_BUD;
                    case DEEPCAP, FROSTCAP -> SoundType.FUNGUS;
                    default -> SoundType.GRASS;
                });
        if (herb == Herb.MOONLILY) {
            properties.noOcclusion();
        } else {
            properties.noCollission().offsetType(BlockBehaviour.OffsetType.XZ);
        }
        if (herb.bloom() != Herb.Bloom.ALWAYS) {
            properties.randomTicks();
        }
        return properties;
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> POTTED.forEach((herb, pot) -> ((FlowerPotBlock) Blocks.FLOWER_POT).addPlant(HERBS.get(herb).getId(), pot)));
    }

    @SubscribeEvent
    public static void onBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder brewing = event.getBuilder();
        for (Herb herb : Herb.values()) {
            List<DeferredHolder<Potion, Potion>> tonic = TONICS.get(herb);
            brewing.addMix(Potions.AWKWARD, item(herb), tonic.get(0));
            brewing.addMix(tonic.get(0), Items.REDSTONE, tonic.get(1));
            brewing.addMix(tonic.get(0), Items.GLOWSTONE_DUST, tonic.get(2));
        }
    }

    /** Kinship with an element (a tonic's effect). */
    public static Holder<MobEffect> kinship(Element element) {
        return KINSHIPS.get(element);
    }

    public static Item item(Herb herb) {
        return HERB_ITEMS.get(herb).get();
    }

    /** A herb's plain tonic. */
    public static Holder<Potion> tonic(Herb herb) {
        return TONICS.get(herb).getFirst();
    }

    /** The herbs, in the elements' order (for the creative tab and JEI). */
    public static List<Item> herbItems() {
        List<Item> items = new ArrayList<>();
        for (Herb herb : Herb.values()) {
            items.add(item(herb));
        }
        return items;
    }

    /** Every tonic, plain, long and strong, herb by herb (for the creative tab). */
    public static List<DeferredHolder<Potion, Potion>> tonics() {
        List<DeferredHolder<Potion, Potion>> tonics = new ArrayList<>();
        TONICS.values().forEach(tonics::addAll);
        return tonics;
    }
}
